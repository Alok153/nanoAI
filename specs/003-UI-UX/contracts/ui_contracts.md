# UI Contracts: UI/UX — Polished Product-Grade Experience

**Feature**: 003-UI-UX  
**Date**: 2025-10-02  

## Overview
Contracts for UI components and screens based on functional requirements. Each contract defines the interface, behavior, and validation for UI elements.

## Welcome Screen Contract
**Requirement**: FR-001 - Clear Welcome experience for first-time users.

**Interface**:
- `WelcomeScreen(onGetStarted: () -> Unit, onExploreFeatures: () -> Unit, onSkip: () -> Unit)`

**Behavior**:
- Display hero message, 2-3 CTAs, progress/skip control.
- Onboarding highlight if applicable.

**Validation**:
- CTAs must be accessible and respond within 100ms.

## Home Screen Contract
**Requirement**: FR-002 - Prioritize recent/recommended actions in single-column area.

**Interface**:
- `HomeScreen(recentActions: List<Action>, onActionClick: (Action) -> Unit, onExpandTools: () -> Unit)`

**Behavior**:
- Single-column content, collapsible tools panel.
- Surface most-recently used actions.

**Validation**:
- Layout adapts to screen size, tools discoverable but collapsed by default.

## Sidebar Navigation Contract
**Requirement**: FR-012 - Left-side persistent sidebar, collapsible on mobile.

**Interface**:
- `Sidebar(drawerState: DrawerState, onNavigate: (Screen) -> Unit)`

**Behavior**:
- Persistent on large screens, drawer on mobile.
- Keyboard/screen reader accessible.

**Validation**:
- Supports deep linking, collapsible without layout jumps.

## Settings Screen Contract
**Requirement**: FR-008 - Grouped options with descriptions.

**Interface**:
- `SettingsScreen(settings: Map<String, Setting>, onSettingChange: (String, Any) -> Unit)`

**Behavior**:
- Options grouped, inline help, persistent Save/Undo.

**Validation**:
- Changes undoable, help accessible.

## Theme Toggle Contract
**Requirement**: FR-011 - Light/Dark themes with toggle and system sync.

**Interface**:
- `ThemeToggle(currentTheme: ThemePreference, onThemeChange: (ThemePreference) -> Unit)`

**Behavior**:
- Instant switch, persists in profile.

**Validation**:
- No layout jumps, contrast maintained.

## Offline Banner Contract
**Requirement**: FR-006 - Graceful offline UX.

**Interface**:
- `OfflineBanner(isOffline: Boolean, onRetry: () -> Unit)`

**Behavior**:
- Show when offline, disable unavailable features.

**Validation**:
- Clear messaging, queue sync operations.

## Onboarding Tooltip Contract
**Requirement**: FR-013 - Lightweight tooltips.

**Interface**:
- `OnboardingTooltip(message: String, onDismiss: () -> Unit, onDontShowAgain: () -> Unit)`

**Behavior**:
- Dismissible, non-modal, re-openable from Help.

**Validation**:
- Doesn't block UI, persists choices.
