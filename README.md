# Slate

<p align="center">
  <img src="app/src/main/assets/Icon_logo.png" width="140" alt="Slate Logo" />
</p>

<p align="center">
  <strong>Private AI on your device.</strong><br>
  Download and run local LLMs entirely offline. No cloud. No tracking. No accounts.
</p>

<p align="center">
  <a href="https://github.com/BEKO2210/Android_Engineer/actions"><img src="https://github.com/BEKO2210/Android_Engineer/actions/workflows/build.yml/badge.svg" alt="Build" /></a>
</p>

---

## Download

Get the latest APK from [GitHub Actions](https://github.com/BEKO2210/Android_Engineer/actions) → select the latest successful build → download **Slate-x.x.x-release.apk** from Artifacts.

**Requirements:** Android 8.0+, ARM64 device, 4 GB+ RAM

---

## Features

### AI Chat
- Run LLMs **100% on-device** via llama.cpp — no internet needed after download
- **Streaming responses** with real-time token display
- **Stop / Regenerate / Clear** conversation controls
- **Conversation starters** in 8 languages (EN, DE, FR, ES, TR, AR, ZH, JA)
- **Markdown rendering** — headers, lists, bold, italic, code, tables
- **Syntax highlighting** in code blocks (Kotlin, Python, JS, and more)
- **Copy button** on every message and code block
- **Chat history** persisted across app restarts (optional, can be disabled)

### Models
- **5 built-in models** optimized for mobile:

| Model | Size | RAM | Best for |
|---|---|---|---|
| **Qwen 2.5 3B** | 2.0 GB | 6 GB+ | Best quality, multilingual (recommended) |
| **Gemma 3 1B** | 806 MB | 3 GB+ | Google's newest, good balance |
| **SmolLM2 1.7B** | 1.0 GB | 4 GB+ | Simple tasks, fast |
| **Phi-3 Mini 3.8B** | 2.3 GB | 8 GB+ | Reasoning, coding |
| **Llama 3.2 1B** | 808 MB | 3 GB+ | Ultra-fast, testing |

- **Import your own GGUF models** from device storage
- **Real downloads** with progress, pause/resume, integrity verification
- **Device compatibility checks** — warns before downloading incompatible models
- **Green checkmark** on downloaded models

### Privacy
- **Zero analytics** — no SDKs, no tracking, no telemetry
- **Zero accounts** — no login, no registration
- **Zero cloud** — all AI processing on-device
- **HTTPS only** — encrypted model downloads
- **Your controls** — delete conversations, models, or all data anytime
- **Open source** — verify everything yourself

### Design
- **Dark mode first** — anthrazit, ice-blue accents
- **Model identity colors** — each model has its own accent
- **Thinking animation** — orbital indicator while AI processes
- **Smooth transitions** — fade between tabs, slide for details
- **Professional typography** — clean, readable, minimal

---

## Screenshots

*Install the app to see it in action.*

---

## Building from Source

```bash
# Clone with submodules
git clone --recursive https://github.com/BEKO2210/Android_Engineer.git
cd Android_Engineer

# Build debug APK
./gradlew assembleDebug

# Build release APK (signed with debug key)
./gradlew assembleRelease

# Run tests
./gradlew testDebugUnitTest
```

**Requirements:** Android SDK 35, NDK 27.2, CMake 3.22.1, JDK 17+

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Inference | llama.cpp via JNI (ARM64 NEON) |
| Architecture | MVVM + Clean Architecture (14 modules) |
| DI | Hilt |
| Database | Room |
| Preferences | DataStore |
| Downloads | WorkManager + OkHttp |
| Build | Gradle Kotlin DSL + Version Catalog |
| CI | GitHub Actions |

---

## Project Structure

```
app/                    Application shell, navigation, DI
core/
  core-common/          Utilities, device capability, formatting
  core-model/           Domain entities
  core-data/            Repositories
  core-database/        Room DB, DAOs, entities
  core-datastore/       DataStore preferences
  core-network/         OkHttp client
  core-ui/              Design system, theme, shared components
feature/
  feature-chat/         Chat screen, streaming, markdown
  feature-models/       Model catalog, detail, downloads
  feature-settings/     Settings, privacy, storage, import
  feature-onboarding/   First-launch flow
inference/
  inference-llamacpp/   llama.cpp JNI bridge, native build
download/
  download-engine/      WorkManager workers, download manager
```

---

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for full release history.

---

## Privacy Policy

See [PRIVACY.md](PRIVACY.md) for the complete privacy policy.

**TL;DR:** Your data never leaves your device. No analytics. No tracking. No cloud. No accounts.

---

## Contact

Belkis Aslani — belkis.aslani@gmail.com

---

## License

App code: See repository license.
Model licenses: See [MODELS.md](MODELS.md) for individual model licenses.
