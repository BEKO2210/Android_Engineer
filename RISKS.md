# Risk Register

## R1: NDK Build Complexity — MITIGATED
**Description:** Cross-compiling llama.cpp for Android ARM64 via CMake/NDK.
**Status:** Resolved in Phase 5. llama.cpp builds successfully for arm64-v8a with NDK 27.2, CMake 3.22.1. Key fixes: no -ffast-math flag, no common.h dependency, direct llama_tokenize API.
**Residual risk:** NDK version updates may break build. Pinned to specific versions.

## R2: OOM Kills During Inference — MITIGATED
**Description:** Android's low-memory killer terminates app during model load/inference.
**Mitigation applied:**
- RAM check (1.3x file size) via ActivityManager before model load
- Device tier detection (LOW/BASIC/STANDARD/HIGH) in onboarding
- Compatibility warnings on model cards
- mmap enabled by default (OS manages memory)
- Single-model-loaded policy
**Residual risk:** Background apps can consume RAM between check and load.

## R3: Model File Size vs. Storage — MITIGATED
**Description:** Budget devices have limited storage; smallest model is 600MB.
**Mitigation applied:**
- Pre-download storage check with 10% margin
- Storage screen shows per-model usage + available space
- Compatibility warnings on model cards when storage insufficient
- Onboarding recommends device-appropriate model

## R4: Download Stability over Mobile Networks — MITIGATED
**Description:** Downloads of 0.6-3 GB can be interrupted.
**Mitigation applied:**
- HTTP Range resume (partial .part files preserved)
- WorkManager retry with exponential backoff
- Room-persisted download state survives app restart
- Stale download recovery on app launch
- Only retryable HTTP codes trigger retry; permanent failures fail fast

## R5: Thermal Throttling During Inference — ACTIVE (Medium)
**Description:** Sustained inference heats device, causing throttling.
**Current mitigation:** Thread count set to cores-2 (min 2). No continuous background inference.
**Future:** Monitor token rate drops, suggest cooldown.

## R6: Hugging Face URL Stability — ACTIVE (Low)
**Description:** Direct download URLs may change.
**Current mitigation:** URLs in local JSON asset, easily updatable.

## R7: License Compliance — ACTIVE (Medium)
**Description:** Models have different licenses.
**Current mitigation:** License displayed on model detail screen. MVP prioritizes Apache 2.0/MIT models.

## R8: Corrupted Downloads — MITIGATED
**Description:** Network errors or disk issues can corrupt model files.
**Mitigation applied:**
- SHA-256 verification after download (when hash available)
- Hash mismatch → file deleted, status FAILED, user informed
- Retry available from error state
- .part files cleaned up on non-resumable failures

## R9: Unsupported Devices — MITIGATED
**Description:** Low-end devices cannot run LLMs effectively.
**Mitigation applied:**
- Device tier detection on first launch
- Clear warning for LOW tier devices
- Model compatibility indicators on catalog cards
- UnsatisfiedLinkError catch for non-ARM64 devices
- Graceful error states for load failures

## R10: Stale App State — MITIGATED
**Description:** App killed during download/inference leaves inconsistent state.
**Mitigation applied:**
- recoverStaleDownloads() on app launch (marks orphaned DOWNLOADING as PAUSED)
- Inference state defaults to Idle on restart (model must be reloaded)
- Partial files preserved for resume
