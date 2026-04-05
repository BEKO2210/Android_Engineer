# Slate

<p align="center">
  <img src="app/src/main/assets/Icon_logo.png" width="128" alt="Slate Logo" />
</p>

<p align="center">
  <strong>v1.1.0</strong> — Private AI on your device. Download and run local LLMs entirely offline.
</p>

## What is Slate?

Slate is an Android app that lets you download and run AI language models directly on your phone. All inference happens locally — your conversations never leave your device.

## Features

- **Local inference** — Run LLMs on-device via llama.cpp (GGUF format)
- **Model catalog** — Browse and download models optimized for mobile
- **Real downloads** — Progress tracking, pause/resume, integrity verification
- **Chat interface** — Streaming token display, stop/regenerate, conversation history
- **Dark mode** — Professional dark-first design with ice-blue accents

## Privacy

Slate is privacy-first by design:

- **No cloud** — All AI processing happens on your device
- **No tracking** — Zero analytics, no advertising IDs, no telemetry
- **No accounts** — No login, registration, or user identifiers
- **No data collection** — We don't collect, transmit, or sell any data
- **Your controls** — Delete conversations, models, or all data at any time
- **HTTPS only** — Model downloads use encrypted connections
- **App-private storage** — Other apps cannot access your data

See [PRIVACY.md](PRIVACY.md) for the full privacy policy.

## Supported Models

| Model | Size | Device |
|---|---|---|
| SmolLM2 1.7B | ~1.1 GB | 4 GB+ RAM |
| Qwen 2.5 3B | ~1.9 GB | 6 GB+ RAM |
| Phi-3 Mini 3.8B | ~2.3 GB | 8 GB+ RAM |

All models use Q4_K_M quantization for optimal mobile performance.

## Tech Stack

- Kotlin + Jetpack Compose + Material 3
- llama.cpp via JNI (ARM64 NEON)
- Room + DataStore for persistence
- WorkManager + OkHttp for downloads
- Hilt for dependency injection
- 14-module clean architecture

## Building

```
./gradlew assembleDebug
```

Requires Android SDK 35, NDK 27.2, CMake 3.22.1.

## Contact

Belkis Aslani — belkis.aslani@gmail.com

## License

See individual model licenses in [MODELS.md](MODELS.md).
