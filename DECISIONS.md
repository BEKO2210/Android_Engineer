# Architecture Decision Records (ADR)

## ADR-001: App Name — Slate

**Date:** 2026-04-05
**Status:** Accepted

**Context:** We need a short, professional, privacy-associated app name for a local LLM Android app.

**Candidates evaluated:** Cortex, Vessel, Anvil AI, Haven AI, Slate

**Decision:** **Slate**
- "Clean Slate" metaphor aligns with no-tracking, no-baggage philosophy
- Short, language-independent, extensible (Slate Pro, Slate Models)
- No trademark collisions in the Android/privacy space
- Haven AI was runner-up but collides with Guardian Project's Haven app (same target audience)
- Cortex collides with Palo Alto Networks product

**Package:** `dev.slate.ai` (reverse-domain style under a domain we control, avoids `com.slate.ai` collision risk)

---

## ADR-002: Inference Stack — llama.cpp (GGUF)

**Date:** 2026-04-05
**Status:** Accepted

**Context:** We need an on-device LLM inference engine that works across a broad range of Android devices.

**Options evaluated:**
| Stack | Verdict |
|---|---|
| llama.cpp | **Chosen** — most mature, broadest device support, largest GGUF model ecosystem |
| LiteRT-LM (Google AI Edge) | Tracked alternative — strong GPU/NPU acceleration but flagship-focused, limited model format |
| MLC LLM | Rejected for MVP — complex setup (Rust+TVM), cannot run in emulator, UI freezing issues |
| ExecuTorch | Rejected for MVP — less mature for LLM use cases, complex model preparation |
| ONNX Runtime Mobile | Rejected for MVP — requires format conversion, more overhead |

**Decision:** llama.cpp via JNI for MVP.

**Tracked alternative:** Google AI Edge / LiteRT-LM is a strong future option, particularly for devices with NPU support (Snapdragon 8 Gen 3+, Tensor G3+). We will revisit this after MVP when device-specific acceleration becomes a priority. It is not dismissed — it is deferred because the MVP prioritizes broad device compatibility over peak performance on flagships.

**Consequences:**
- CPU-only inference in MVP (no GPU/NPU offload)
- GGUF model format is our primary format
- JNI bridge required (native C++ compilation via NDK)

---

## ADR-003: Target ABI — arm64-v8a (MVP Scope)

**Date:** 2026-04-05
**Status:** Accepted

**Context:** llama.cpp must be cross-compiled for Android. Supporting multiple ABIs increases build complexity and APK size.

**Decision:** MVP targets **arm64-v8a only**.

This is a **scoped MVP decision**, not a universal Android claim. arm64-v8a covers approximately 95% of active Android devices as of 2025. armeabi-v7a (32-bit ARM) and x86_64 (emulator/Chromebook) are deferred to post-MVP.

**Consequences:**
- APK will not run on 32-bit-only devices (very rare in 2025+)
- Emulator testing requires arm64 emulator image or physical device
- Post-MVP: add armeabi-v7a for broader reach, x86_64 for emulator convenience

---

## ADR-004: Download Mechanism — WorkManager + OkHttp

**Date:** 2026-04-05
**Status:** Accepted

**Context:** Model files are 0.6–3 GB. We need reliable downloads with progress, resume, integrity verification, and lifecycle resilience.

**Why not Android DownloadManager:**
DownloadManager is not the best fit for this product because we need fine-grained control over:
- Real-time progress observation (DownloadManager lacks a progress observation API)
- Download state persistence in our own database
- SHA-256 integrity verification after download
- Resume with HTTP Range headers under our control
- Chained post-download verification workers

DownloadManager is a valid Android API, but it does not provide the control surface our product requires.

**Download path by API level:**

| API Level | Mechanism | Details |
|---|---|---|
| **34+ (Android 14+)** | User-Initiated Data Transfer (UIDT) | JobScheduler-based API for user-initiated large transfers. Requires `RUN_USER_INITIATED_JOBS` permission. Jobs can only be scheduled when app is visible. Not subject to App Standby Bucket quotas. This is a **JobScheduler feature**, not a WorkManager feature. |
| **26–33 (Android 8–13)** | WorkManager with Foreground Service | Standard expedited work with `FOREGROUND_SERVICE_TYPE_DATA_SYNC`. Persistent notification required. Subject to battery optimization constraints. |

**Fallback strategy:**
```
if (SDK >= 34) → UIDT via JobScheduler (best priority, longest runtime)
else           → WorkManager + Foreground Service (reliable, well-tested)
```

Both paths use OkHttp for the actual HTTP transfer with Range header resume support.

**Consequences:**
- Two code paths for download scheduling (UIDT vs WorkManager)
- Shared OkHttp download logic regardless of scheduler
- Room DB persists state for both paths

---

## ADR-005: Package Naming — dev.slate.ai

**Date:** 2026-04-05
**Status:** Accepted

**Context:** Android applicationId must be a unique reverse-domain identifier. Using `com.slate.ai` risks collision with domains we don't control.

**Decision:** `dev.slate.ai` — uses a domain format we can reasonably register and control.

**Consequences:**
- All module packages follow `dev.slate.ai.*` convention
- Play Store listing will use this applicationId
- Can be changed before first Play Store submission if a different domain is secured

---

## ADR-006: Quantization Standard — Q4_K_M

**Date:** 2026-04-05
**Status:** Accepted

**Context:** GGUF models come in multiple quantization levels. We need a default that balances quality, size, and performance on mobile.

**Decision:** Q4_K_M as the default quantization for all models in the catalog.

**Rationale:**
- ~68% size reduction vs FP16
- Good quality (perplexity 8.57 on WikiText-2)
- Fast inference on mobile CPUs
- Industry standard for mobile GGUF deployment
- Q5_K_M offered as optional "high quality" variant for flagship devices
