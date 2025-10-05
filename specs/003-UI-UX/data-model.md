# Data Model — UI/UX — Polished Product-Grade Experience

## Overview
UI refactor relies on shared state objects that orchestrate the home hub, dual sidebars, command palette, progress center, and offline handling across modes. These models extend existing Room/DataStore entities while introducing new UI-only state wrappers in the presentation layer.

## Entities

### `ShellLayoutState`
| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `windowSizeClass` | `WindowSizeClass` | Calculated | Controls modal vs permanent drawer and column counts. |
| `isLeftDrawerOpen` | `Boolean` | ViewModel state | Persisted per session via `UiStateSnapshotEntity`. |
| `isRightDrawerOpen` | `Boolean` | ViewModel state | Right drawer hosts contextual controls / progress center. |
| `activeMode` | `ModeId` | Nav controller | `ModeId` enumerates `HOME`, `CHAT`, `IMAGE`, `AUDIO`, `CODE`, `TRANSLATE`, `HISTORY`, `LIBRARY`, `SETTINGS`, `TOOLS`. |
| `showCommandPalette` | `Boolean` | ViewModel state | Tracks palette overlay visibility and focus management. |
| `connectivity` | `ConnectivityStatus` | Flow from connectivity monitor | `ONLINE`, `OFFLINE`, `LIMITED`; drives banner. |
| `pendingUndoAction` | `UndoPayload?` | Shared event channel | Enables inline undo per spec. |

### `ModeCard`
| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `id` | `ModeId` | Static | Keys into navigation graph. |
| `title` | `String` | Localized strings | Short label ("Chat"). |
| `subtitle` | `String` | Localized strings | Optional description. |
| `icon` | `ImageVector` | Material Symbols | Compose vector resource. |
| `primaryAction` | `CommandAction` | Derived | Launch or quick-start (e.g., "New Chat"). |
| `enabled` | `Boolean` | Feature flags/offline | Disabled offline if mode unavailable. |
| `badge` | `BadgeInfo?` | Derived | Show progress or new features. |

### `CommandPaletteState`
| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `query` | `String` | Mutable state | Filter actions. |
| `results` | `List<CommandAction>` | Combined | Union of navigation targets, quick actions, recent jobs. |
| `recentCommands` | `List<CommandAction>` | DataStore | Maintains last N actions. |
| `selectedIndex` | `Int` | Mutable state | Keyboard navigation. |
| `surfaceTarget` | `CommandCategory` | Derived | Grouping: Modes, History, Jobs, Settings. |

### `ProgressJob`
| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `jobId` | `UUID` | WorkManager/Room | Links to queued generation/download job. |
| `type` | `JobType` | Domain | `IMAGE_GENERATION`, `AUDIO_RECORDING`, `MODEL_DOWNLOAD`, etc. |
| `status` | `JobStatus` | Domain | `PENDING`, `RUNNING`, `PAUSED`, `FAILED`, `COMPLETED`. |
| `progress` | `Float` | Derived | 0f..1f, from WorkManager progress or inference callback. |
| `eta` | `Duration?` | Calculated | Heuristic for user feedback. |
| `canRetry` | `Boolean` | Domain | Based on failure type + offline state. |
| `queuedAt` | `Instant` | Room `DownloadTask`/`GenerationLog` | For ordering. |

### `ConnectivityBannerState`
| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `status` | `ConnectivityStatus` | Connectivity monitor | Drives copy and visuals. |
| `lastDismissedAt` | `Instant?` | DataStore | Avoids re-showing within cooldown. |
| `queuedActionCount` | `Int` | Use cases | Number of actions waiting to sync. |
| `cta` | `CommandAction?` | Derived | Quick jump to progress center or retry. |

### `UiPreferenceSnapshot`
| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `theme` | `ThemeMode` | DataStore | `LIGHT`, `DARK`, `SYSTEM`. |
| `density` | `DensityMode` | DataStore | `COMPACT`, `COMFORTABLE`. |
| `fontScale` | `Float` | DataStore | 1.0 default; respects accessibility requirements. |
| `onboardingCompleted` | `Boolean` | DataStore | For rewinding help overlays. |
| `dismissedTooltips` | `Set<String>` | DataStore | Global store for composer/tooltips "Don't show again". |

### `RecentActivityItem`
| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `id` | `String` | Room `ChatThreadEntity` / `ImageGenerationEntity` | Unique reference. |
| `modeId` | `ModeId` | Derived | For icon + routing. |
| `title` | `String` | Domain | Thread title / asset name. |
| `timestamp` | `Instant` | Domain | Sorting and recency text. |
| `status` | `RecentStatus` | Domain | `COMPLETED`, `IN_PROGRESS`, `FAILED`. |

## Supporting Types
- `ModeId`: sealed hierarchy enumerating all navigable surfaces.
- `CommandAction`: label, icon, shortcut, `CommandDestination` (navigate, open drawer, trigger job).
- `UndoPayload`: data needed to reverse a destructive action (e.g., deleted chat, cancelled download).
- `ConnectivityStatus`: tri-state network health.
- `JobStatus`: matches WorkManager states with additional `STREAMING` for chat/image generation.
- `BadgeInfo`: count, type (`NEW`, `PRO`, `SYNCING`).

## Persistence Impact
- Extend `UiStateSnapshotEntity` to include `isLeftDrawerOpen`, `isRightDrawerOpen`, `activeMode`, maintaining encryption for pinned elements.
- Add DataStore keys for `command_palette_recent`, `connectivity_banner_last_dismissed`, `density_mode` (if absent).
- Room migrations: ensure new columns default to safe values (`false`, `HOME`) to avoid breaking existing sessions.

## ViewModel Responsibilities
- Shell ViewModel aggregates Flows from `UiPreferenceRepository`, `ConnectivityMonitor`, `ProgressRepository`, and navigation events, exposing immutable `StateFlow<ShellLayoutState>`.
- Command palette uses coroutine snapshotFlow over search query and action providers (navigation graph, job queue) for responsive updates.
- Progress center subscribes to WorkManager + Room flows to keep job list fresh and actionable.

## Relationships Diagram (textual)
```
UiPreferenceRepository ─┐
ProgressRepository ─────┼─► ShellViewModel ─► ShellLayoutState
ConnectivityMonitor ────┘             │
                                      ├─► CommandPaletteState (derived)
RecentActivityUseCase ────────────────┘             │
                                                   └─► ProgressJob list
```
