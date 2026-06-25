# Orbit-AI

On-device Android workspace for AI agents. Self-contained runtime — no Termux required.

## Features

- **Self-contained POSIX runtime** — BusyBox bundled in APK (~1MB) provides `sh`, `cp`, `mv`, `tar`, `grep`, `wget`, and 40+ other tools without needing Termux.
- **Multiple agent presets** — Choose from different AI agent configurations shipped as APK flavors: `normal`, `opencode`, `openclaude`, `claudecode`, `codex`. Each with its own app label and agent defaults.
- **Agent installation from assets or GitHub** — Agents are bundled as tarballs in APK assets or downloaded from GitHub Releases on first setup.
- **Package manager** — Built-in package installer downloads and manages runtimes (node, python, git) with registry-based tracking.
- **Local command execution** — Agents run locally on-device via a shell-based command runner with isolated runtime environment.
- **API-based agents** — Connect to Gemini, Claude, OpenAI, OpenRouter via API key configuration.
- **Optional Shizuku elevation** — Root-level commands via Shizuku when available.
- **Auto-updates** — In-app update system that checks GitHub Releases for new APKs and installs them via FileProvider.
- **Logging** — All logs written to `/storage/emulated/0/omniclaw_logs/` (or app-private fallback); crashes also saved to `Downloads/omniclaw_logs/`.

## APK Flavors

The project builds 5 product flavors, each targeting a different agent ecosystem:

| Flavor       | App Label        | Agent Preset   |
|--------------|------------------|----------------|
| `normal`     | Orbit AI         | Default        |
| `opencode`   | OpenCode         | OpenCode       |
| `openclaude` | OpenClaude       | OpenClaude     |
| `claudecode` | Claude Code      | Claude Code    |
| `codex`      | Codex            | Codex          |

Build a specific flavor:
```bash
./gradlew assembleOpenclaudeDebug
./gradlew assembleOpenclaudeRelease
```

The CI workflow builds all 5 flavors and creates a GitHub Release with 5 APK artifacts.

## Architecture

```
app/src/main/java/com/omniclaw/
├── ui/
│   ├── screens/         # Compose UI screens (Chat, Setup, Settings, Dashboard)
│   └── viewmodels/      # ViewModels for reactive state
├── data/
│   ├── local/
│   │   ├── runtime/     # PackageInstaller, OrbitRuntimeManager
│   │   ├── runner/      # LocalCommandRunner (sh-based execution)
│   │   ├── entity/      # Room entities
│   │   └── dao/         # Room DAOs
│   ├── remote/          # API clients (Gemini, Claude, OpenAI, OpenRouter)
│   └── repository/      # Repository implementations
├── core/
│   ├── logging/         # FileLogger (writes to omniclaw_logs/)
│   └── di/              # Dependency injection
├── agent/               # Agent definitions and management
└── service/             # Foreground services, update manager
```

## Runtime

Orbit-AI does **not** depend on Termux. The app includes a BusyBox ARM64 binary extracted at setup time, providing a POSIX environment with `sh`, `cp`, `mv`, `tar`, `grep`, `wget`, `sed`, `awk`, and more. The runtime directory lives at:

```
/data/data/<package>/files/orbit_runtime/
├── bin/          # BusyBox + wrappers + agent entry points
├── tmp/
├── packages/     # Installed runtimes (node, python, git, ...)
├── downloads/
├── agents/       # Extracted agent code
├── logs/
└── environments/
```

PATH is automatically set to include `orbit_runtime/bin/` for all local command execution.

## Setup Flow

1. App launches → Setup Wizard
2. User configures agent type and API keys (if using API-based agents)
3. For local agents: BusyBox is installed from APK assets → agent tarball is extracted or downloaded
4. Agent wrapper scripts are created in `orbit_runtime/bin/`
5. Chat interface opens — messages are piped to the local agent process

## Logging

Logs are written to `/storage/emulated/0/omniclaw_logs/` when "All files access" is granted (Settings → Apps → Orbit-AI → All files access). Without this permission, logs fall back to the app's private external files directory.

- Daily log files: `app_YYYY-MM-DD.log`
- Crash reports: `crash_YYYYMMDD_HHmmss.log`
- Old logs are auto-cleaned (7 daily logs, 10 crash reports retained)

## Building

Prerequisites: Android SDK, JDK 17+, Gradle (wrapped).

```bash
git clone https://github.com/your-org/Orbit-AI.git
cd Orbit-AI
./gradlew assembleNormalDebug
```

For a release build, configure signing in `app/build.gradle.kts` or use CI.

## CI/CD

The GitHub Actions workflow (`android-build.yml`):
- Builds all 5 flavors on push to `main`
- Uploads APKs as artifacts
- Auto-creates a GitHub Release with all APKs

## Security Notes

- API keys (Gemini, Claude, OpenAI, OpenRouter) are stored in user preferences — not hardcoded.
- Agent code runs in-app with the same UID as the app itself.
- BusyBox wrappers use `#!/system/bin/sh` shebang to avoid external shell dependencies.
