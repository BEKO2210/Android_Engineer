# Test Plan

## Unit Tests

### ViewModels
- [ ] ModelsViewModel: catalog loading, filtering, state transitions
- [ ] ChatViewModel: message sending, streaming state, stop/regenerate
- [ ] SettingsViewModel: preference changes, storage calculations
- [ ] DownloadViewModel: progress observation, cancel, retry

### Repositories
- [ ] ModelRepository: catalog parsing, model lookup
- [ ] DownloadRepository: state persistence, status transitions
- [ ] ChatRepository: conversation CRUD, message ordering
- [ ] PreferencesRepository: read/write preferences

### Download Engine
- [ ] DownloadWorker: successful download flow
- [ ] DownloadWorker: resume from partial file
- [ ] DownloadWorker: retry on network error
- [ ] VerificationWorker: SHA-256 match
- [ ] VerificationWorker: SHA-256 mismatch → file deleted
- [ ] StorageUtils: free space check accuracy

### Inference Engine
- [ ] LlamaCppEngine: model load success
- [ ] LlamaCppEngine: model load failure (bad file)
- [ ] LlamaCppEngine: generate token stream
- [ ] LlamaCppEngine: stop generation mid-stream
- [ ] LlamaCppEngine: unload model, memory freed

## UI Tests (Compose)

- [ ] Navigation: all bottom nav destinations reachable
- [ ] Models screen: catalog renders, cards clickable
- [ ] Model detail: download button visible, info displayed
- [ ] Chat screen: message input, send, message list
- [ ] Settings screen: toggles functional

## Integration Tests

- [ ] Full download flow: tap download → progress → verification → complete
- [ ] Download resume: kill app during download → restart → resumes
- [ ] Download cancel: cancel mid-download → file cleaned up
- [ ] Model load + chat: download → load → send message → receive response
- [ ] Model switch: unload model A → load model B → chat works
- [ ] Model delete: delete model → storage freed → catalog updated

## Edge Case Tests

- [ ] Insufficient storage: download blocked with clear message
- [ ] Insufficient RAM: model load fails gracefully
- [ ] Corrupted download: verification fails → user informed
- [ ] No network: download button disabled or shows offline state
- [ ] App killed during inference: no crash on restart
- [ ] Rotation during generation: state preserved

## Device Matrix (Target)

| Device Class | Example | RAM | API | Priority |
|---|---|---|---|---|
| Budget | Samsung A14 | 4 GB | 33 | P1 |
| Mid-range | Pixel 6a | 6 GB | 34 | P0 |
| Flagship | Pixel 8 Pro | 12 GB | 35 | P0 |
| Emulator | arm64 image | 4 GB | 34 | P0 (dev) |
