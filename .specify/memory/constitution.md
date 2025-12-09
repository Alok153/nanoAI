<!-- Sync Impact Report
Version change: 1.5.0 → 1.6.0
Modified principles: Refined Kotlin-first clean architecture, Performance/Offline, Security & Privacy, Quality Gates, Documentation & Observability
Added sections: Technology & Standards, Development Workflow & Quality Gates
Removed sections: None
Templates requiring updates: ✅ plan-template.md ✅ spec-template.md ✅ tasks-template.md ✅ agent-file-template.md
Follow-up TODOs: None
-->

# nanoAI Constitution

## Core Principles

### Kotlin-First Clean Architecture
- All product code MUST be Kotlin. UI (Compose) → ViewModel → UseCase → Repository → DataSource; no direct cross-layer calls. Coroutines/Flow only; no threads/Rx/Java unless explicitly exempted.
- ViewModels hold state; composables remain stateless and collect from StateFlow.
- Domain logic belongs in use cases; data access only via repositories/data sources.

### Performance, Offline, and UX Discipline
- Budgets: cold start <1.5s; jank <5% frames; critical interactions show feedback <100ms and content <500ms; heavy work off main thread.
- Offline-first: all user-critical features must operate with cached data; retries/backoff and clearly defined loading/empty/error states.
- Material 3 alignment: use theme tokens and a11y (contrast, touch targets, semantics) for every surface.

### Security & Privacy Stewardship
- Secrets (API keys, tokens) MUST use EncryptedSecretStore; never hardcode. Sensitive data encrypted at rest; network calls enforce TLS with modern ciphers.
- Data classification required: Secrets vs Sensitive vs Non-Sensitive; store accordingly. No unnecessary retention; prefer on-device processing.

### Quality Gates and Testing
- Coverage floors: ≥75% ViewModel/Domain, ≥70% Data/Repositories, ≥65% UI. Macro/perf tests protect budgets; offline and accessibility scenarios are mandatory in critical flows.
- CI gates: spotlessCheck, detekt, testDebugUnitTest, verifyCoverageThresholds MUST pass before merge; no ignored blockers.
- TDD encouraged: write/verify failing tests before implementation for new logic.

### Documentation, Observability, and Change Safety
- KDoc for public APIs and non-obvious internals; keep specs/plan/tasks in sync with changes. Update README/docs when behavior or contracts change.
- Structured logging and telemetry for critical operations; capture errors with context while respecting privacy.
- Breaking changes require documented migration or fallback.

## Technology & Standards
- Stack: Kotlin 2.2.x, Jetpack Compose Material 3, Hilt DI, WorkManager for long-running/deferrable work, Room for storage, DataStore for prefs, Retrofit + Kotlin Serialization + OkHttp for networking, Coroutines/Flow for async, Coil for media, MediaPipe Generative (LiteRT) for ML.
- State management: StateFlow in ViewModels; collectAsState in UI; avoid MutableState in composables for source of truth.
- Dependency rules: single DI framework (Hilt); keep modules scoped; repositories expose suspend/Flow APIs only.
- Accessibility: semantics labels, focus order, large touch targets, and color contrast must be defined for new UI.

## Development Workflow & Quality Gates
- Always route feature work: Composable → ViewModel → UseCase → Repository → DataSource; no shortcuts. Create use cases for business logic.
- Tests: unit tests for ViewModels/use cases/repos; UI tests for critical flows, including offline/a11y; contract/integration tests for network/storage boundaries.
- Performance/offline: verify cold start and jank budgets; ensure offline fallbacks and cache consistency.
- Secrets/config: store outside code; never commit keys. Use environment or encrypted stores.
- Reviews: PRs must confirm constitution compliance, coverage adherence, and documentation updates. No merges on red CI.
- Required commands before merge: `./gradlew spotlessCheck detekt testDebugUnitTest verifyCoverageThresholds`.

## Governance
- Constitution supersedes other practices; exceptions require documented rationale and version bump when persistent.
- Amendments: propose change, update this file with version bump and dates, run impact checks on templates and agents, and communicate in PR description.
- Compliance: reviewers must block non-compliant changes; CI should enforce gates.

**Version**: 1.6.0 | **Ratified**: 2025-10-02 | **Last Amended**: 2025-12-10
