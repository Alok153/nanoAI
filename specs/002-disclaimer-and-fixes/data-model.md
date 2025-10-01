# Data Model: First-launch Disclaimer and Fixes

**Feature**: First-launch Disclaimer and Fixes
**Spec**: /home/vijay/Personal/myGithub/nanoAI/specs/002-disclaimer-and-fixes/spec.md
**Date**: 2025-10-01

## Entities

### PrivacyPreference
- description: Stores user privacy preferences and first-launch disclaimer acknowledgement.
- fields:
  - `exportWarningsDismissed: Boolean` (existing)
  - `telemetryOptIn: Boolean` (existing)
  - `consentAcknowledgedAt: Instant?` (new) — timestamp when disclaimer acknowledged
  - `disclaimerShownCount: Int` (new) — how many times disclaimer was presented
- constraints:
  - `consentAcknowledgedAt` is null until acknowledged

### BackupBundle (logical)
- description: Logical representation of an import/export bundle
- fields:
  - `personas: List<PersonaProfile>`
  - `apiProviders: List<APIProviderConfig>`
  - `settings: Map<String, Any>`
- format: JSON root object with named keys. Optionally wrapped in ZIP when additional files included.

### ImportJob (transient)
- description: Represents a running import operation (progress, errors)
- fields:
  - `jobId: UUID`
  - `status: enum {PENDING, RUNNING, COMPLETED, FAILED}`
  - `errorMessage: String?`

## Relationships
- `PrivacyPreference` is a single-row store (DataStore or EncryptedSharedPreferences) and links to user settings logically.

## Validation Rules
- Imported personas and API provider IDs must be unique; existing entries with the same ID must be updated or merged based on user choice.
- Backup JSON must match the top-level schema (personas, apiProviders, settings). If invalid, import fails with a clear error message.

## State Transitions
- Disclaimer dialog: (UNSEEN -> SHOWN) on first launch; (SHOWN -> ACKNOWLEDGED) when user taps acknowledge; (SHOWN -> DISMISSED) when dismissed without acknowledge (re-show next launch).
