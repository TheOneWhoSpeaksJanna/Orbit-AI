import { findByName, findByProps } from "@vendetta/metro";
import { React, ReactNative } from "@vendetta/metro/common";
import { instead, after } from "@vendetta/patcher";

// ────────────────────────────────────────────────────────────
//  Utilities
// ────────────────────────────────────────────────────────────

const ChannelStore = findByProps("getChannel", "getDMFromUserId");
const GuildChannelStore = findByProps("getChannels");
const PermissionStore = findByProps("can", "can");
const ViewConst = findByProps("VIEW_CHANNEL", "SEND_MESSAGES");

function getChannel(id: string) {
  return ChannelStore?.getChannel?.(id) ?? null;
}

function isHidden(ch: any): boolean {
  if (!ch) return false;
  if (typeof ch === "string") ch = getChannel(ch);
  if (!ch) return false;
  if ([4, 11, 12, 13, 14, 15, 16].includes(ch.type)) return false;
  if (ch._hc) return false;
  ch._hc = true;
  try {
    const perm = ViewConst?.VIEW_CHANNEL ?? 1024;
    return !PermissionStore?.can?.(perm, ch);
  } finally {
    delete ch._hc;
  }
}

const hiddenIds = new Set<string>();

function scanHidden() {
  hiddenIds.clear();
  const gcs = GuildChannelStore?.getChannels?.();
  if (!gcs) return;
  for (const guildId in gcs) {
    for (const c of gcs[guildId] ?? []) {
      if (isHidden(c)) hiddenIds.add(c.id);
    }
  }
}

// ────────────────────────────────────────────────────────────
//  HiddenChannel component (shown instead of "No Access")
// ────────────────────────────────────────────────────────────

const styles = ReactNative.StyleSheet.create({
  wrap: { flex: 1, backgroundColor: "var(--background-primary)", padding: 16 },
  lock: { fontSize: 48, textAlign: "center", marginBottom: 8 },
  name: {
    color: "var(--header-primary)", fontSize: 24,
    fontFamily: "ggsans-Bold", textAlign: "center", marginTop: 16,
  },
  topic: {
    color: "var(--text-muted)", fontSize: 14,
    fontFamily: "ggsans-Medium", textAlign: "center",
    marginTop: 8, marginHorizontal: 32,
  },
  card: {
    backgroundColor: "var(--background-secondary)", borderRadius: 12,
    padding: 16, marginTop: 24,
  },
  lab: {
    color: "var(--text-muted)", fontSize: 12, fontFamily: "ggsans-Medium",
    textTransform: "uppercase", letterSpacing: 1, marginBottom: 2,
  },
  val: {
    color: "var(--text-normal)", fontSize: 16, fontFamily: "ggsans-Medium",
    marginBottom: 12,
  },
  note: {
    color: "var(--text-muted)", fontSize: 12, fontFamily: "ggsans-Medium",
    textAlign: "center", marginTop: 24, opacity: 0.6,
  },
});

function chanType(t: number) {
  const m: Record<number, string> = {
    0: "Text", 2: "Voice", 4: "Category", 5: "Announcement",
    13: "Stage", 15: "Forum", 16: "Media",
  };
  return m[t] ?? `Type ${t}`;
}

function HiddenChannel({ channelId }: { channelId: string }) {
  const ch = getChannel(channelId);
  if (!ch)
    return (
      <ReactNative.View style={styles.wrap}>
        <ReactNative.Text style={{ color: "var(--text-muted)", fontSize: 16, textAlign: "center" }}>
          No channel data available
        </ReactNative.Text>
        <ReactNative.Text style={styles.note}>ID: {channelId}</ReactNative.Text>
      </ReactNative.View>
    );

  const parent = ch.parent_id ? getChannel(ch.parent_id) : null;

  return (
    <ReactNative.ScrollView style={styles.wrap}>
      <ReactNative.Text style={styles.lock}>🔒</ReactNative.Text>
      <ReactNative.Text style={styles.name}>{ch.name ?? "?"}</ReactNative.Text>
      {ch.topic ? <ReactNative.Text style={styles.topic}>{ch.topic}</ReactNative.Text> : null}

      <ReactNative.View style={styles.card}>
        <ReactNative.Text style={styles.lab}>Channel ID</ReactNative.Text>
        <ReactNative.Text style={styles.val}>{channelId}</ReactNative.Text>

        <ReactNative.Text style={styles.lab}>Type</ReactNative.Text>
        <ReactNative.Text style={styles.val}>{chanType(ch.type)}</ReactNative.Text>

        <ReactNative.Text style={styles.lab}>Category</ReactNative.Text>
        <ReactNative.Text style={styles.val}>{parent?.name ?? "None"}</ReactNative.Text>

        <ReactNative.Text style={styles.lab}>Position</ReactNative.Text>
        <ReactNative.Text style={styles.val}>{ch.position ?? "?"}</ReactNative.Text>

        <ReactNative.Text style={styles.lab}>NSFW</ReactNative.Text>
        <ReactNative.Text style={styles.val}>{ch.nsfw ? "Yes" : "No"}</ReactNative.Text>
      </ReactNative.View>

      <ReactNative.Text style={styles.note}>
        You don't have permission to view messages in this channel.
        Channel metadata is from server info provided on connect.
      </ReactNative.Text>
    </ReactNative.ScrollView>
  );
}

// ────────────────────────────────────────────────────────────
//  Patches
// ────────────────────────────────────────────────────────────

const patches: Array<() => void> = [];

// 1. Patch Permissions.can — makes hidden channels visible in the list
function patchPerms(): boolean {
  if (!PermissionStore?.can) return false;
  const VIEW = ViewConst?.VIEW_CHANNEL ?? 1024;
  patches.push(
    instead("can", PermissionStore, (args: any[], orig: Function) => {
      const [perm, ch] = args;
      if (perm !== VIEW || !ch || typeof ch !== "object") return orig(...args);
      if (ch._hc) return orig(...args);
      return isHidden(ch) ? true : orig(...args);
    }),
  );
  return true;
}

// 2. Patch Router — prevent navigating INTO hidden channels (crashes)
function patchRouter(): boolean {
  const mod =
    findByProps("transitionToGuild", "transitionTo") ??
    findByProps("transitionTo") ??
    findByName("Router", false);
  if (!mod) return false;
  const key = mod.transitionTo ? "transitionTo" : "transitionToGuild";
  patches.push(
    instead(key, mod, (args: any[], orig: Function) => {
      const [route] = args;
      if (route?.channelId && hiddenIds.has(route.channelId)) {
        // Navigate to guild instead of hidden channel
        if (route.guildId) return orig({ ...route, channelId: undefined });
        return;
      }
      return orig(...args);
    }),
  );
  return true;
}

// 3. Patch fetchMessages — prevent API 403 errors for hidden channels
function patchFetch(): boolean {
  const mod = findByProps("fetchMessages", "getMessages");
  if (!mod?.fetchMessages) return false;
  patches.push(
    instead("fetchMessages", mod, (args: any[], orig: Function) => {
      const [opts] = args;
      const cid = opts?.channelId ?? opts?.channel_id;
      if (cid && hiddenIds.has(cid)) return Promise.resolve({ body: [] });
      return orig(...args);
    }),
  );
  return true;
}

// 4. Patch the channel message component to show our info card
function patchWrapper(): boolean {
  // Try known component names in order of likelihood
  const names = [
    "MessagesWrapperConnected",
    "MessagesWrapper",
    "Chat",
    "ChannelMessages",
    "ChatScreen",
    "ChannelView",
  ];
  for (const name of names) {
    try {
      const mod = findByName(name, false);
      if (!mod) continue;
      patches.push(
        after("render", mod, (args: any[], ret: any) => {
          if (!ret) return;
          // Sniff channel ID from props
          const p = args[0] ?? {};
          const id = p?.channel?.id ?? p?.channelId;
          if (id && hiddenIds.has(id)) {
            return React.createElement(HiddenChannel, { channelId: id, key: `hc-${id}` });
          }
          return ret;
        }),
      );
      return true;
    } catch {}
  }
  return false;
}

// 5. Patch the "No Access" overlay — Discord added this in newer builds
function patchNoAccess(): boolean {
  // Try to find the "No Access" / "AccessDenied" component
  const denyNames = ["AccessDenied", "NoAccess", "ChannelAccessDenied", "NoticeChannelRestricted"];
  for (const name of denyNames) {
    try {
      const mod = findByName(name, false);
      if (mod) {
        patches.push(
          instead("render", mod, (args: any[], orig: Function) => {
            // Override the "No Access" render with our component
            const p = args[0] ?? {};
            const id = p?.channel?.id ?? p?.channelId;
            if (id && hiddenIds.has(id)) {
              return React.createElement(HiddenChannel, {
                channelId: id,
                key: `hc-noaccess-${id}`,
              });
            }
            return orig(...args);
          }),
        );
        return true;
      }
    } catch {}
  }

  // Also try patching the specific module by props
  const noAccessMod = findByProps("ACCESS_DENIED", "NO_ACCESS", "CHANNEL_RESTRICTED");
  if (noAccessMod) {
    // Try to find a render/component inside this module
    for (const key of Object.keys(noAccessMod)) {
      const val = noAccessMod[key];
      if (typeof val === "function" && key !== "prototype") {
        try {
          patches.push(
            instead(key, noAccessMod, (args: any[], orig: Function) => {
              const p = args[0] ?? {};
              const id = p?.channel?.id ?? p?.channelId;
              if (id && hiddenIds.has(id)) {
                return React.createElement(HiddenChannel, {
                  channelId: id,
                  key: `hc-deny-${id}`,
                });
              }
              return orig(...args);
            }),
          );
          return true;
        } catch {}
      }
    }
  }
  return false;
}

// ────────────────────────────────────────────────────────────
//  Plugin export
// ────────────────────────────────────────────────────────────

export default {
  onLoad() {
    console.log("[HiddenChannels] load");
    try {
      const p1 = patchPerms();
      scanHidden();
      const p2 = patchRouter();
      const p3 = patchFetch();
      const p4 = patchWrapper();
      const p5 = patchNoAccess();
      // Refresh hidden channels cache every 10s
      const iv = setInterval(scanHidden, 10000);
      (this as any)._iv = iv;
      console.log(
        `[HiddenChannels] ok (perms=${p1} router=${p2} fetch=${p3} wrapper=${p4} noaccess=${p5}), ${hiddenIds.size} hidden`,
      );
    } catch (e) {
      console.error("[HiddenChannels] fail", e);
    }
  },

  onUnload() {
    console.log("[HiddenChannels] unload");
    for (const u of patches) {
      try { u(); } catch {}
    }
    patches.length = 0;
    hiddenIds.clear();
    clearInterval((this as any)._iv);
  },
};
