// src/index.tsx
import { findByName, findByProps } from "@vendetta/metro";
import { React, ReactNative } from "@vendetta/metro/common";
import { instead, after } from "@vendetta/patcher";
var ChannelStore = findByProps("getChannel", "getDMFromUserId");
var GuildChannelStore = findByProps("getChannels");
var PermissionStore = findByProps("can", "can");
var ViewConst = findByProps("VIEW_CHANNEL", "SEND_MESSAGES");
function getChannel(id) {
  return ChannelStore?.getChannel?.(id) ?? null;
}
function isHidden(ch) {
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
var hiddenIds = /* @__PURE__ */ new Set();
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
var styles = ReactNative.StyleSheet.create({
  wrap: { flex: 1, backgroundColor: "var(--background-primary)", padding: 16 },
  lock: { fontSize: 48, textAlign: "center", marginBottom: 8 },
  name: {
    color: "var(--header-primary)",
    fontSize: 24,
    fontFamily: "ggsans-Bold",
    textAlign: "center",
    marginTop: 16
  },
  topic: {
    color: "var(--text-muted)",
    fontSize: 14,
    fontFamily: "ggsans-Medium",
    textAlign: "center",
    marginTop: 8,
    marginHorizontal: 32
  },
  card: {
    backgroundColor: "var(--background-secondary)",
    borderRadius: 12,
    padding: 16,
    marginTop: 24
  },
  lab: {
    color: "var(--text-muted)",
    fontSize: 12,
    fontFamily: "ggsans-Medium",
    textTransform: "uppercase",
    letterSpacing: 1,
    marginBottom: 2
  },
  val: {
    color: "var(--text-normal)",
    fontSize: 16,
    fontFamily: "ggsans-Medium",
    marginBottom: 12
  },
  note: {
    color: "var(--text-muted)",
    fontSize: 12,
    fontFamily: "ggsans-Medium",
    textAlign: "center",
    marginTop: 24,
    opacity: 0.6
  }
});
function chanType(t) {
  const m = {
    0: "Text",
    2: "Voice",
    4: "Category",
    5: "Announcement",
    13: "Stage",
    15: "Forum",
    16: "Media"
  };
  return m[t] ?? `Type ${t}`;
}
function HiddenChannel({ channelId }) {
  const ch = getChannel(channelId);
  if (!ch)
    return /* @__PURE__ */ React.createElement(ReactNative.View, { style: styles.wrap }, /* @__PURE__ */ React.createElement(ReactNative.Text, { style: { color: "var(--text-muted)", fontSize: 16, textAlign: "center" } }, "No channel data available"), /* @__PURE__ */ React.createElement(ReactNative.Text, { style: styles.note }, "ID: ", channelId));
  const parent = ch.parent_id ? getChannel(ch.parent_id) : null;
  return /* @__PURE__ */ React.createElement(ReactNative.ScrollView, { style: styles.wrap }, /* @__PURE__ */ React.createElement(ReactNative.Text, { style: styles.lock }, "\u{1F512}"), /* @__PURE__ */ React.createElement(ReactNative.Text, { style: styles.name }, ch.name ?? "?"), ch.topic ? /* @__PURE__ */ React.createElement(ReactNative.Text, { style: styles.topic }, ch.topic) : null, /* @__PURE__ */ React.createElement(ReactNative.View, { style: styles.card }, /* @__PURE__ */ React.createElement(ReactNative.Text, { style: styles.lab }, "Channel ID"), /* @__PURE__ */ React.createElement(ReactNative.Text, { style: styles.val }, channelId), /* @__PURE__ */ React.createElement(ReactNative.Text, { style: styles.lab }, "Type"), /* @__PURE__ */ React.createElement(ReactNative.Text, { style: styles.val }, chanType(ch.type)), /* @__PURE__ */ React.createElement(ReactNative.Text, { style: styles.lab }, "Category"), /* @__PURE__ */ React.createElement(ReactNative.Text, { style: styles.val }, parent?.name ?? "None"), /* @__PURE__ */ React.createElement(ReactNative.Text, { style: styles.lab }, "Position"), /* @__PURE__ */ React.createElement(ReactNative.Text, { style: styles.val }, ch.position ?? "?"), /* @__PURE__ */ React.createElement(ReactNative.Text, { style: styles.lab }, "NSFW"), /* @__PURE__ */ React.createElement(ReactNative.Text, { style: styles.val }, ch.nsfw ? "Yes" : "No")), /* @__PURE__ */ React.createElement(ReactNative.Text, { style: styles.note }, "You don't have permission to view messages in this channel. Channel metadata is from server info provided on connect."));
}
var patches = [];
function patchPerms() {
  if (!PermissionStore?.can) return false;
  const VIEW = ViewConst?.VIEW_CHANNEL ?? 1024;
  patches.push(
    instead("can", PermissionStore, (args, orig) => {
      const [perm, ch] = args;
      if (perm !== VIEW || !ch || typeof ch !== "object") return orig(...args);
      if (ch._hc) return orig(...args);
      return isHidden(ch) ? true : orig(...args);
    })
  );
  return true;
}
function patchRouter() {
  const mod = findByProps("transitionToGuild", "transitionTo") ?? findByProps("transitionTo") ?? findByName("Router", false);
  if (!mod) return false;
  const key = mod.transitionTo ? "transitionTo" : "transitionToGuild";
  patches.push(
    instead(key, mod, (args, orig) => {
      const [route] = args;
      if (route?.channelId && hiddenIds.has(route.channelId)) {
        if (route.guildId) return orig({ ...route, channelId: void 0 });
        return;
      }
      return orig(...args);
    })
  );
  return true;
}
function patchFetch() {
  const mod = findByProps("fetchMessages", "getMessages");
  if (!mod?.fetchMessages) return false;
  patches.push(
    instead("fetchMessages", mod, (args, orig) => {
      const [opts] = args;
      const cid = opts?.channelId ?? opts?.channel_id;
      if (cid && hiddenIds.has(cid)) return Promise.resolve({ body: [] });
      return orig(...args);
    })
  );
  return true;
}
function patchWrapper() {
  const names = [
    "MessagesWrapperConnected",
    "MessagesWrapper",
    "Chat",
    "ChannelMessages",
    "ChatScreen",
    "ChannelView"
  ];
  for (const name of names) {
    try {
      const mod = findByName(name, false);
      if (!mod) continue;
      patches.push(
        after("render", mod, (args, ret) => {
          if (!ret) return;
          const p = args[0] ?? {};
          const id = p?.channel?.id ?? p?.channelId;
          if (id && hiddenIds.has(id)) {
            return React.createElement(HiddenChannel, { channelId: id, key: `hc-${id}` });
          }
          return ret;
        })
      );
      return true;
    } catch {
    }
  }
  return false;
}
function patchNoAccess() {
  const denyNames = ["AccessDenied", "NoAccess", "ChannelAccessDenied", "NoticeChannelRestricted"];
  for (const name of denyNames) {
    try {
      const mod = findByName(name, false);
      if (mod) {
        patches.push(
          instead("render", mod, (args, orig) => {
            const p = args[0] ?? {};
            const id = p?.channel?.id ?? p?.channelId;
            if (id && hiddenIds.has(id)) {
              return React.createElement(HiddenChannel, {
                channelId: id,
                key: `hc-noaccess-${id}`
              });
            }
            return orig(...args);
          })
        );
        return true;
      }
    } catch {
    }
  }
  const noAccessMod = findByProps("ACCESS_DENIED", "NO_ACCESS", "CHANNEL_RESTRICTED");
  if (noAccessMod) {
    for (const key of Object.keys(noAccessMod)) {
      const val = noAccessMod[key];
      if (typeof val === "function" && key !== "prototype") {
        try {
          patches.push(
            instead(key, noAccessMod, (args, orig) => {
              const p = args[0] ?? {};
              const id = p?.channel?.id ?? p?.channelId;
              if (id && hiddenIds.has(id)) {
                return React.createElement(HiddenChannel, {
                  channelId: id,
                  key: `hc-deny-${id}`
                });
              }
              return orig(...args);
            })
          );
          return true;
        } catch {
        }
      }
    }
  }
  return false;
}
var index_default = {
  onLoad() {
    console.log("[HiddenChannels] load");
    try {
      const p1 = patchPerms();
      scanHidden();
      const p2 = patchRouter();
      const p3 = patchFetch();
      const p4 = patchWrapper();
      const p5 = patchNoAccess();
      const iv = setInterval(scanHidden, 1e4);
      this._iv = iv;
      console.log(
        `[HiddenChannels] ok (perms=${p1} router=${p2} fetch=${p3} wrapper=${p4} noaccess=${p5}), ${hiddenIds.size} hidden`
      );
    } catch (e) {
      console.error("[HiddenChannels] fail", e);
    }
  },
  onUnload() {
    console.log("[HiddenChannels] unload");
    for (const u of patches) {
      try {
        u();
      } catch {
      }
    }
    patches.length = 0;
    hiddenIds.clear();
    clearInterval(this._iv);
  }
};
export {
  index_default as default
};
