## nanoAI UI/UX Codebase Audit

Date: 2025-10-07

Scope: automated/manual scan of the `app/` module focusing on shell/navigation, drawers, first-run experience, and top-level UX components. Files inspected (non-exhaustive):

- `app/src/main/java/com/vjaykrsna/nanoai/ui/navigation/NavigationScaffold.kt`
- `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/shell/NanoShellScaffold.kt`
- `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/presentation/ShellViewModel.kt`
- `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/presentation/WelcomeViewModel.kt` (removed in latest iteration)
- `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/presentation/AppViewModel.kt`
- `app/src/main/java/com/vjaykrsna/nanoai/MainActivity.kt`

Summary of findings
-------------------

Critical (should fix before next release)

1) Welcome overlay blocked Home Hub (✅ Resolved 2025-10-07)
  - Fix: Removed welcome overlay UI, view model, analytics, and tests. `NavigationScaffold` now renders `NanoShellScaffold` immediately and home is first content on every cold start.
  - Follow-up: If a reimagined onboarding is needed, prefer contextual surfaces in Home rather than modal overlays.

2) Back navigation behavior is missing in `MainActivity`
  - Evidence: No `OnBackPressedCallback` found in `MainActivity.kt` (searched repository). The shell supports switching modes via `ShellViewModel.openMode`, but back press handling to close drawers or navigate home isn't wired.
  - Why critical: Users expect Android back button behavior to navigate rather than exit unexpectedly.
  - Suggested fixes:
    - Add OnBackPressedCallback in `MainActivity` that queries ShellViewModel or Shell repository state and issues `ShellUiEvent.ToggleRightDrawer`/`ToggleLeftDrawer` or `openMode(ModeId.HOME)` as appropriate.
  - Acceptance criteria: Back press closes right drawer -> else closes left drawer -> else navigates back to HOME if not there -> else default finish.

Major (high priority but not necessarily blocking)

3) Right sidebar pushes/shrinks main content on small screens
  - Evidence: `ShellRightRailHost` composes a `Row` with `ShellMainSurface` weight(1f) and a 320.dp `Surface` for the rail. When the rail is visible as modal on small devices, it still participates in the Row layout and reduces main content width.
  - Why it matters: UX spec expects right panel to overlay so the main surface isn't squeezed. Chat UI and other content can become cramped.
  - Suggested fixes:
    - Replace the Row-based host for floating/modal right panel with a Box layering approach: draw the main surface full-width, then draw the right panel on top with absolute positioning (align end). Keep permanent rail behavior for large screens.
    - Use AnimatedVisibility + Box.offset or a Compose `Dialog`/`Modal` approach if appropriate.
  - Acceptance criteria: On small window sizes, opening the right panel overlays (doesn't impact) the width of main content. Permanent rail remains for large width classes.

4) Right panel is not contextual enough
  - Evidence: `RightSidebarPanels` is used but panels include fixed `PROGRESS_CENTER`, `MODEL_SELECTOR` etc. TopAppBar actions call `onToggleRightDrawer(RightPanel.MODEL_SELECTOR)` which may switch modes unexpectedly.
  - Suggestion: ensure model selector is contextual (e.g., when on Chat, open model selection for Chat, not navigate to a different app section). Disambiguate persona vs model selector UI and their locations.
  - Acceptance criteria: Model selection affects current mode's model; persona chooser remains chat-specific.

Minor / Cosmetic / Clarifications

5) Left drawer toggle flow looks correct but should be validated
  - Evidence: `ShellTopAppBar` shows a nav icon that calls `onToggleLeftDrawer` leading to `ShellViewModel.toggleLeftDrawer()` which flips repository state. Also uses drawerState listener in `NanoShellScaffold` to emit ToggleLeftDrawer when drawer open/close originates from UI. This is OK but edge case: if useModalNavigation==false the drawerState is forcibly closed in LaunchedEffect; validate state synchronization.
  - Acceptance criteria: Clicking the top-bar nav icon toggles drawer on modal and permanent variants without producing state inversion loops.

6) Command palette keyboard handling appears present
  - Evidence: `handleShellShortcuts` in `NanoShellScaffold` handles Ctrl+K and Escape. Tests in `androidTest` reference the scaffold. Keep coverage.

Files and code pointers (exact locations)

- Home-first launch state: `app/src/main/java/com/vjaykrsna/nanoai/ui/navigation/NavigationScaffold.kt` (direct shell render)
- App state: `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/presentation/AppViewModel.kt` (no onboarding flag; exposes theme/offline/hydration only)
- Shell scaffold and drawers: `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/shell/NanoShellScaffold.kt` (see `ShellRightRailHost`, `ShellMainSurface`, `ShellTopAppBar`)
- Shell view model: `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/presentation/ShellViewModel.kt`
- MainActivity: `app/src/main/java/com/vjaykrsna/nanoai/MainActivity.kt`

Suggested low-risk patches to include in this repo (examples)

1) Make right panel overlay for modal case
  - Change `ShellRightRailHost` to use a Box: render `ShellMainSurface` full-width then conditionally show the modal right panel in an aligned Box scope with width 320.dp. Keep `PermanentNavigationDrawer` case unchanged.

2) Add back-handler in `MainActivity`
  - Add an OnBackPressedCallback that reads from `ShellViewModel.uiState` (or calls small `shellViewModel.handleBack()` helper) and maps to the rules: close right drawer -> close left drawer -> navigate home -> default.

Next steps and quick plan
-------------------------

1. Implement the two low-risk changes (right panel overlay + back handler) to align navigation with overview expectations. (Estimated: 1-2 dev days)
2. Run unit/compose tests (there are existing `androidTest` Compose tests in `feature/uiux`) and add a small unit test for back handler or a smoke Compose test for right rail overlay. (Estimated: 0.5-1 day)
3. Triage remaining items into tracked issues with acceptance criteria, owners, and priority.

Requirements coverage matrix
---------------------------

- Overview: "Home-first" -> Status: ✅ Done. App opens directly to Home; onboarding overlay removed.
- Overview: "Right sidebar overlays, not squeeze" -> Currently: squeezes on small screens. Status: Not done (see fix 3).
- Overview: "Back navigation should go home" -> Currently: missing. Status: Not done (see fix 2).

If you'd like, I can prepare the two safe patches now (right-panel overlay change + MainActivity back handling) and run a quick build to verify compile errors. Tell me which of the two to prioritize first. 
