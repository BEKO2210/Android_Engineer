# Test Plan

## Unit Tests

### ViewModels
- [ ] ModelsViewModel: catalog loading, filtering, device compatibility check
- [ ] ChatViewModel: message send, streaming state, stop, regenerate, clear
- [ ] SettingsViewModel: preference changes, clear history
- [ ] ModelDetailViewModel: download states, error handling
- [ ] OnboardingViewModel: device info, tier detection, complete flag
- [ ] StorageViewModel: model list, delete, storage refresh

### Repositories
- [ ] ModelRepository: JSON parsing, model lookup
- [ ] ChatRepository: conversation CRUD, message ordering, cascade delete
- [ ] DownloadRepository: state persistence, status transitions

### Download Engine
- [ ] DownloadWorker: successful full download
- [ ] DownloadWorker: resume from partial file (Range header)
- [ ] DownloadWorker: retry on 5xx, fail on 4xx
- [ ] VerificationWorker: SHA-256 match → COMPLETE
- [ ] VerificationWorker: SHA-256 mismatch → file deleted, FAILED
- [ ] VerificationWorker: blank hash → skip verification
- [ ] StorageUtils: free space check, model directory creation
- [ ] ModelDownloadManager: enqueue, cancel, resume, delete
- [ ] ModelDownloadManager: stale download recovery

### Inference Engine
- [ ] LlamaCppEngine: model load with valid file
- [ ] LlamaCppEngine: model load with invalid path → Error
- [ ] LlamaCppEngine: model load with corrupted file → Error
- [ ] LlamaCppEngine: insufficient RAM → InsufficientMemoryException
- [ ] LlamaCppEngine: generate returns tokens
- [ ] LlamaCppEngine: stop generation mid-stream
- [ ] LlamaCppEngine: unload frees resources
- [ ] LlamaCppEngine: reload after unload works
- [ ] LlamaCppEngine: UnsatisfiedLinkError on incompatible device

## UI Tests (Compose)

- [ ] Navigation: all 3 bottom nav destinations reachable
- [ ] Navigation: model detail back button works
- [ ] Navigation: privacy/storage back button works
- [ ] Onboarding: shows on first launch, not on subsequent
- [ ] Models: filter chips change displayed list
- [ ] Models: compatibility warnings visible for incompatible models
- [ ] Model detail: all specs displayed, download button visible
- [ ] Chat: send button disabled with empty input
- [ ] Chat: stop button visible during generation
- [ ] Settings: toggles update preferences
- [ ] Settings: clear history dialog confirms before delete

## Integration / Scenario Tests

### First Launch
- [ ] Onboarding shows device tier and recommended model
- [ ] "Get started" navigates to chat
- [ ] Onboarding does not show again after completion

### Download Flow
- [ ] Tap download → progress bar → verification → complete
- [ ] Insufficient storage → warning dialog, no download
- [ ] Kill app during download → reopen → status shows PAUSED
- [ ] Resume paused download → continues from partial file
- [ ] Cancel download → file cleaned up (or .part preserved for resume)
- [ ] Failed verification → error message, file deleted
- [ ] Network error → retry with backoff

### Inference Flow
- [ ] Load downloaded model → Ready state
- [ ] Send prompt → streaming response in chat
- [ ] Stop mid-generation → partial text preserved
- [ ] Regenerate → last response replaced with new generation
- [ ] Unload model → Idle state
- [ ] Load invalid/corrupted file → Error state with message
- [ ] Insufficient RAM → Error with explanation

### Chat Persistence
- [ ] Send messages → close app → reopen → messages restored
- [ ] Clear conversation → all messages gone
- [ ] Switch model → new conversation started
- [ ] Disable chat history → new messages not persisted

### Settings
- [ ] Clear chat history → all conversations deleted
- [ ] Delete model from storage → model removed, storage updated
- [ ] Toggle dark theme → theme changes
- [ ] Toggle offline mode → setting persisted

### Error Recovery
- [ ] Broken download state on app start → marked as PAUSED
- [ ] Delete model that is currently loaded → handled gracefully
- [ ] Generation error → error state shown, can retry
- [ ] Empty model catalog → error state with message

## Device Matrix (Target)

| Device Class | Example | RAM | API | Priority |
|---|---|---|---|---|
| Low | Samsung A14 | 4 GB | 33 | P1 |
| Basic | Pixel 6a | 6 GB | 34 | P0 |
| Standard | Pixel 7 | 8 GB | 34 | P0 |
| High | Pixel 8 Pro | 12 GB | 35 | P0 |
| Emulator | arm64 image | 4 GB | 34 | P0 (dev) |

## Accessibility Checks

- [ ] All buttons have content descriptions
- [ ] Download progress announced to screen readers
- [ ] Model cards describe model name and tier
- [ ] Navigation items have labels
- [ ] Color contrast meets WCAG AA (dark theme)
- [ ] Touch targets minimum 48dp
- [ ] Headings marked with heading() semantics
