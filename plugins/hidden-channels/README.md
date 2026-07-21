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

1. Open Discord → Settings → Plugins
2. Tap "Add Plugin" / "Install from URL"
3. Paste either URL:

   **Manifest (recommended):**
   ```
   https://cdn.jsdelivr.net/gh/TheOneWhoSpeaksJanna/Orbit-AI@main/plugins/hidden-channels/manifest.json
   ```

   **Direct bundle (if your mod doesn't support manifest):**
   ```
   https://cdn.jsdelivr.net/gh/TheOneWhoSpeaksJanna/Orbit-AI@main/plugins/hidden-channels/dist/index.js
   ```

4. Enable the "Hidden Channels" toggle

> ⚠️ `raw.githubusercontent.com` URLs don't work from mobile mods due to
> missing CORS headers. Always use jsDelivr (above) for installation.

## Manual install (advanced)

If ShiggyCord supports loading plugins from a local HTTP server:

1. Download `dist/index.js` from this repo
2. Host it on a local HTTP server (`python3 -m http.server 8080`)
3. In ShiggyCord: Settings → Developer → Load from custom URL →
   `http://<your-ip>:8080/dist/index.js`
4. Restart Discord

## Files

```
hidden-channels/
├── manifest.json     # Plugin metadata
├── src/
│   ├── index.ts      # Main plugin (patches + lifecycle)
│   └── HiddenChannel.tsx  # React component for hidden channel view
└── dist/
    └── index.js      # Compiled bundle (what you install)
```
