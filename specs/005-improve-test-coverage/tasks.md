# Tasks: Restore nanoAI Test Coverage Health

**Input**: Design docs under `specs/005-improve-test-coverage/`
**Audit Date**: 2025-10-12
**Recent Commands**: `./gradlew jacocoUnitReport`

## Current Audit
- `app:testDebugUnitTest` fails for the UI/UX domain suites (`ObserveUserProfileUseCaseTest`, `ToggleCompactModeUseCaseTest`, `UpdateThemePreferenceUseCaseTest`, `UserProfileModelTest`) because the tests expect onboarding and dismissed-tip fields that were intentionally removed from production models.
- `jacocoFullReport` cannot complete without a connected device; no merged coverage artefacts exist under `app/build/coverage/` or `app/build/reports/jacoco/full/`.
- Core UI/UX service classes (`CommandPaletteActionProvider`, `ProgressCenterCoordinator`, `UserProfileRepositoryImpl`) ship without dedicated unit coverage.
- Risk register items RR-CRIT-041, RR-HIGH-027, and RR-HIGH-033 remain unresolved and continue to block coverage gates.

## Phase 1: Stabilise UI/UX Domain Contracts
- [X] T101 Update the failing JUnit5 suites (`ObserveUserProfileUseCaseTest`, `ToggleCompactModeUseCaseTest`, `UpdateThemePreferenceUseCaseTest`, `UserProfileModelTest`) to remove expectations for `onboardingCompleted`, `dismissedTips`, pinned tool ordering, and TalkBack-friendly connectivity metadata, aligning them with the intentionally simplified production models.
- [X] T102 Ensure `UserProfile` (plus `UserProfileEntity` + sanitizers) no longer tracks dismissed tips, onboarding progress, and layout limits, as these were removed on purpose; verify schema migrations handle the removal without data loss if needed.
- [X] T103 Refresh `UserProfileLocalDataSource` and `UserProfileRepositoryImpl` to remove handling of the preference fields, persist onboarding completions (if any remain), and reflect compact-mode side effects; add focused unit tests verifying cache hydration and overlay logic without the removed fields.

## Phase 2: Backfill Domain & Service Coverage
- [X] T104 Create `CommandPaletteActionProviderTest` (JUnit5 + coroutine test utilities) to cover mode availability toggles, recent-activity formatting, and query filtering edge cases.
- [X] T105 Add `ProgressCenterCoordinatorTest` using a fake `DownloadManager` to exercise retry/pause/resume branches and ensure new job observers surface coverage signals.
- [X] T106 Introduce `UserProfileRepositoryImplTest` with an in-memory Room database + fake `UiPreferencesStore` to validate overlay precedence, compact-mode propagation, and command palette recents.

## Phase 3: Restore Coverage Automation
- [X] T107 Configure a Gradle Managed Virtual Device (API 34 minimal footprint) so `connectedDebugAndroidTest` and `jacocoFullReport` run headlessly in CI; document local emulator fallbacks.
- [X] T108 Regenerate merged coverage via `jacocoFullReport`, execute `coverageMarkdownSummary`, and publish refreshed artefacts under `app/build/coverage/` and `app/build/reports/jacoco/full/`.
- [X] T109 Update `docs/coverage/risk-register.md` and `docs/testing.md` with the new coverage snapshot, thresholds, and emulator guidance.

## Phase 4: Close Open Coverage Risks
- [X] T110 Expand `CoverageDashboardTest` to assert the offline banner + TalkBack copy (resolving RR-CRIT-041) using MockWebServer failure scenarios.
- [X] T111 Extend `RiskRegisterCoordinatorTest` to verify mitigation-tag handling and overdue escalation logic (resolving RR-HIGH-027).
- [X] T112 Harden `HuggingFaceAuthCoordinatorTest` around `slow_down` back-off copy and accessible error surfacing (resolving RR-HIGH-033).

## Dependencies
- Complete Phase 1 before adding new coverage in Phases 2–4 so the current failing unit suites stabilise.
- Phase 3 relies on instrumentation availability introduced in T107.
- Risk fixes in Phase 4 consume artefacts regenerated in Phase 3 for stakeholder sign-off.
