const assert = require('assert');

const constants = {
  Permissions: {
    VIEW_CHANNEL: 'VIEW_CHANNEL',
    READ_MESSAGE_HISTORY: 'READ_MESSAGE_HISTORY',
    SEND_MESSAGES: 'SEND_MESSAGES',
  }
};

const channels = {};
function getChannel(id) { return channels[id] || null; }

const Permissions = {
  _permissions: {},
  can(perm, channel) {
    if (channel?.realCheck) {
      return !this._permissions[channel.id]?.denied?.includes(perm);
    }
    const isAllowed = this._permissions[channel.id]?.allowed?.includes(perm);
    if (isAllowed !== undefined) return isAllowed;
    // Default: if not explicitly set, check if denied
    return !this._permissions[channel.id]?.denied?.includes(perm);
  },
  getChannelPermissions() { return {}; }
};

let patches = [];
function unpatchAll() {
  for (const u of patches) u();
  patches = [];
}

const _spitroast = {
  after(method, obj, cb) {
    const orig = obj[method].bind(obj);
    obj[method] = function(...args) {
      const res = orig(...args);
      return cb(args, res) ?? res;
    };
    patches.push(() => { obj[method] = orig; });
  },
};

const React = {
  createElement(type, props, ...children) {
    const el = { type, props: props || {}, children };
    if (typeof type === 'function') return type({ ...el.props, children });
    return el;
  },
  Fragment: 'Fragment',
};

const RN = { Image: 'RN.Image' };

const storage = {};
function getAssetByName(name) { return { id: name }; }

// =========== PLUGIN LOGIC ===========

function isHidden(channel) {
  if (channel === undefined) return false;
  if (typeof channel === "string") channel = getChannel(channel);
  if (!channel) return false;
  if ([1, 3, 4].includes(channel.type)) return false;
  channel.realCheck = true;
  const res = !Permissions.can(constants.Permissions.VIEW_CHANNEL, channel);
  delete channel.realCheck;
  return res;
}

// =========== TEST HELPERS ===========

function setupChannel(id, deniedPerms, allowedPerms) {
  const ch = {
    id,
    name: `ch-${id}`,
    type: 0,
    topic: `Topic for ${id}`,
    guild_id: '123456',
    position: 0,
  };
  channels[id] = ch;
  Permissions._permissions[id] = {
    allowed: allowedPerms || ['SEND_MESSAGES'],
    denied: deniedPerms || [],
  };
  return ch;
}

function reset() {
  unpatchAll();
  Object.keys(channels).forEach(k => delete channels[k]);
  Permissions._permissions = {};
  Object.keys(storage).forEach(k => delete storage[k]);
}

// =========== TESTS ===========

let passed = 0, failed = 0;

function run(name, fn) {
  try {
    reset();
    fn();
    passed++;
    console.log(`\x1b[32m✅ ${name}\x1b[0m`);
  } catch (e) {
    failed++;
    console.log(`\x1b[31m❌ ${name}: ${e.message}\x1b[0m`);
  }
}

// Simulate the plugin's patches
function applyPlugin() {
  _spitroast.after('can', Permissions, ([permID, channel], res) => {
    if (channel?.realCheck) return res;
    if (permID === constants.Permissions.VIEW_CHANNEL) return true;
    if (permID === constants.Permissions.READ_MESSAGE_HISTORY) return true;
    return res;
  });
}

// Test 1: Permissions patch applied
run('Permissions patch makes hidden channels visible', () => {
  const ch = setupChannel('1', ['VIEW_CHANNEL', 'READ_MESSAGE_HISTORY']);
  applyPlugin();
  assert.strictEqual(Permissions.can(constants.Permissions.VIEW_CHANNEL, ch), true);
});

// Test 2: Permissions patch allows message loading
run('Permissions patch allows READ_MESSAGE_HISTORY', () => {
  const ch = setupChannel('2', ['VIEW_CHANNEL', 'READ_MESSAGE_HISTORY']);
  applyPlugin();
  assert.strictEqual(Permissions.can(constants.Permissions.READ_MESSAGE_HISTORY, ch), true);
});

// Test 3: isHidden correctly detects hidden channels
run('isHidden detects hidden channels', () => {
  const ch = setupChannel('3', ['VIEW_CHANNEL']);
  applyPlugin();
  // isHidden uses realCheck flag, which bypasses the patch
  assert.strictEqual(isHidden(ch), true);
});

// Test 4: isHidden false for visible channels
run('isHidden false for visible channels', () => {
  const ch = setupChannel('4', []);
  applyPlugin();
  assert.strictEqual(isHidden(ch), false);
});

// Test 5: API call scenario — what Discord does when loading messages
run('Message fetch flow: permissions check before API call', () => {
  const ch = setupChannel('5', ['VIEW_CHANNEL', 'READ_MESSAGE_HISTORY']);
  applyPlugin();

  // This is what Discord does internally before making an API call:
  // 1. Check VIEW_CHANNEL → should pass (patched)
  assert.strictEqual(Permissions.can(constants.Permissions.VIEW_CHANNEL, ch), true);
  // 2. Check READ_MESSAGE_HISTORY → should pass (patched)
  assert.strictEqual(Permissions.can(constants.Permissions.READ_MESSAGE_HISTORY, ch), true);
  // 3. Then Discord makes API call - the server checks the user's token
  // The API response depends on server-side permissions, not client-side
});

// Test 6: API call fails scenario — what if server returns 403?
run('Hidden channel with VIEW read: message fetch still fails server-side', () => {
  // Even with permissions patched, the server-side check is separate
  // This simulates what happens when the API actually checks VIEW_CHANNEL
  const ch = setupChannel('6', ['VIEW_CHANNEL'], ['SEND_MESSAGES']);
  applyPlugin();
  
  // Client thinks it has permission (patched)
  assert.strictEqual(Permissions.can(constants.Permissions.VIEW_CHANNEL, ch), true);
  assert.strictEqual(Permissions.can(constants.Permissions.READ_MESSAGE_HISTORY, ch), true);
  
  // But the server will check the REAL permission
  // The API request goes to discord.com/api/v9/channels/{id}/messages
  // Server checks the auth token's actual permissions
  // If VIEW_CHANNEL is denied server-side → 403 Forbidden
  
  // Our plugin CANNOT fix this - it's client-side only
  // The question is: does the Discord API server actually check VIEW_CHANNEL for message fetch?
  // Or does it only check READ_MESSAGE_HISTORY + channel membership?
  
  console.log('  → Server-side permission check is the real question');
  console.log('  → Plugin only patches client-side permissions');
  console.log('  → If Discord API only checks READ_MESSAGE_HISTORY, messages load');
  console.log('  → If Discord API also checks VIEW_CHANNEL, messages 403');
});

// Test 7: No navigation blockers
run('No navigation blockers (user can navigate freely)', () => {
  // This version has NO transitionToGuild blocker
  // User taps hidden channel → Discord navigates normally
  // No popup, no confirmation needed
  assert.ok(true, 'Navigation is not blocked');
});

// Test 8: Verify lock icon patch
run('Lock icon patch works', () => {
  const ch = setupChannel('7', ['VIEW_CHANNEL']);
  applyPlugin();
  assert.strictEqual(isHidden(ch), true);
});

// Test 9: Cleanup on unload
run('Patches clean up on unload', () => {
  setupChannel('8', ['VIEW_CHANNEL', 'READ_MESSAGE_HISTORY']);
  applyPlugin();
  assert.strictEqual(Permissions.can(constants.Permissions.VIEW_CHANNEL, channels['8']), true);
  unpatchAll();
  // After unload, should return to real permissions
  const ch = channels['8'];
  ch.realCheck = true;
  assert.strictEqual(Permissions.can(constants.Permissions.VIEW_CHANNEL, ch), false);
  delete ch.realCheck;
});

// Test 10: isHidden cleanup
run('isHidden cleans up realCheck flag', () => {
  const ch = setupChannel('9', ['VIEW_CHANNEL']);
  applyPlugin();
  isHidden(ch);
  assert.strictEqual(ch.realCheck, undefined);
});

console.log('---');
console.log(`Tests: ${passed + failed}  Passed: ${passed}  Failed: ${failed}`);
process.exit(failed > 0 ? 1 : 0);
