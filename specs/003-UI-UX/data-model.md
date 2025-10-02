# Data Model: UI/UX — Polished Product-Grade Experience

**Feature**: 003-UI-UX  
**Date**: 2025-10-02  

## Entities

### UserProfile
Represents user-specific UI preferences and metadata.

**Fields**:
- `id`: String (unique identifier, e.g., UUID)
- `displayName`: String? (optional display name)
- `themePreference`: ThemePreference (enum: LIGHT, DARK, SYSTEM)
- `visualDensity`: VisualDensity (enum: DEFAULT, COMPACT, EXPANDED)
- `onboardingCompleted`: Boolean (default false)
- `dismissedTips`: Map<String, Boolean> (per-tip ID dismissals)
- `lastOpenedScreen`: ScreenType (enum: WELCOME, HOME, SETTINGS, etc.)
- `compactMode`: Boolean (default false, for advanced users)
- `pinnedTools`: List<String> (list of tool IDs for quick access)
- `savedLayouts`: List<LayoutSnapshot> (user-saved UI layouts)

**Validation Rules**:
- `id` must be non-empty
- `displayName` max 50 characters if present
- `pinnedTools` max 10 items
- `dismissedTips` keys unique, values boolean
- `savedLayouts` max 5 items

**Relationships**:
- One-to-one with UIStateSnapshot (can be combined)
- One-to-many with LayoutSnapshot

### LayoutSnapshot
Represents a saved UI layout configuration.

**Fields**:
- `id`: String (unique identifier)
- `name`: String (layout name)
- `lastOpenedScreen`: String (screen ID)
- `pinnedTools`: List<String> (pinned items)
- `isCompact`: Boolean (compact mode flag)

**Validation Rules**:
- `name` max 64 characters
- `pinnedTools` max 10 items

### UIStateSnapshot
Represents the current UI state for session restoration.

**Fields**:
- `userId`: String (foreign key to UserProfile.id)
- `expandedPanels`: List<String> (IDs of expanded collapsible panels)
- `recentActions`: List<String> (recently used action IDs, max 5)
- `sidebarCollapsed`: Boolean (default false)

**Validation Rules**:
- `userId` must reference valid UserProfile
- `recentActions` max 5 items

**State Transitions**:
- On app launch: Load from DataStore/Room
- On screen change: Update lastOpenedScreen
- On user action: Add to recentActions, rotate if >5

## Storage Strategy
- Persist UserProfile and UIStateSnapshot in Room database for structured data if needing indexing/querying.
- Use DataStore (Proto or Preferences) for small UI prefs (themePreference, onboarding, dismissedTips).
- LayoutSnapshot in Room for saved layouts.
- Sync across sessions, backup-friendly.
