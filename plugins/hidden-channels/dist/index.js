// Hidden Channels - Vendetta/ShiggyCord Plugin
// View hidden Discord channels (name, description, topic, permissions)
(function(exports, metro, common, patcher, ui, uiAssets) {
    "use strict";
    
    const { findByProps, findByName } = metro;
    const { React, ReactNative, stylesheet, constants, moment, clipboard, toasts } = common;
    const { instead, after } = patcher;
    
    // ── Channel utilities
    const ChannelStore = findByProps("getChannel", "getDMFromUserId");
    const GuildChannelStore = findByProps("getChannels");
    const PermissionStore = findByProps("can", "can");
    const ViewConst = findByProps("VIEW_CHANNEL", "SEND_MESSAGES");
    const Router = findByProps("transitionToGuild", "transitionTo");
    const MessageStore = findByProps("getMessages");
    const ChannelTypes = findByProps("ChannelTypes");
    
    const VIEW = ViewConst?.VIEW_CHANNEL ?? 1024;
    const SKIP_TYPES = new Set([
        ChannelTypes?.ChannelTypes?.DM,
        ChannelTypes?.ChannelTypes?.GROUP_DM,
        ChannelTypes?.ChannelTypes?.GUILD_CATEGORY,
    ]);
    
    let hiddenIds = new Set();
    let scanTimer = null;
    let patches = [];
    
    function isHidden(ch) {
        if (!ch) return false;
        if (typeof ch === "string") ch = ChannelStore?.getChannel(ch);
        if (!ch) return false;
        if (SKIP_TYPES.has(ch.type)) return false;
        if (PermissionStore) {
            ch._hc = true;
            const allowed = !!PermissionStore.can(VIEW, ch);
            ch._hc = false;
            return !allowed;
        }
        return false;
    }
    
    function scanHidden() {
        const ids = new Set();
        const channels = ChannelStore?.getSortedPrivateChannels?.() ?? [];
        for (const c of channels) {
            if (isHidden(c)) ids.add(c.id);
        }
        // Guild channels
        const { getChannels } = GuildChannelStore ?? {};
        if (getChannels) {
            for (const [guildId, cats] of Object.entries(getChannels())) {
                for (const [catId, chList] of Object.entries(cats)) {
                    for (const ch of chList) {
                        if (isHidden(ch)) ids.add(ch.id);
                    }
                }
            }
        }
        hiddenIds = ids;
    }
    
    // ── Permission patch
    function patchPerms() {
        const key = "can";
        const mod = PermissionStore;
        if (!mod || !mod[key]) return;
        patches.push(instead(key, mod, function(args, orig) {
            const [perm, ch] = args;
            if (perm !== VIEW || !ch || typeof ch !== "object") return orig.apply(this, args);
            if (ch._hc) return orig.apply(this, args);
            if (hiddenIds.has(ch.id)) return true;
            return orig.apply(this, args);
        }));
    }
    
    // ── Router (prevent navigation into hidden channels)
    function patchRouter() {
        const targets = [
            ["transitionToGuild", Router],
            ["transitionTo", Router],
        ];
        for (const [key, mod] of targets) {
            if (!mod || !mod[key]) continue;
            patches.push(instead(key, mod, function(args, orig) {
                const [route] = args;
                if (route?.channelId && hiddenIds.has(route.channelId)) {
                    if (route.guildId) return orig.call(this, { ...route, channelId: undefined });
                    return; // Don't navigate to DM hidden channel
                }
                return orig.apply(this, args);
            }));
        }
    }
    
    // ── Message fetch (prevent 403 errors)
    function patchFetch() {
        const mod = findByProps("fetchMessages");
        if (!mod || !mod.fetchMessages) return;
        patches.push(instead("fetchMessages", mod, function(args, orig) {
            const [opts] = args;
            const cid = opts?.channelId ?? opts?.channel_id;
            if (cid && hiddenIds.has(cid)) return Promise.resolve({ body: [] });
            return orig.apply(this, args);
        }));
    }
    
    // ── "No Access" overlay patch
    function patchNoAccess() {
        // Try to find and patch the NoAccess/ChannelLocked component
        const noAccessNames = [
            "NoAccess", "ChannelLocked", "ChannelRestricted",
            "PrivateChannel", "GuildChannelAccessRestricted"
        ];
        for (const name of noAccessNames) {
            try {
                const mod = findByName(name, false);
                if (mod) {
                    patches.push(instead(mod.render ?? mod.default ?? mod, mod, function(args, orig) {
                        const props = args[0] ?? {};
                        const chId = props?.channel?.id ?? props?.channelId;
                        if (chId && hiddenIds.has(chId)) return null; // Hide the NoAccess overlay
                        return orig.apply(this, args);
                    }));
                    break;
                }
            } catch (e) {}
        }
    }
    
    // ── Create the info screen for hidden channels
    function HiddenChannelCard({ channelId }) {
        const ch = ChannelStore?.getChannel(channelId);
        if (!ch) return null;
        const { View, Text, ScrollView, TouchableOpacity } = ReactNative;
        const { semanticColors } = ui;
        
        const styles = stylesheet.createThemedStyleSheet({
            container: {
                flex: 1,
                padding: 16,
                backgroundColor: semanticColors?.BACKGROUND_PRIMARY ?? "#1a1a2e",
            },
            header: {
                fontSize: 24,
                fontFamily: constants?.Fonts?.PRIMARY_SEMIBOLD ?? "sans-serif",
                color: semanticColors?.HEADER_PRIMARY ?? "#ffffff",
                paddingBottom: 16,
                textAlign: "center",
            },
            card: {
                backgroundColor: semanticColors?.BACKGROUND_SECONDARY ?? "#16213e",
                borderRadius: 12,
                padding: 16,
                marginBottom: 12,
            },
            label: {
                fontSize: 12,
                fontFamily: constants?.Fonts?.PRIMARY_NORMAL ?? "sans-serif",
                color: semanticColors?.TEXT_MUTED ?? "#888888",
                textTransform: "uppercase",
                letterSpacing: 1,
                marginBottom: 4,
            },
            value: {
                fontSize: 15,
                fontFamily: constants?.Fonts?.PRIMARY_NORMAL ?? "sans-serif",
                color: semanticColors?.TEXT_NORMAL ?? "#dddddd",
            },
            badge: {
                backgroundColor: semanticColors?.BACKGROUND_ACCENT ?? "#e74c3c",
                borderRadius: 8,
                paddingHorizontal: 8,
                paddingVertical: 3,
                alignSelf: "flex-start",
            },
            badgeText: {
                color: uiAssets?.getAssetByName?.("white") ? "#ffffff" : "#ffffff",
                fontSize: 11,
                fontFamily: constants?.Fonts?.PRIMARY_BOLD ?? "sans-serif",
            }
        });
        
        return React.createElement(ScrollView, { style: styles.container },
            React.createElement(Text, { style: styles.header }, "#" + (ch.name || "hidden-channel")),
            
            React.createElement(View, { style: styles.card },
                React.createElement(Text, { style: styles.label }, "Channel ID"),
                React.createElement(Text, { style: styles.value }, ch.id)
            ),
            
            ch.type !== undefined ? React.createElement(View, { style: styles.card },
                React.createElement(Text, { style: styles.label }, "Type"),
                React.createElement(Text, { style: styles.value },
                    ["DM", "GROUP_DM", "GUILD_TEXT", "GUILD_VOICE", "GUILD_CATEGORY", "GUILD_ANNOUNCEMENT", "ANNOUNCEMENT_THREAD", "PUBLIC_THREAD", "PRIVATE_THREAD", "GUILD_STAGE_VOICE", "GUILD_DIRECTORY", "GUILD_FORUM", "GUILD_MEDIA"][ch.type] ?? ("TYPE_" + ch.type))
            ) : null,
            
            ch.topic ? React.createElement(View, { style: styles.card },
                React.createElement(Text, { style: styles.label }, "Topic"),
                React.createElement(Text, { style: styles.value }, ch.topic)
            ) : null,
            
            ch.guild_id ? React.createElement(View, { style: styles.card },
                React.createElement(Text, { style: styles.label }, "Guild ID"),
                React.createElement(Text, { style: styles.value }, ch.guild_id)
            ) : null,
            
            ch.parent_id ? React.createElement(View, { style: styles.card },
                React.createElement(Text, { style: styles.label }, "Category ID"),
                React.createElement(Text, { style: styles.value }, ch.parent_id)
            ) : null,
            
            ch.position !== undefined ? React.createElement(View, { style: styles.card },
                React.createElement(Text, { style: styles.label }, "Position"),
                React.createElement(Text, { style: styles.value }, String(ch.position))
            ) : null,
            
            ch.nsfw !== undefined ? React.createElement(View, { style: styles.card },
                React.createElement(Text, { style: styles.label }, "NSFW"),
                React.createElement(View, { style: [styles.badge, { backgroundColor: ch.nsfw ? "#e74c3c" : "#27ae60" }] },
                    React.createElement(Text, { style: styles.badgeText }, ch.nsfw ? "YES" : "NO"))
            ) : null,
            
            React.createElement(View, { style: styles.card },
                React.createElement(Text, { style: styles.label }, "Permissions"),
                React.createElement(Text, { style: styles.value }, 
                    PermissionStore?.can(VIEW, ch) ? "You have VIEW_CHANNEL ✓" : "No access to this channel")
            ),
            
            React.createElement(View, { style: styles.card },
                React.createElement(Text, { style: styles.label }, "Note"),
                React.createElement(Text, { style: { fontSize: 13, color: semanticColors?.TEXT_MUTED ?? "#888", fontStyle: "italic" } },
                    "Message content cannot be read client-side. This is enforced by Discord's API.")
            ),
        );
    }
    
    // ── Patch the MessagesWrapper / Chat to show our card instead of "No Access"
    function patchWrapper() {
        const wrapperNames = [
            "MessagesWrapperConnected", "MessagesWrapper",
            "Chat", "ChannelMessages", "ChatScreen",
        ];
        for (const name of wrapperNames) {
            try {
                const mod = findByName(name, false);
                if (!mod) continue;
                const renderKey = mod.default?.render ? "default" : mod.render ? "render" : null;
                const target = renderKey ? mod[renderKey] : mod;
                const key = renderKey || "render";
                
                patches.push(after(key, mod, function(_args, ret) {
                    if (!ret) return ret;
                    const props = _args[0] ?? {};
                    const chId = props?.channel?.id ?? props?.channelId;
                    if (chId && hiddenIds.has(chId)) {
                        return React.createElement(HiddenChannelCard, { channelId: chId, key: "hc-" + chId });
                    }
                    return ret;
                }));
                return; // Found and patched
            } catch (e) {}
        }
        
        // Fallback: patch by finding the render function for channel screens
        try {
            const key = "render";
            const mod = findByName("ChannelScreen", false);
            if (mod && mod[key]) {
                patches.push(after(key, mod, function(_args, ret) {
                    if (!ret) return ret;
                    const props = _args[0] ?? {};
                    const chId = props?.channel?.id ?? props?.channelId;
                    if (chId && hiddenIds.has(chId)) {
                        return React.createElement(HiddenChannelCard, { channelId: chId, key: "hc-" + chId });
                    }
                    return ret;
                }));
            }
        } catch (e) {}
    }
    
    // ── Plugin lifecycle
    function onLoad() {
        // Initial scan
        scanHidden();
        
        // Patch
        patchPerms();
        patchRouter();
        patchFetch();
        patchNoAccess();
        patchWrapper();
        
        // Periodic re-scan
        scanTimer = setInterval(scanHidden, 10000);
    }
    
    function onUnload() {
        for (const p of patches) p();
        patches = [];
        if (scanTimer) {
            clearInterval(scanTimer);
            scanTimer = null;
        }
        hiddenIds = new Set();
    }
    
    exports.default = { onLoad, onUnload };
    
})({}, vendetta.metro, vendetta.metro.common, vendetta.patcher, vendetta.ui, vendetta.ui.assets);
