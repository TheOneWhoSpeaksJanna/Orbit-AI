# Orbit AI - Android Workspace

Orbit AI is a powerful, on-device Android workspace for AI Agents. It provides a clean environment for running AI operations and local commands.

## Features

- **Setup Wizard & Configurations:** Onboarding flow for easy theme, agent, root permissions, and API configurations.
- **Multiple Agents:** Easily manage interactions via `Hermes` (Gemini-powered logic) or `OpenClaude` (Anthropic).
- **Termux & Shizuku Tooling:** Directly interface with your device safely using local tooling commands and optional root-level permissions via Shizuku.
- **Modern Architecture:** Built using Kotlin, Jetpack Compose, Material 3, Clean Architecture principles, and a robust CI/CD workflow.

## Project Structure

- `app/src/main/java/com/example/data`: App persistence (Room DB), preference management (DataStore), API handling (Retrofit), and Local Runtimes (Shizuku, Termux).
- `app/src/main/java/com/example/domain`: Core application logic (Repositories, Models).
- `app/src/main/java/com/example/presentation`: UI codebase built fully with Compose, using `ViewModels` for reactive state handling.
- `.github/workflows`: Contains out-of-the-box GitHub Action configuration to automatically build and post APKs.

## GitHub Actions: Build & Release

This repository is equipped with a GitHub Action to build an APK directly on every pushed/merged commit.

### How to use:
1. Push your code changes to the `main` branch.
2. Navigate to the "Actions" tab in your Github repository.
3. Observe the `Android Build` workflow in action. Once completed, a `.apk` artifact will be published under the workflow summary.
4. For Production releases, generate git tags to automatically construct an official Github release with attached binaries.

## Local Development (Android Studio)
1. Open this repository in Android Studio.
2. Select your testing device / emulator.
3. Click "Run".
*Ensure you configure your `.env` securely!*

## Rationale & Philosophy
- Avoid mocked UI — the Shizuku, Termux, and AI connections are built to be 100% interoperable inside standard Android execution constraints if correctly elevated.
- Avoid fake data.
