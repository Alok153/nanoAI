# UI/UX Issues and Improvements

## Critical Issues (User-Reported & Blocking)

### 1. Welcome Screen on First Launch
- **Status**: ✅ Resolved (2025-10-07).
- **Summary**: Welcome/onboarding overlay and view model removed. `NavigationScaffold` now renders the shell immediately and launches land on Home Hub every time.
- **Follow-up**: If a future first-run experience is desired, design a lightweight in-context surface (card/banner) that does not block Home.

### 2. Back Navigation Closes App Instead of Going Home
- **Issue**: Pressing back from Chat UI closes the app instead of navigating back to Home screen.
- **Location**: `MainActivity.kt` has no `onBackPressed` handling.
- **Expected Behavior**: Per overview.md, back should navigate to previous screen or Home.
- **Fix**: Add `OnBackPressedCallback` in `MainActivity` to:
  - If right drawer open, close it.
  - If left drawer open, close it.
  - If active mode != HOME, switch to HOME.
  - Else, allow default (close app).

### 3. Right Sidebar Squeezes Main UI
- **Issue**: When opening the right sidebar, the entire app UI/Chat UI gets squeezed horizontally.
- **Location**: `NanoShellScaffold.kt` in `ShellRightRailHost`, uses `Row` with main `weight(1f)` and sidebar `320.dp`.
- **Root Cause**: On small screens, the modal right sidebar pushes the main content, reducing its width.
- **Fix**: Change right sidebar to overlay instead of pushing. Use `Box` with absolute positioning for modal right drawer, similar to left `ModalNavigationDrawer`.

### 4. Left Sidebar Toggle Button Broken
- **Issue**: The button to open the left sidebar may not be working properly.
- **Location**: `ShellTopAppBar.kt` has `navigationIcon` with `onClick = onToggleLeftDrawer`.
- **Investigation Needed**: Verify if the toggle logic in `ShellViewModel.toggleLeftDrawer()` and `ShellStateRepository.toggleLeftDrawer()` works correctly. Check if `useModalNavigation` logic affects it.

## Major Inconsistencies with Overview.md

### 5. Navigation Architecture
- **Current**: Left sidebar is modal on small screens, permanent on large. Right sidebar is permanent on large, modal (pushing) on small.
- **Overview**: Left sidebar persistent or collapsible. Right sidebar dynamic controls.
- **Issue**: Right sidebar should not squeeze on small screens; should be overlay/modal like left.

### 6. Home-First Architecture
- **Current**: App opens directly to HOME mode on every launch.
- **Status**: ✅ Aligned after removing the welcome overlay.

### 7. Chat Screen Layout
- **Current**: Top bar with thread title and persona selector, messages list, input area.
- **Overview**: Header with model label and menu; main threaded chat; bottom composer; optional right drawer.
- **Issue**: Missing model label in header (persona is different), no right drawer for Chat mode, no menu in header.

### 8. Right Sidebar Contextuality
- **Current**: Right sidebar has fixed panels: Progress Center, Model Selector, Settings Shortcuts.
- **Overview**: Dynamic controls related to the active mode (model selector, input options, etc.).
- **Issue**: Not contextual to mode; Model Selector panel switches modes, not selects models for current mode.

### 9. Model Selector vs Persona Confusion
- **Current**: Chat has persona selector in top bar (different AI personalities/models).
- **Overview**: Model selector in right sidebar for Chat.
- **Issue**: Confusion between persona (chat-specific) and model (global).

### 10. Settings Architecture
- **Current**: Settings screen with API providers, privacy, UI/UX.
- **Overview**: Structured tabs: General, Appearance, Privacy & Security, etc.
- **Issue**: Check if it matches the exact sections.

### 11. Reusable Components
- **Current**: Some components like MessageBubble, but not shared.
- **Overview**: Compact system of primitives and composed widgets (ComposerBar, ChatBubble, etc.).
- **Issue**: Components not reusable across modes; e.g., no shared ComposerBar.

### 12. User Flow Principles
- **Current**: Create → Edit → Review not fully implemented per mode.
- **Overview**: Universal flows for all modes.
- **Issue**: Flows not consistent across implemented modes.

## Missing Features

### 13. Mode Implementations
- **Current**: Only Chat, Library, Settings implemented; others are placeholders.
- **Overview**: All modes (Image, Audio, Code, Translate, History, Tools) should have full layouts.
- **Issue**: Many core features not implemented.

### 14. History Navigation
- **Current**: Recent activity list in Home with "View history" button (placeholder).
- **Overview**: Recent activity list at bottom.
- **Issue**: History navigation not implemented.

### 15. Contextual Right Panels
- **Current**: Fixed panels.
- **Overview**: Panels change based on active mode (e.g., chat settings for Chat mode).
- **Issue**: No mode-specific controls.

## Improvements and Polish

### 16. Back Navigation Stack
- **Improvement**: Implement proper navigation stack. Currently, modes are flat; no history of navigation.
- **Suggestion**: Track navigation history to allow proper back navigation between modes.

### 17. Drawer Behavior Consistency
- **Improvement**: Make both left and right drawers behave consistently (overlay, not push).
- **Current**: Left overlays, right pushes on small screens.

### 18. Onboarding/Welcome Integration
- **Status**: Deferred. Full-screen onboarding was removed; any new first-run UX should avoid modal takeovers and live within Home.

### 19. Tone & Microcopy
- **Current**: Basic text, no specific microcopy.
- **Overview**: Short verbs, concise tooltips, helpful inline notes.
- **Issue**: Improve copy to match human, modular, graceful, fast ethos.

### 20. Design Ethos Alignment
- **Current**: Basic implementation.
- **Overview**: Human, modular, graceful, fast.
- **Issue**: Ensure all interactions are smooth, components reusable, degraded states usable.

### 21. Accessibility
- **Check**: Ensure all interactive elements have proper semantics, keyboard navigation.
- **Current**: Some elements have `contentDescription`, but verify completeness.

### 22. Performance & Animations
- **Check**: Animations and transitions should be smooth.
- **Current**: Uses `AnimatedVisibility` with tweens, should be fine.

### 23. Connectivity & Offline Handling
- **Current**: Banner for offline, but check if all features degrade gracefully.
- **Improvement**: Ensure full offline support and graceful degradation.

### 24. Error Handling
- **Current**: Snackbars for errors, but check inline remedies.
- **Improvement**: Add contextual error recovery options.

## Implementation Plan

### Phase 1: Critical Fixes (small, high-confidence changes)
1. **Fix Welcome Screen** — ✅ Completed. Welcome overlay, view model, and tests removed so launches land on Home by default (2025-10-07).
2. **Implement Back Navigation**: Add back handling in `MainActivity`.
  - Acceptance criteria: Back press closes right drawer if open, else closes left drawer if open, else navigates to Home if active mode != HOME, else performs default finish. File refs: `app/src/main/java/com/vjaykrsna/nanoai/MainActivity.kt`, `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/presentation/ShellViewModel.kt`.
3. **Fix Right Sidebar Layout**: Make the right contextual drawer overlay (not push) on small screens.
  - Acceptance criteria: On small window sizes `ShellRightRailHost` should not shrink main content; modal right panel should draw over main surface using `Box` + offset/absolute placement. File ref: `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/shell/NanoShellScaffold.kt`.
4. **Verify Left Drawer Toggle**: Confirm `ToggleLeftDrawer` flows from TopAppBar -> ShellViewModel -> ShellStateRepository.
  - Acceptance criteria: Clicking top-bar nav icon toggles the left drawer on both modal and permanent variants. File refs: `NanoShellScaffold.kt`, `ShellViewModel.kt`, and the repository under `feature/uiux/data`.

### Phase 2: Core Alignment
5. **Align Chat with Overview**: Add model label, right drawer.
6. **Make Right Sidebar Contextual**: Change based on active mode.
7. **Implement History Navigation**: Add history mode and navigation.
8. **Create Reusable Components**: ComposerBar, ChatBubble, etc.

### Phase 3: Feature Completion
9. **Implement Missing Modes**: Start with Image, Audio, Code, Translate, Tools.
10. **Improve Settings Structure**: Match overview tabs.
11. **Enhance User Flows**: Ensure consistent Create → Edit → Review across modes.

### Phase 4: Polish
12. **Improve Microcopy**: Update text to match ethos.
13. **Accessibility Audit**: Verify all elements are accessible.
14. **Performance Testing**: Ensure smooth animations and transitions.
15. **Cross-Device Testing**: Run on different screen sizes to verify layouts.
