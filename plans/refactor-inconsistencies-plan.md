## Plan: Error Handling & Reactive Contracts Refresh

Refactor remaining viewmodels and Compose surfaces to the unified envelope-driven action pipeline while annotating all domain APIs with the new reactive contracts, ensuring consistency and test coverage before running regression gates.

**Phases 5**
1. **Phase 1: Settings Error & UI Pipeline Alignment**
    - **Objective:** Migrate `SettingsViewModel` and associated UI to emit `NanoAIErrorEnvelope` events via a single action/event pipeline modeled after the Image feature.
    - **Files/Functions to Modify/Create:** `feature/settings/presentation/SettingsViewModel`, `feature/settings/presentation/*Actions`, `feature/settings/ui/SettingsScreen`, `feature/settings/ui/SettingsScreenState`, related Compose collectors, `SettingsViewModelTest`, `SettingsScreenTest`.
    - **Tests to Write:** `SettingsViewModel emits envelope events`, `SettingsScreen shows snackbar from envelope`, updated snapshot/UI tests covering dialog + snackbar coordination.
    - **Steps:**
        1. Write failing unit test asserting `SettingsViewModel` emits `UiEvent.ErrorRaised(NanoAIErrorEnvelope)` and mirrors `userMessage` into state.
        2. Implement envelope mapping helpers, refactor delegates to return `NanoAIResult`, update state host to emit new events, ensure old bespoke `SettingsError` removed.
        3. Update Compose UI to consume the single event flow via `SettingsScreenActions`, simplifying dialog/snackbar wiring; adjust UI tests to new collectors.

2. **Phase 2: Chat History & Screen Event Refactor**
    - **Objective:** Align `HistoryViewModel` error handling and `ChatScreen` action/event coordination with the new pipeline, ensuring telemetry-ready envelopes.
    - **Files/Functions to Modify/Create:** `feature/chat/presentation/HistoryViewModel`, `feature/chat/ui/ChatScreen`, supporting `ChatScreenActions` struct, snackbar collection helpers, `HistoryViewModelTest`, `ChatScreenTest`.
    - **Tests to Write:** `HistoryViewModel maps failures to envelopes`, `ChatScreen displays envelope snackbar via single collector`, regression test for action dispatch flow.
    - **Steps:**
        1. Add/modify failing tests verifying `HistoryViewModel` emits envelope events and no longer mutates state with raw strings.
        2. Refactor `HistoryViewModel` and associated use-case interactions to use `NanoAIResult` → envelope mapping, updating state host and event channel.
        3. Introduce unified `ChatScreenActions` + collector mirroring Image feature, replacing multiple `LaunchedEffect`s and dialog coordinators; update Compose tests accordingly.

3. **Phase 3: Coverage Dashboard Error Normalization**
    - **Objective:** Ensure `CoverageDashboardViewModel` and its data sources adopt `NanoAIErrorEnvelope` and the shared event flow.
    - **Files/Functions to Modify/Create:** `core/coverage/presentation/CoverageDashboardViewModel`, `core/coverage/domain/GetCoverageReportUseCase`, repository/data sources if needed, `CoverageDashboardViewModelTest` (new/updated), related UI collectors.
    - **Tests to Write:** `CoverageDashboardViewModel emits envelope when use case fails`, `state reflects envelope userMessage`, repository/use-case tests if behavior changes.
    - **Steps:**
        1. Create failing unit test for the viewmodel covering envelope emission on repository failure.
        2. Update use case/repository to return `NanoAIResult` (or equivalent) and map errors to envelopes inside the viewmodel's event channel.
        3. Wire UI collectors (if any) to consume the standardized events, ensuring telemetry metadata preserved.

4. **Phase 4: Reactive Contract Annotation Sweep**
    - **Objective:** Annotate all public repositories/use cases with `@ReactiveStream` or `@OneShot`, fixing any hybrid return types discovered.
    - **Files/Functions to Modify/Create:** Settings, Downloads, Library, Audio, Coverage repositories/use cases referenced in findings, `docs/development/REACTIVE_DATA_CONTRACT.md` if clarifications needed, relevant DI modules.
    - **Tests to Write:** Update/extend unit tests ensuring contract behavior (e.g., Flow-based use cases remain cold streams) and add regression tests if refactors change signatures.
    - **Steps:**
        1. Write/adjust tests around representative use cases to confirm behavior (e.g., verifying flows remain reactive, suspend functions return once) before annotation.
        2. Add the appropriate annotations, refactor any `Flow<NanoAIResult>` hybrids into either pure Flow or `NanoAIResult` suspenders, updating call sites as needed.
        3. Run detekt/spotless for annotation imports and ensure documentation references the new coverage.

5. **Phase 5: Full Regression Validation**
    - **Objective:** Execute lint/static analysis and comprehensive tests, addressing any regressions before finalizing.
    - **Files/Functions to Modify/Create:** N/A (commands + potential minor fixes triggered by tooling), possibly CI config if failures arise.
    - **Tests to Write:** None new; run `./gradlew spotlessCheck detekt testDebugUnitTest verifyCoverageThresholds` and add targeted fixes/tests if failures occur.
    - **Steps:**
        1. Execute the required Gradle tasks; capture failures.
        2. Address any regressions uncovered (additional tests, code tweaks) by iterating locally.
        3. Re-run tooling until all pass and document results for the completion report.

**Open Questions 3** ( ** THESE QUESTIONS HAS BEEN ANSWERED MAKE SURE TO ADDRESS THESE AFTER YOUR TASK IS COMPLETE ** )
1. Should `GetCoverageReportUseCase` and its repository return `NanoAIResult` to simplify envelope mapping, or should the viewmodel continue wrapping thrown exceptions locally?
Answer - Yes

2. How should fire-and-forget helpers like `ToggleCompactModeUseCase.toggle` be annotated under the reactive contract policy—convert to suspend `@OneShot` or document a third contract type?
Answer - Convert to suspend `@OneShot`

3. For APIs returning `Result<T>` (e.g., backup import/export), do we normalize them to `NanoAIResult` before emitting envelopes, or can we adapt existing results via local mapping utilities?
Answer - Normalize to NanoAIResult
