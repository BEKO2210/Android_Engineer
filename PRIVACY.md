# Privacy Documentation

## Core Principles

1. **Offline-first**: All AI inference runs locally. No cloud processing.
2. **No tracking**: Zero analytics SDKs, no persistent device identifiers.
3. **Data minimization**: Only store what's strictly necessary for app function.
4. **User control**: Users can delete all local data at any time.
5. **Transparency**: Clear documentation of what data exists and where.

## Data Inventory

| Data | Storage | Purpose | User Deletable |
|---|---|---|---|
| Downloaded models | App-specific external storage | Local inference | Yes (per model) |
| Chat history | Room DB (app-internal) | Conversation continuity | Yes (per conversation or all) |
| App preferences | DataStore (app-internal) | Settings persistence | Yes (clear app data) |
| Download state | Room DB (app-internal) | Resume/track downloads | Auto-cleaned on completion |

## Network Activity

| Connection | When | Purpose | Data Sent |
|---|---|---|---|
| Hugging Face HTTPS | User initiates model download | Download model file | HTTP GET request (URL only) |
| None | During inference | N/A | Nothing — fully offline |
| None | During chat | N/A | Nothing — fully offline |

## What We Do NOT Do

- No analytics or telemetry (not even opt-in in MVP)
- No crash reporting (deferred; if added, explicit opt-in only)
- No persistent device identifiers
- No user accounts or authentication
- No cloud inference or API calls
- No advertising SDKs
- No third-party tracking SDKs
- No data sharing with any third party
- No cleartext HTTP connections (enforced via network security config)

## Technical Enforcement

- `network_security_config.xml`: Cleartext traffic disabled globally
- No INTERNET permission beyond model downloads
- App-specific storage only (no shared storage access)
- No READ_EXTERNAL_STORAGE or WRITE_EXTERNAL_STORAGE permissions
- ProGuard/R8 stripping of unused framework components

## Play Store Data Safety Declaration

- **Data collected:** None
- **Data shared:** None
- **Security practices:** Data encrypted in transit (HTTPS), app-specific storage
- **Data deletion:** Users can delete all data via app settings or by uninstalling
