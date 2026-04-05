# Release Notes — Slate v1.0.0

**Date:** April 2026
**Build:** v1.0.0 (versionCode 1)
**ABI:** arm64-v8a only
**Min SDK:** Android 8.0 (API 26)
**Target SDK:** Android 15 (API 35)

---

## What is Slate?

Slate is a privacy-first Android app for running local AI language models entirely on-device. No cloud, no tracking, no accounts.

## Features

- Download GGUF models from Hugging Face with real progress, resume, and integrity verification
- Run llama.cpp inference locally via JNI (ARM64 NEON optimized)
- Chat interface with streaming token display, stop, regenerate
- Message persistence across app restarts
- Onboarding with device capability assessment
- Privacy-first: zero analytics, zero telemetry, HTTPS-only downloads
- Dark mode first design with ice-blue accents

## Supported Models

| Model | Size | Min RAM | License |
|---|---|---|---|
| SmolLM2 1.7B (Q4_K_M) | 1.1 GB | 4 GB | Apache 2.0 |
| Llama 3.2 1B (Q4_K_M) | 0.6 GB | 3 GB | Meta CL |
| Qwen 2.5 3B (Q4_K_M) | 1.9 GB | 6 GB | Apache 2.0 |
| Phi-3 Mini 3.8B (Q4_K_M) | 2.3 GB | 8 GB | MIT |

---

## Build Instructions

### Prerequisites

- Android SDK (compileSdk 35)
- Android NDK 27.2.12479018
- CMake 3.22.1
- Java 17+

### Debug Build

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk (40 MB)
```

### Release Build (unsigned)

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk (23 MB)
```

### Signing for Distribution

1. Generate a keystore:
```bash
keytool -genkey -v -keystore keystore/slate-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias slate -storepass YOUR_PASSWORD -keypass YOUR_PASSWORD
```

2. Uncomment the signing config in `app/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("keystore/slate-release.jks")
        storePassword = System.getenv("SLATE_KEYSTORE_PASSWORD") ?: ""
        keyAlias = "slate"
        keyPassword = System.getenv("SLATE_KEY_PASSWORD") ?: ""
    }
}
```

3. Set environment variables and build:
```bash
export SLATE_KEYSTORE_PASSWORD=your_password
export SLATE_KEY_PASSWORD=your_password
./gradlew assembleRelease
```

4. Alternatively, sign manually:
```bash
apksigner sign --ks keystore/slate-release.jks \
  --out app-release-signed.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## Install Instructions

### Via ADB (debug or signed release)

```bash
adb install app-debug.apk
# or
adb install app-release-signed.apk
```

### Manual Install

Transfer the APK to the device and open it. Enable "Install from unknown sources" if prompted.

### First Launch

1. Onboarding screen shows device capability assessment
2. Tap "Get started"
3. Go to Models tab → select a model → tap Download
4. Wait for download + verification to complete
5. Go to Chat tab → tap "Load [model name]"
6. Type a message and send

---

## Testing Checklist (Manual, on ARM64 Device)

- [ ] Install APK cleanly
- [ ] Onboarding shows on first launch
- [ ] Tap "Get started" → navigates to Chat
- [ ] Navigate to Models → see 4 models listed
- [ ] Tap a model → see detail screen
- [ ] Download a model → real progress bar, notification
- [ ] Download completes → "Ready to use" shown
- [ ] Navigate to Chat → load the model
- [ ] Send a prompt → streaming response appears
- [ ] Tap Stop → generation stops, partial text kept
- [ ] Tap Regenerate → new response
- [ ] Kill app → reopen → messages restored
- [ ] Navigate to Settings → clear history works
- [ ] Navigate to Storage → model listed, can delete
- [ ] Navigate to Privacy → policy text displayed

---

## Known Limitations

### Architecture
- **arm64-v8a only.** The app will not install on 32-bit ARM devices or x86 emulators without ARM translation. This covers approximately 95% of active Android devices.

### Inference
- **CPU-only.** No GPU or NPU offload in this release. Performance is 4-20 tokens/second depending on model and device.
- **Single model at a time.** Only one model can be loaded into memory. Loading a second model unloads the first.
- **No chat history template.** Prompt is built as simple `User:/Assistant:` format without model-specific chat templates. Quality may vary.

### Downloads
- **No pause button in notification.** Downloads can be paused from within the app but not from the notification shade.
- **SHA-256 hashes not yet populated.** Model catalog has empty SHA-256 fields, so integrity verification is skipped. Hash values should be added before production release.

### Device Support
- **LOW tier devices (< 4 GB RAM)** will show a warning but are not blocked. Performance may be very poor.
- **Android 8-9 (API 26-28)** are technically supported but untested.

### UI
- **No landscape optimization.** The app works in landscape but layout is not optimized.
- **Markdown rendering is minimal.** Only paragraphs, code blocks, inline code, and bold are supported.
- **No conversation list.** Only the most recent conversation per model is loaded.

### Privacy
- **No data export feature yet.** Users can clear data but cannot export conversations.
- **No certificate pinning.** HTTPS is enforced but connections are not pinned to specific certificates.

---

## Go / No-Go Assessment

| Criterion | Status |
|---|---|
| Release build compiles | GO |
| R8 minification works | GO |
| Native libs in APK | GO |
| ProGuard rules cover JNI/Room/Hilt | GO |
| Model catalog present | GO |
| Download engine functional | GO |
| Inference engine functional | GO |
| Chat UI functional | GO |
| Settings/Privacy functional | GO |
| Onboarding functional | GO |
| 59 unit tests passing | GO |
| 111/118 scenarios verified | GO |
| No release blockers | GO |
| Known limitations documented | GO |

**Recommendation: GO for limited release / internal testing.**

Before Play Store submission:
- [ ] Generate and configure release signing keystore
- [ ] Populate SHA-256 hashes in model_catalog.json
- [ ] Test on 3+ physical devices (budget, mid-range, flagship)
- [ ] Complete Play Store data safety form
- [ ] Add proper app icon (current is placeholder)
- [ ] Review Meta Community License for Llama 3.2 commercial use

---

## Contact

Belkis Aslani — belkis.aslani@gmail.com
