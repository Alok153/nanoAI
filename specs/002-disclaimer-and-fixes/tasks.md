# Tasks: First-launch Disclaimer and Fixes (Phase 2)

Feature: First-launch Disclaimer and Fixes
Branch: 002-disclaimer-and-fixes
Spec: /home/vijay/Personal/myGithub/nanoAI/specs/002-disclaimer-and-fixes/spec.md
Plan: /home/vijay/Personal/myGithub/nanoAI/specs/002-disclaimer-and-fixes/plan.md

Guiding principles: TDD-first where practical, fix test harness and linting before feature work, preserve constitutional checks (Kotlin-first, Material UX, privacy by design).

Legend:
- [P] = parallelizable task (independent files)
- TID = Task ID
- Dependencies: list of TIDs that must be completed before this task

---

T001 - Setup: Local verification and CI sandbox
- Description: Ensure developer environment can run lint & unit tests locally and reproduce current failures. Capture exact failing output for targeted fixes.
- Actions:
  1. Run the project's static analysis and unit test commands (as defined in the feature plan) and capture outputs to `specs/002-disclaimer-and-fixes/logs/initial-ci-output.log`.
  2. Create `specs/002-disclaimer-and-fixes/logs/` directory if missing and save outputs there. Do not commit large/generated logs to source; attach them to the PR or upload as CI artifacts when needed.
- Files/paths: repo root commands, `specs/002-disclaimer-and-fixes/logs/`
- Dependencies: none
- Why: Provides baseline test/lint output to triage failures.

T002 - Test harness fixes: resolve test compile issues (namespaces/imports)
- Description: Fix unit test compile failures caused by missing imports, wrong test source set assumptions, or test dependencies.
- Actions:
  1. Inspect failing tests under `app/src/test/java/` (e.g., `core/data/db/ChatMessageDaoTest.kt`, `feature/chat/domain/SendPromptAndPersonaUseCaseTest.kt`) and add required test dependencies or update imports.
  2. If tests rely on Android-only classes (e.g., `AndroidJUnit4`, `ApplicationProvider`), move them to `androidTest` or add Robolectric where appropriate.
  3. Add minimal test doubles/mocks for missing repositories where tests assert behavior but implementations are incomplete.
- Files/paths: `app/src/test/java/**`, `app/build.gradle.kts` (test deps)
- Dependencies: T001
- Parallel: no
- Why: Compiling tests unlocks TDD flow.

T003 - Lint cleanup: fix SuspiciousIndentation & run formatters
- Description: Fix lint errors found by `./gradlew lintDebug` (suspicious indentation in `ConversationRepositoryImpl.kt`) and run formatters.
- Actions:
  1. Edit `app/src/main/java/com/vjaykrsna/nanoai/core/data/repository/impl/ConversationRepositoryImpl.kt` to fix indentation and any lint-reported issues.
  2. Run `./gradlew ktlintFormat detekt` and ensure no new blocking lint errors appear.
- Files/paths: `app/src/main/java/com/vjaykrsna/nanoai/core/data/repository/impl/ConversationRepositoryImpl.kt`
- Dependencies: T001
- Parallel: no
- Why: Lint is a CI blocker per constitution.

T004 - Data model migration: add fields to `PrivacyPreference`
- Description: Add `consentAcknowledgedAt: Instant?` and `disclaimerShownCount: Int` to the `PrivacyPreference` data model and persistence store.
- Actions:
  1. Update `app/src/main/java/com/vjaykrsna/nanoai/core/data/preferences/PrivacyPreference.kt` to include the new fields with defaults.
  2. Update `PrivacyPreferenceStore` to persist and expose these fields; add helper methods `acknowledgeConsent(timestamp)` and `incrementDisclaimerShown()`.
  3. Add unit tests for the DataStore behavior (initial default, acknowledge flow).
- Files/paths: `app/src/main/java/com/vjaykrsna/nanoai/core/data/preferences/PrivacyPreference.kt`, `.../PrivacyPreferenceStore.kt`, `app/src/test/java/.../` tests
- Dependencies: T003
- Parallel: [P]
- Why: Required by FR-001 and quickstart verification.

T005 - Implement first-launch disclaimer UI (non-blocking)
- Description: Add Compose dialog shown on first launch per spec and wire to `PrivacyPreferenceStore`.
- Actions:
  1. Create a composable `FirstLaunchDisclaimerDialog` under `feature/settings/ui` or `core/ui` with `Acknowledge` and `Dismiss` actions, content per spec text.
  2. On app startup (`MainActivity` or `NanoAIApplication`/root ViewModel), check `privacyPreferenceStore.consentAcknowledgedAt` and `disclaimerShownCount`; show dialog if not acknowledged. On `Acknowledge`, store timestamp and increment shown count; on `Dismiss`, increment shown count.
  3. Add accessibility semantics (content descriptions and roles) for dialog controls.
  4. Add a UI test that asserts the dialog appears when the stored preference shows no acknowledgment and that acknowledging records the consent state.
- Files/paths: `app/src/main/java/com/vjaykrsna/nanoai/feature/settings/ui/FirstLaunchDisclaimer.kt`, `app/src/main/java/com/vjaykrsna/nanoai/NanoAIApplication.kt` or `MainActivity.kt` wiring, tests under `app/src/androidTest/` or `app/src/test/`.
- Dependencies: T004
- Parallel: no
- Why: Directly implements FR-001.

T006 - Settings import handler and validation
- Description: Implement import endpoint handler in app settings (local import from file) using the `BackupBundle` schema.
- Actions:
  1. Add `ImportService` in `feature/settings/data/impl` that reads a JSON file, validates schema, and applies personas and API providers via corresponding repositories.
  2. Add file picker integration in `SettingsScreen` to select a JSON file and call `ImportService.importBackup(uri)` on confirm.
  3. Add error handling and user-facing messages (success/failure) and tests for invalid JSON.
- Files/paths: `app/src/main/java/com/vjaykrsna/nanoai/feature/settings/data/ImportService.kt`, UI hooks in `app/src/main/java/.../SettingsScreen.kt` and `SettingsViewModel.kt`.
- Dependencies: T003, T004
- Parallel: no
- Why: Implements FR-002 import path from spec.

T007 - Export: use existing export flow and show warnings
- Description: Ensure `ExportDialog` warns about unencrypted backup and uses JSON format; connect to existing export use case in `ModelDownloadsAndExportUseCase` or similar.
- Actions:
  1. Verify `SettingsScreen` ExportDialog uses Downloads directory safely and shows warning; add `exportWarningsDismissed` toggle to not show again.
  2. Implement unit test ensuring the generated file contains personas and apiProviders JSON keys.
- Files/paths: `app/src/main/java/.../feature/settings/ui/SettingsScreen.kt`, `feature/library/data/export/*`
- Dependencies: T006
- Parallel: [P]
- Why: Completes FR-002 export side.

T008 - Sidebar local/cloud toggle wiring
- Description: Add the Local/Cloud toggle to the sidebar and wire it to `InferenceOrchestrator` so user choice affects inference preference.
- Actions:
  1. Add toggle UI to `ui/navigation/NavigationScaffold.kt` or `feature/sidebar/presentation/SidebarViewModel.kt` and persist choice in DataStore.
  2. Update `InferenceOrchestrator` to read the user preference when `generateResponse` is invoked.
  3. Add unit tests for toggling behavior.
- Files/paths: `app/src/main/java/com/vjaykrsna/nanoai/ui/navigation/NavigationScaffold.kt`, `.../SidebarViewModel.kt`, `InferenceOrchestrator.kt`.
- Dependencies: T004, T003
- Parallel: no
- Why: Implements FR-003.

T009 - Contract tests for import/export/disclaimer
- Description: Create failing contract tests based on `contracts/import-export-openapi.yaml` to codify expected behavior.
- Actions:
  1. Add tests under `app/src/test/contract/` that assert schema and basic field presence for import/export endpoints.
  2. Tests should be written to fail (TDD) until implementation is complete.
- Files/paths: `app/src/test/contract/ImportExportContractTest.kt`
- Dependencies: T002
- Parallel: [P]
- Why: Capture expected API behavior and drive implementation.

T010 - Add sample backup and QA scripts
- Description: Add sample backup JSON and a QA script to run quickstart checks automatically.
- Actions:
  1. Ensure `specs/002-disclaimer-and-fixes/contracts/sample-backup.json` is present (already added).
  2. Add a small shell script `specs/002-disclaimer-and-fixes/qa/run-quickstart.sh` that automates steps 2-4 from `quickstart.md` using adb commands where possible and prints results.
- Files/paths: `specs/002-disclaimer-and-fixes/qa/run-quickstart.sh`
- Dependencies: T005, T006
- Parallel: [P]
- Why: Speeds QA validation.

T011 - CI integration: ensure tasks run in pipeline
- Description: Add or update CI workflow to include `ktlint`, `detekt`, `lintDebug`, and `testDebugUnitTest` for this branch.
- Actions:
  1. Add a GitHub Actions job or update existing one to run before merge for branch `002-disclaimer-and-fixes`.
  2. Ensure the job publishes test output to artifacts for review.
- Files/paths: `.github/workflows/ci.yml` (or relevant workflow file)
- Dependencies: T001, T003, T009
- Parallel: no
- Why: Enforces constitution automated quality gates.

T012 - Polish & docs
- Description: Write docs, update spec to reference implemented files, and ensure quickstart is accurate.
- Actions:
  1. Update `specs/002-disclaimer-and-fixes/spec.md` with implemented changes and link to new files.
  2. Update README or developer notes for how to test disclaimer behavior.
- Files/paths: `specs/002-disclaimer-and-fixes/spec.md`, `README.md`
- Dependencies: T005, T006, T007
- Parallel: [P]
- Why: Completes feature delivery.

T013 - Accessibility verification
- Description: Validate accessibility semantics and ensure UI elements expose necessary content descriptions and roles. Require evidence for PR review.
- Actions:
  1. Add Compose semantics tests that assert contentDescription/semantics presence for key UI controls (dialog buttons, sidebar toggles, model download controls, export dialog).
  2. Add a short script or CI step to capture screenshots of critical screens (ChatScreen, SettingsScreen, ModelLibraryScreen) and include them as PR artifacts.
  3. Document any remaining accessibility gaps in the PR and create follow-up tasks for unresolved items.
- Files/paths: `app/src/androidTest/.../AccessibilityChecks.kt`, `specs/002-disclaimer-and-fixes/qa/` screenshots and scripts
- Dependencies: T005, T027
- Parallel: [P]
- Why: Makes FR-005 explicit and verifiable for reviewers and agents.

---

Parallel execution examples (run these concurrently):
- Group A [P]: T004 (data model migration), T009 (contract tests), T010 (QA scripts)
- Group B [P]: T007 (export polish), T012 (docs)

Try it (local):
- Run the setup task first to capture failures:

```bash
./gradlew ktlintFormat detekt lintDebug testDebugUnitTest
```

Deliverables produced:
- `specs/002-disclaimer-and-fixes/tasks.md` (this file)
- Updated plan/data-model/research/quickstart/contracts already present
