# Risk Register

## R1: NDK Build Complexity — HIGH

**Description:** Cross-compiling llama.cpp for Android ARM64 via CMake/NDK is non-trivial. Build failures from GGML backend selection, missing SIMD flags, or NDK version mismatches can block progress.

**Mitigation:**
- Pin to a specific llama.cpp release tag (not `master`)
- Reference working CMake configs from existing Android apps (SmolChat)
- Test native build early in Phase 1, not Phase 5
- Keep pre-built `.so` fallback for development

**Status:** Active — will be tested in Phase 1

---

## R2: OOM Kills During Inference — HIGH

**Description:** Android's low-memory killer will terminate the app if model loading + inference consumes too much RAM. Models need ~1.3x their file size in RAM.

**Mitigation:**
- RAM check via `ActivityManager.getMemoryInfo()` before model load
- Use `mmap` to let OS manage memory mapping
- Recommend smaller models on low-RAM devices
- Graceful error handling on load failure

**Status:** Active — addressed in Phase 5

---

## R3: Model File Size vs. Storage — HIGH

**Description:** Even "tiny" models are 600MB+. Budget devices often have limited free storage.

**Mitigation:**
- Pre-download storage check with clear UI messaging
- Default-recommend 1B model for low-storage devices
- Model deletion with immediate space reclamation
- Clear size information in model catalog

**Status:** Active — addressed in Phase 3-4

---

## R4: Download Stability over Mobile Networks — MEDIUM

**Description:** Downloads of 0.6-3 GB over mobile networks can be interrupted by connectivity changes, background killing, or timeouts.

**Mitigation:**
- HTTP Range header resume support
- WorkManager automatic retry with exponential backoff
- Room-persisted download state survives app restarts
- Clear UI for interrupted/resumable downloads

**Status:** Active — addressed in Phase 4

---

## R5: Thermal Throttling During Inference — MEDIUM

**Description:** Sustained LLM inference is CPU-intensive, causing device heating and performance throttling.

**Mitigation:**
- Monitor token generation rate; warn on significant drops
- Limit inference thread count to `availableProcessors - 2`
- No automatic background inference
- Future: investigate GPU offload for heat distribution

**Status:** Active — addressed in Phase 8

---

## R6: Hugging Face URL Stability — LOW

**Description:** Direct download URLs from Hugging Face could change format or require authentication in the future.

**Mitigation:**
- Abstract download URLs behind a model catalog config
- Model catalog is a local JSON asset, easily updatable
- No hardcoded URLs in UI or business logic

**Status:** Monitoring

---

## R7: License Compliance — MEDIUM

**Description:** Different models have different licenses (Apache 2.0, MIT, Meta Community License, Gemma Terms). Commercial use restrictions may apply.

**Mitigation:**
- Document license for every model in MODELS.md
- Display license info on model detail screen
- MVP prioritizes Apache 2.0 / MIT licensed models
- Legal review before Play Store release

**Status:** Active — tracked in MODELS.md
