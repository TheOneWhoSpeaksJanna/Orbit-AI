# Hidden Channels — ShiggyCord/Kettu Plugin

Reveals channel names, descriptions, topics, and permission info for channels
you don't have access to. Works by patching Discord's client-side permission
checks so hidden channels render in the channel list instead of "No Access".

## How it works

Discord's Gateway sends ALL channel data in the READY/GUILD_CREATE event
(including channels you can't see). The client normally filters them out
based on `Permissions.can(VIEW_CHANNEL)`. This plugin:

1. Patches `Permissions.can` so it returns `true` for `VIEW_CHANNEL` on
   otherwise-hidden channels (they appear in the list)
2. Renders a compact info card instead of "No Access" when you open one
3. Blocks navigation into hidden channels (no crash/API error)

## Limitations

- **You CANNOT read messages** — message content is enforced server-side by
  Discord's API. The plugin only reveals channel metadata.
- Works on guild channels only (not DMs/categories/groups).

## Install on ShiggyCord

**Step 1:** Enable GitHub Pages on the Orbit-AI repo (one-time, 10 seconds):
1. Go to https://github.com/TheOneWhoSpeaksJanna/Orbit-AI/settings/pages
2. Under **Source**, select **GitHub Actions**
3. Done — a workflow now auto-deploys plugins on every push

**Step 2:** Install the plugin:
1. Open Discord → Settings → Plugins → Add Plugin
2. Enter this URL:

   ```
   https://theonewhospeaksjanna.github.io/Orbit-AI
   ```

   The mod auto-appends `manifest.json` and then loads `index.js` from the same location.

3. Enable the "Hidden Channels" toggle

## Files

```
hidden-channels/
├── manifest.json     # Plugin metadata (main: index.js)
├── index.js          # Compiled IIFE bundle (uses vendetta.* globals)
├── dist/index.js     # Same bundle (backup copy)
├── src/index.tsx     # TypeScript source
├── test-plugin.mjs   # Test script (simulates mod fetch + evaluation)
└── README.md         # This file
```
