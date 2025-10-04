# TODO Next: Fixes and Inconsistencies Stabilization

_Last updated: 2025-10-04_

## Near-Term Priorities
- **MediaPipe LiteRT integration**: Replace the mocked synthesis path in `MediaPipeLocalModelRuntime` with real on-device inference and add latency benchmarks (< 2s, Scenario 6 follow-up).
- **Checksum & URL Hardening**: Extend `ModelDownloadWorker` to fetch per-package download URLs and verify cryptographic signatures in addition to SHA-256 hashes.
- **Secrets Hardening UX**: Surface post-migration confirmations in Settings with guidance for rotating provider credentials, and cover with instrumentation tests.
- **Accessibility Sweep**: Audit remaining Compose surfaces (chat transcript, settings lists) for TalkBack ordering and semantics gaps identified under `docs/inconsistencies.md`.

## Medium-Term Enhancements
- **Error Envelope Unification**: Align all telemetry publishers with the new `TelemetryReporter` contract and add dashboards for `RecoverableError` trends.
- **Offline Sync Roadmap**: Flesh out conflict resolution for queued actions, including retry jitter and WorkManager constraints.
- **Deep Link Coverage**: Expand `NavigationScaffold` to support deep links into persona and history routes and add regression tests.

## Artifacts & Ownership
- Owner: UI/UX Platform Working Group
- Weekly checkpoint: Review outstanding inconsistencies and close resolved items in `docs/inconsistencies.md`.
- Reporting: Summaries posted in `docs/changelog.md` (to be created) once major milestones are delivered.
