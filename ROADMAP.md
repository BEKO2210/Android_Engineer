# Roadmap

## Phase 0 — Research + Product Definition ✅
- [x] On-device inference stack research and comparison
- [x] Model selection and sizing analysis
- [x] Download architecture research
- [x] App naming decision
- [x] Architecture decisions documented
- [x] Risk assessment complete

**Output:** DECISIONS.md, RISKS.md, MODELS.md, ROADMAP.md

---

## Phase 1 — Project Scaffold (Current)
- [ ] Android project with Gradle Kotlin DSL
- [ ] Module structure (core/*, feature/*, inference, download)
- [ ] Version catalog (libs.versions.toml)
- [ ] Hilt DI setup
- [ ] Material 3 Dark Theme + Design Tokens
- [ ] MainActivity with Compose Scaffold + Bottom Navigation
- [ ] Network Security Config (HTTPS only)
- [ ] Empty screen shells for all destinations

**Output:** App compiles, runs, shows themed scaffold with navigation

---

## Phase 2 — Design System
- [ ] Shared components: SlateButton, SlateCard, SlateProgressBar, SlateTopBar
- [ ] Skeleton loading shimmer
- [ ] Animation primitives (scale, fade, crossfade)
- [ ] Download state UI components (progress, paused, error, complete)
- [ ] Dialog and bottom sheet components

**Output:** Visually complete UI component library

---

## Phase 3 — Model Catalog
- [ ] Room database schema (Models, Downloads, Conversations, Messages)
- [ ] Model repository with hardcoded catalog (JSON asset)
- [ ] Models screen: list with device tier tags
- [ ] Model detail screen
- [ ] Storage info display

**Output:** Users can browse model catalog

---

## Phase 4 — Download Engine
- [ ] ModelDownloadManager + ModelDownloadWorker
- [ ] OkHttp with Range header resume
- [ ] SHA-256 verification worker (chained)
- [ ] Foreground notification with progress
- [ ] Room-backed download state persistence
- [ ] UIDT support (API 34+) with WorkManager fallback (API 26-33)
- [ ] Storage space pre-check
- [ ] Download UI on Models screen

**Output:** Real model downloads from Hugging Face with progress, resume, verification

---

## Phase 5 — Local Model Integration
- [ ] llama.cpp git submodule + CMake build for arm64-v8a
- [ ] JNI bridge (load, generate, stop, unload)
- [ ] LlamaCppEngine Kotlin API with Flow<InferenceToken>
- [ ] Memory check before model load
- [ ] Test screen for inference validation

**Output:** First locally running GGUF model

---

## Phase 6 — Chat UI
- [ ] Chat screen with streaming token display
- [ ] User input bar with send button
- [ ] Stop generation / regenerate
- [ ] Auto-scroll during generation
- [ ] tok/s display
- [ ] Basic markdown rendering

**Output:** Real offline chat experience

---

## Phase 7 — Privacy + Settings
- [ ] Settings screen (storage, theme, about, privacy policy)
- [ ] Chat history persistence (Room)
- [ ] Clear/export history
- [ ] Opt-in only telemetry (default: none)
- [ ] Data minimization enforcement

**Output:** Privacy-first behavior complete

---

## Phase 8 — Hardening
- [ ] Edge cases: no storage, corrupted files, incompatible devices
- [ ] Onboarding flow (first launch, device check, model recommendation)
- [ ] Accessibility (content descriptions, keyboard nav)
- [ ] Error states for all screens

**Output:** Robust app, not a demo

---

## Phase 9 — Tests
- [ ] Unit tests (ViewModels, Repositories, Download logic)
- [ ] UI tests (Compose, Navigation)
- [ ] Download tests (resume, error, cancel)
- [ ] Inference tests (load, generate, OOM)

**Output:** Reliable test coverage

---

## Phase 10 — Release
- [ ] App icon (minimalist, Slate branding)
- [ ] ProGuard/R8 rules
- [ ] App signing config
- [ ] APK split per ABI
- [ ] Data safety declaration
- [ ] Release notes

**Output:** Signed release APK
