# Research: Fixes and Inconsistencies Stabilization Pass

**Feature**: 004-fixes-and-inconsistencies  
**Date**: 2025-10-03  
**Research Focus**: Stabilize the nanoAI Android app by eliminating Detekt blockers, securing secrets, validating model downloads, and hardening tests while preserving performance and UX guarantees.

## Findings

### Encrypted Secrets Migration (Jetpack Security)
- **Decision**: Migrate provider credentials and API keys from plaintext SharedPreferences/DataStore wrappers into a dedicated `EncryptedSharedPreferences` instance backed by `MasterKey` (AES256_GCM) and AES256_SIV/AES256_GCM key/value schemes. Wrap access behind a repository to permit rotation and testing.
- **Rationale**: AndroidX Security Crypto provides battle-tested primitives with automatic key management and backwards-compatible APIs. Using repository abstraction supports unit tests via in-memory fake. Migration can run once during app start, copying existing plaintext entries before wiping the old store.
- **Alternatives Considered**: Custom Tink wrapper (more manual plumbing, harder to audit); SQLCipher/Room column encryption (heavier migration, overkill for config secrets).
- **References**: AndroidX Security Crypto API docs — `EncryptedSharedPreferences.create(...)` (`/androidx/androidx` snippets `_snippet_6`, `_snippet_3`).

### Model Download Integrity & WorkManager Policy
- **Decision**: Extend `ModelDownloadWorker` to read a signed manifest (JSON) that includes `sha256` and `sizeBytes`, verify it before installing, and use `Result.retry()` with WorkManager backoff (exponential, max 3). Persist manifest metadata in Room for auditing.
- **Rationale**: Manifest-driven validation prevents tampered or partial downloads, aligning with AI Inference Integrity. WorkManager provides resilient background execution with automatic constraints handling.
- **Alternatives Considered**: Inline checksum per download request (duplicates logic); relying solely on HTTP ETag (insufficient for corruption).
- **References**: Android Developers WorkManager docs ("Schedule tasks with WorkManager" – https://developer.android.com/topic/libraries/architecture/workmanager) and MediaPipe Task package integrity recommendations.

### Standardized Error Envelope
- **Decision**: Introduce a sealed `NanoAIResult` hierarchy representing `Success<T>`, `RecoverableError`, and `FatalError`, each carrying telemetry metadata. Domain use cases return this type, and UI layers map to Compose state.
- **Rationale**: Eliminates unchecked exceptions, aligns with FR-007, and simplifies error handling consistency across offline/online runtimes.
- **Alternatives Considered**: Kotlin `Result<T>` (lacks typed error metadata), throwing exceptions (current inconsistent approach).
- **References**: Kotlin language guidance on sealed hierarchies; internal architecture spec 001-foundation.

### Compose Decomposition & Accessibility
- **Decision**: Split oversized composables (`NavigationScaffold`, `HomeScreen`, `SidebarContent`, `ThemeToggle`) into focused composables with state hoisting, slot APIs, and preview coverage. The legacy welcome screen was retired entirely; future first-run affordances should live within Home.
- **Rationale**: Reduces complexity to satisfy Detekt rules, improves testability, and maintains UX polish/performance budgets.
- **Alternatives Considered**: Suppressing Detekt (violates spec), rewriting screens in XML (breaks Compose consistency).
- **References**: Jetpack Compose Material 3 guidance (https://developer.android.com/develop/ui/compose/material3) and Accessibility docs.

### Deterministic Offline & Fallback Testing
- **Decision**: Use Robolectric + Turbine for ViewModel/domain tests, and Compose UI + Espresso for instrumentation flows with fake network/local runtimes. Introduce `TestDownloadManifestServer` (MockWebServer) and `FakeEncryptedPrefs` for deterministic assertions.
- **Rationale**: Ensures offline persona, disclaimer dialog, model library, and cloud fallback flows run in CI without flakiness.
- **Alternatives Considered**: Manual QA only (fails automation gates), instrumentation-only tests (too slow for CI coverage).
- **References**: Android Testing documentation (https://developer.android.com/training/testing) for Compose + WorkManager patterns.

## Research Tasks (Completed)
- Reviewed AndroidX Security Crypto APIs for encrypted preferences usage and migration steps.
- Validated WorkManager retry + checksum patterns for long-running downloads and MediaPipe manifest integrity requirements.
- Evaluated sealed result pattern suitability for clean architecture layers.
- Mapped Compose decomposition practices to targeted oversize composables while preserving accessibility semantics.
- Confirmed deterministic test strategies for offline/cloud fallback flows using existing tooling.

## Resolved Unknowns
- Migration path for secrets, manifest verification design, error handling strategy, Compose decomposition, and deterministic tests are defined. No outstanding `NEEDS CLARIFICATION` items.

## Output / Next Steps
- Proceed to Phase 1 design: produce `data-model.md`, populate `contracts/`, generate `quickstart.md`, and update `.github/copilot-instructions.md` using the gathered decisions and references.
