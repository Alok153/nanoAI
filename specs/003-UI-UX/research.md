# Phase 0 Research — UI/UX — Polished Product-Grade Experience

## Overview
Assessed the existing nanoAI Android shell, current Compose screen implementations, and the desired UX vision captured in `overview.md`. Focus areas: adaptive navigation shell, global command/feedback systems, and offline/performance guarantees.

## Decisions

### 1. Adaptive Navigation Shell and Sidebars
- **Decision**: Consolidate shell into a single `ModalNavigationDrawer` + `PermanentNavigationDrawer` implementation that switches based on `WindowSizeClass`, with Home Hub and mode screens presented inside a shared `Scaffold` managed by `feature/uiux`.
- **Rationale**: Aligns with spec priorities for consistency and responsiveness; Material 3 guidance recommends modal drawers for compact screens and permanent drawers for wide layouts. A single shell reduces duplicated scaffolds across `feature/chat`, `feature/library`, and `feature/settings` while enabling shared accessibility and theming logic.
- **Alternatives Considered**:
  - Keep per-feature `Scaffold` compositions: rejected because it perpetuates inconsistent paddings, top-bar duplication, and makes command palette injection harder.
  - Navigation rail only for large screens: rejected because right-sidebar (contextual controls) needs simultaneous surface; dual-drawer shell offers better affordance.
- **References**: Jetpack Compose Material 3 navigation drawer docs (ModalNavigationDrawer, ModalDrawerSheet, PermanentNavigationDrawer) retrieved via Context7 on 2025-10-06.

### 2. Home Hub Layout & Mode Launch Targets
- **Decision**: Implement the Home Hub as a responsive grid (2-column compact, 3-column medium, 4-column expanded) with quick actions row and recent activity list, backed by existing Room data for recents/history.
- **Rationale**: Meets spec goal of reaching any mode within two interactions and leverages existing `History` data models. Grid scaling ensures calm but information-rich presentation and prepares for future mode additions.
- **Alternatives Considered**:
  - Linear list for modes: rejected because it slows access for larger catalogs and underutilizes wide layouts.
  - Separate tabs per mode: rejected due to navigation friction and mismatch with sidebar-first architecture.

### 3. Global Command Palette & Progress Center
- **Decision**: Introduce a command palette overlay triggered by keyboard shortcut and top search bar button, sourcing actions from navigation graph + recent jobs. Progress Center will reuse existing WorkManager download telemetry and inference job state, surfaced in a right-side drawer panel with filterable queue.
- **Rationale**: Overview emphasizes keyboard-first navigation and unified job tracking. Reusing existing repositories avoids new persistence layers while centralizing feedback.
- **Alternatives Considered**:
  - Keep disparate snackbars/toasts: rejected because users currently lack a single place to monitor AI jobs or offline queues.
  - Modal dialogs for progress: rejected for obstructing ongoing workflows.

### 4. Offline & Performance Safeguards
- **Decision**: Maintain connectivity banner state within shared ViewModel using DataStore-backed preference for dismissal, ensure queued jobs persist via Room, and run macrobenchmark + Compose semantics accessibility tests as part of validation.
- **Rationale**: Constitution requires offline readiness and measurable performance. Centralizing connectivity state stops duplication (currently each feature re-implements). Macrobenchmark ensures transitions respect <100 ms budgets.
- **Alternatives Considered**:
  - Feature-specific offline banners: rejected due to inconsistent messaging and higher maintenance.
  - Skipping macrobenchmarks: rejected because UI refactor may affect start-up/tap latency.

## Current State Audit Highlights
- Each feature (`feature/chat/ui/ChatScreen`, etc.) defines its own `Scaffold`, leading to inconsistent paddings and no shared command palette injection.
- `feature/sidebar` currently exposes a narrow Drawer implementation that lacks adaptive behavior and duplicates items between modes.
- There is no centralized progress queue UI; downloads live in Model Library while generation jobs only show snackbars.
- Offline handling is per-feature; banners missing in Chat and Code surfaces.

## Validation & Tooling Notes
- Adopt `calculateWindowSizeClass` in the shell ViewModel and provide Compose previews for compact/medium/expanded states.
- Extend macrobenchmark module to cover Home Hub launch + mode switch to ensure budgets.
- Compose testing: use `setContent` with `TestNavHostController` for verifying command palette navigation.

## Open Follow-ups
- Confirm copywriting for command palette categories with product before implementation.
- Validate if additional telemetry events are needed when palette actions trigger local inference.
