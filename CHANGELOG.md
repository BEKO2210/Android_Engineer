# Changelog

All notable changes to Slate are documented here.

---

## [Unreleased]

### Added
- **Gemma 3 1B** model support with correct chat template
- **Custom GGUF import** — load your own models from device storage
- **Conversation starters** — 8 language chips (EN, DE, FR, ES, TR, AR, ZH, JA)
- **Markdown tables** — pipe-delimited tables rendered with headers and alternating rows
- **Syntax highlighting** in code blocks (keywords, strings, comments, numbers)
- **Copy button** on every message and code block
- **Scroll-to-bottom FAB** when scrolled up during streaming
- **Model accent colors** — each model has its own identity color
- **Thinking indicator** — orbital animation when AI is processing
- **Input field glow** — subtle accent color glow on focus
- **Animated send button** — spring scale + color transition
- **Navigation transitions** — fade between tabs, slide for detail screens
- **Card press effect** — subtle scale feedback on model cards
- **Device capability detection** — RAM/storage checks, tier classification
- **Onboarding flow** — first-launch device assessment + model recommendation
- **Auto-versioning** — version auto-increments from git commit count

### Changed
- Chat header redesigned: thinner, with logo icon and "Slate" glow heartbeat
- Assistant bubbles use model-specific tinted backgrounds
- Model catalog reordered: Qwen 2.5 3B recommended first
- Temperature lowered to 0.3 for better instruction following
- Repeat penalty increased to 1.15 to prevent repetition
- Few-shot examples baked into every prompt for consistent output quality
- Privacy screen redesigned with DO/DON'T cards and detailed sections
- Settings shows dynamic version from PackageInfo
- Release APK now signed (installable without debug mode)
- APKs renamed to Slate-{version}-{type}.apk in CI

### Fixed
- Models screen crash (AnimatedContent + LazyColumn conflict)
- Download crash (missing SystemForegroundService declaration for Android 14+)
- Chat flickering during streaming (scroll animation on every token)
- Theme toggle not working (preference not read into SlateTheme)
- NavHost crash (reactive startDestination)
- SmolLM2 download URL (wrong repo → 401)
- System prompt tokens leaking into output (<|system|>, <|im_start|>)
- Model simulating fake user conversations
- Context overflow crash on long conversations
- Chat history toggle not enforced
- Delete loaded model not detected
- DeviceCapability crash on some emulators

---

## [1.0.0] — 2026-04-06

### Initial Release
- Local LLM inference via llama.cpp (ARM64 NEON)
- Model catalog: SmolLM2 1.7B, Qwen 2.5 3B, Phi-3 Mini 3.8B, Llama 3.2 1B
- Real download engine: progress, resume, SHA-256 verification
- Chat with streaming token display
- Dark mode first design
- Privacy-first: zero analytics, zero tracking
- 14-module clean architecture
- 59 automated tests
