# Tasks: UI Layer Architecture & Quality Fixes

**Input**: findings.md audit results and codebase analysis from `/specs/001-foundation/`
**Prerequisites**: `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`
**Scope**: Fix all UI layer violations identified in findings.md audit + related ViewModel layer violations that impact UI architecture
**Scope Expansion**: Added ViewModel-UseCase layer fixes (T022) as they directly affect UI layer clean architecture compliance

## Phase 1: UI Quality Infrastructure Setup
**Setup Goal**: Establish the foundation for UI quality improvements and testing infrastructure.

**Setup Tasks**:
- [X] T001 Extract hardcoded UI strings to `app/src/main/res/values/strings.xml` - Create comprehensive string resources for all user-facing text found in UI components
- [X] T002 Configure accessibility testing infrastructure in `app/build.gradle.kts` - Add accessibility testing dependencies and configuration
- [X] T003 Set up UI testing baselines in `config/` - Create baseline files for accessibility and UI quality checks

## Phase 2: User Story - UI Polish & Quality Assurance
**Story Goal**: As a user, I want a polished, accessible UI that follows Material Design guidelines and maintains clean architecture principles.

**Independent Test Criteria**:
- All UI strings are extracted to resources and support localization
- Touch targets meet 48dp minimum accessibility requirements
- Color contrast ratios meet WCAG AA standards (4.5:1 for normal text, 3:1 for large text)
- ShellViewModel is split into focused, single-responsibility ViewModels
- No hardcoded strings remain in UI composables
- UI layer maintains clean architecture (no repository imports)

**Implementation Tasks**:

### String Resource Extraction [P]
- [X] T004 Extract strings from `app/src/main/java/com/vjaykrsna/nanoai/ui/components/DisclaimerDialog.kt` - Extract "Decline", "Agree", disclaimer points to `strings.xml` with proper naming convention
- [X] T005 Extract strings from `app/src/main/java/com/vjaykrsna/nanoai/ui/components/OfflineBanner.kt` - Extract "Retry now" and banner messages to resources
- [X] T006 Extract strings from `app/src/main/java/com/vjaykrsna/nanoai/ui/components/PrimaryActionCard.kt` - Extract "Run" text to resources
- [X] T007 Extract strings from `app/src/main/java/com/vjaykrsna/nanoai/ui/sidebar/SidebarContent.kt` - Extract "Conversations", "Search conversations...", "Show Active"/"Show Archived" labels
- [X] T008 Extract strings from `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/sidebar/SettingsShortcutsPanel.kt` - Extract "Open settings", "Theme", "Density" labels
- [X] T009 Extract strings from `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/sidebar/ModelSelectorPanel.kt` - Extract "Manage installed models", "Activate"/"Unavailable offline" states
- [X] T010 Extract strings from `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/shell/NanoShellScaffold.kt` - Extract "Search", "Select model" and all UI labels
- [X] T011 Extract strings from `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/progress/ProgressCenterPanel.kt` - Extract "Retry", "Clear" and progress messages

### Accessibility Improvements [P]
- [X] T012 Audit and fix touch targets in `app/src/main/java/com/vjaykrsna/nanoai/ui/components/` - Ensure all interactive elements meet 48dp minimum using `Modifier.minimumTouchTargetSize()`
- [X] T013 Implement proper content descriptions in `app/src/main/java/com/vjaykrsna/nanoai/feature/chat/ui/ChatScreen.kt` - Add descriptive contentDescription for screen readers
- [X] T014 Add semantic properties to `app/src/main/java/com/vjaykrsna/nanoai/feature/library/ui/ModelLibraryScreen.kt` - Implement proper heading structure and navigation semantics
- [X] T015 Enhance keyboard navigation in `app/src/main/java/com/vjaykrsna/nanoai/feature/settings/ui/SettingsScreen.kt` - Add focus management and keyboard shortcuts
- [X] T016 Validate color contrast ratios in `app/src/main/java/com/vjaykrsna/nanoai/ui/theme/Color.kt` - Test all color combinations against WCAG AA standards (4.5:1 minimum)

### ViewModel Architecture Refactoring
- [X] T017 Split `ShellViewModel` navigation logic into `NavigationViewModel` (`app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/presentation/NavigationViewModel.kt`) - Extract navigation state management
- [X] T018 Create `ConnectivityViewModel` for network status (`app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/presentation/ConnectivityViewModel.kt`) - Handle connectivity banner and offline state
- [X] T019 Extract `ProgressViewModel` for background jobs (`app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/presentation/ProgressViewModel.kt`) - Manage download and processing progress
- [X] T020 Create `ThemeViewModel` for appearance settings (`app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/presentation/ThemeViewModel.kt`) - Handle theme switching and UI preferences
- [X] T021 Refactor `ShellViewModel` to coordinate child ViewModels (`app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/presentation/ShellViewModel.kt`) - Reduce from 518 lines to focused orchestration logic
- [X] T022 Create missing UseCases for ViewModel operations - Address ViewModel layer violations where repositories are called directly instead of through UseCase layer

### UI Component Quality Improvements [P]
- [X] T023 Add state preservation in `app/src/main/java/com/vjaykrsna/nanoai/feature/chat/ui/ChatScreen.kt` - Implement rememberSaveable for scroll position and composer text
- [X] T024 Implement proper error boundaries in `app/src/main/java/com/vjaykrsna/nanoai/ui/navigation/NavigationScaffold.kt` - Add error handling for navigation failures
- [X] T025 Optimize LazyColumn performance in `app/src/main/java/com/vjaykrsna/nanoai/feature/library/ui/ModelLibraryScreen.kt` - Add contentType and proper key parameters
- [X] T026 Add loading states and skeletons in `app/src/main/java/com/vjaykrsna/nanoai/feature/settings/ui/SettingsScreen.kt` - Implement progressive loading UI patterns
- [X] T027 Refactor monolithic `NanoShellScaffold.kt` - Break down into smaller, focused composables (currently 500+ lines)
- [X] T028 Validate Material3 component spacing in `app/src/main/java/com/vjaykrsna/nanoai/ui/theme/` - Audit spacing tokens against Material3 guidelines
- [X] T029 Standardize elevation usage across UI components - Ensure consistent Card elevation values and shadow hierarchy
- [X] T030 Implement high contrast theme option in `app/src/main/java/com/vjaykrsna/nanoai/ui/theme/Theme.kt` - Add accessibility-focused color scheme
- [X] T031 Add skip links for keyboard navigation in `app/src/main/java/com/vjaykrsna/nanoai/ui/navigation/NavigationScaffold.kt` - Implement accessibility navigation shortcuts

## Phase 3: Cross-Cutting Concerns
- [X] T032 Implement UI testing utilities in `app/src/androidTest/java/com/vjaykrsna/nanoai/testing/` - Create accessibility and UI quality test helpers
- [X] T033 Add UI performance monitoring in `app/src/main/java/com/vjaykrsna/nanoai/telemetry/` - Implement JankStats and frame drop tracking
- [X] T034 Create UI quality lint rules in `config/detekt/` - Add custom rules for hardcoded strings and accessibility violations
- [X] T035 Update architecture documentation in `docs/ARCHITECTURE.md` - Document ViewModel splitting strategy and UI quality standards
- [X] T036 Create component usage documentation in `docs/` - Document UI component patterns and Material3 compliance standards

## Dependencies
- String extraction tasks (T004-T011) can execute in parallel [P] as they modify independent files
- Accessibility tasks (T012-T016) depend on string extraction completion
- ViewModel refactoring (T017-T022) must complete before UI component improvements (T023-T031)
- Cross-cutting tasks (T032-T036) depend on all implementation tasks

## Parallel Execution Examples

### Phase 2 Sprint 1: String Extraction
```
T004 (DisclaimerDialog) + T005 (OfflineBanner) + T006 (PrimaryActionCard) + T007 (SidebarContent)
```
*All modify strings.xml independently*

### Phase 2 Sprint 2: Accessibility
```
T012 (Touch targets) + T013 (ChatScreen semantics) + T014 (ModelLibrary semantics) + T015 (Settings keyboard)
```
*Sequential within components, parallel between features*

### Phase 2 Sprint 3: ViewModel Refactoring
```
T017 (Navigation) → T018 (Connectivity) → T019 (Progress) → T020 (Theme) → T021 (Shell refactor)
```
*Sequential dependencies due to shared ShellViewModel*

## Implementation Strategy
**MVP Scope**: Complete string extraction (T001-T011) + basic accessibility (T012-T016) for immediate user impact
**Incremental Delivery**: ViewModel refactoring can be phased over multiple releases to avoid regression risk
**Testing Priority**: UI quality tasks require manual testing + accessibility validation before merge

## Quality Gates
- `./gradlew spotlessCheck` - All code formatting passes
- `./gradlew detekt` - No new violations in UI layer
- `./gradlew :app:lintDebug` - Accessibility and UI quality checks pass
- Manual accessibility testing with TalkBack enabled
- Color contrast validation with automated tools

## Summary
**Total Tasks**: 36 (T001-T036)
**Parallel Opportunities**: 8 tasks marked [P] for concurrent execution
**User Story Coverage**: Complete UI layer architecture fixes with accessibility compliance, clean architecture restoration, and Material Design 3 adherence
**Scope Coverage**: All findings.md UI-related issues + ViewModel layer violations that impact UI architecture
**Quality Gates**: All UI layer violations from audit addressed with comprehensive testing and documentation

---
*Generated following speckit.tasks.prompt.md methodology - Tasks organized by user story with clear dependencies and parallel execution opportunities*
