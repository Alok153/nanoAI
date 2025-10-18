# Contract: Unified UI Shell & Command Palette

## Overview
Defines the expected behavior for the unified navigation shell ViewModel that manages drawers, command palette visibility, connectivity banners, and mode routing. Combines shell state management with command palette interaction contracts.

## State Shape
```
ShellLayoutState(
  windowSizeClass: WindowSizeClass,
  activeMode: ModeId,
  isLeftDrawerOpen: Boolean,
  isRightDrawerOpen: Boolean,
  showCommandPalette: Boolean,
  connectivity: ConnectivityStatus,
  pendingUndoAction: UndoPayload?,
  progressJobs: List<ProgressJob>,
  recentActivity: List<RecentActivityItem>
)
```

## Intents & Interactions

### Navigation & Drawer Management
| Intent | Input | Preconditions | Expected State Change | Side Effects |
|--------|-------|---------------|-----------------------|--------------|
| `OpenMode(modeId: ModeId)` | Mode tap, command palette action | Mode must be registered in navigation graph | `activeMode = modeId`, `isLeftDrawerOpen = false`, `showCommandPalette = false` | Emit navigation command to NavController |
| `ToggleLeftDrawer()` | Menu icon, swipe gesture | none | Flips `isLeftDrawerOpen`; on compact screens ensures palette is hidden | Persist drawer state snapshot |
| `ToggleRightDrawer(panel: RightPanel)` | Command palette action or progress icon | Right panel types: `PROGRESS_CENTER`, `MODEL_SELECTOR`, `SETTINGS_SHORTCUT` | `isRightDrawerOpen = !isRightDrawerOpen` and sets contextual panel | Log analytics event (if enabled by consent) |

### Command Palette Operations
| Intent | Input | Preconditions | Expected State Change | Side Effects |
|--------|-------|---------------|-----------------------|--------------|
| `ShowCommandPalette(source: PaletteSource)` | Keyboard shortcut or search field button | Drawer closed on compact devices | `showCommandPalette = true`, palette pre-populated with `source` results | focus request to palette field |
| `HideCommandPalette()` | Escape key, overlay dismiss | Palette visible | `showCommandPalette = false`, `selectedIndex` reset | Persist query to recent commands if executed |

### Offline & Progress Management
| Intent | Input | Preconditions | Expected State Change | Side Effects |
|--------|-------|---------------|-----------------------|--------------|
| `QueueGeneration(job: ProgressJob)` | Generate button press while offline | `connectivity != ONLINE` OR local model busy | Adds job with `status = PENDING`, ensures WorkManager enqueue on reconnect | Show inline toast + update offline banner count |
| `CompleteJob(jobId)` | Job finishing callback | Job exists | Removes job or marks `status = COMPLETED` for display | Triggers snackbar "Saved" + optional undo payload |
| `UndoAction(payload: UndoPayload)` | Undo button press | `pendingUndoAction` not null | Reverts associated repository change, clears payload | Reschedules job if relevant |
| `UpdateConnectivity(status)` | OS network callback | none | Updates `connectivity`, toggles offline banner visibility | If transitioning to ONLINE, flush queued jobs |

## Command Palette UI Tests

### Test Environment
- Compose UI test using `createAndroidComposeRule<ComponentActivity>()`.
- Inject test doubles for `CommandPaletteStateProvider` and `NavController` via Hilt test components.
- Palette overlay rendered above unified shell using test tag `"command_palette"`.

### Required Tests
| Test ID | Description | Steps | Assertions |
|---------|-------------|-------|------------|
| `Palette_opensWithShortcut` | Ensure palette opens within <1 frame after shortcut | 1. Launch shell content. 2. Send `KeyEvent` for `Ctrl+K`. | Palette node exists, `searchField` focused, open timestamp - event timestamp < 16 ms. |
| `Palette_filtersActions` | Verify query filters available commands | 1. Open palette. 2. Input "ima". | Results list shows Image Generation command first, others filtered out. |
| `Palette_keyboardNavigation` | Ensure arrow keys cycle results with wrap-around | 1. Open palette. 2. Send `ArrowDown` thrice. | `selectedIndex` cycles across results, `aria-selected` semantics updated. |
| `Palette_executesAction_closes` | Command execution navigates and closes palette | 1. Select "New Chat" command. 2. Press Enter. | NavController received `navigate(ModeId.CHAT)`, palette hidden, left drawer closed. |
| `Palette_disabledMode_showsError` | Disabled actions surface inline error | 1. Mark `CommandAction.enabled=false`. 2. Attempt execution. | Snackbar/toast shows "Unavailable offline", palette remains open with same selection. |

## Error Handling
- Invalid `modeId` MUST fail fast (log error) and leave state unchanged.
- WorkManager enqueue failures raise `pendingUndoAction` with retry CTA.
- Palette actions referencing disabled modes should surface inline error toast and remain selected for alternative choice.

## Accessibility Expectations
- Palette root must set `Role.Dialog` and `aria-modal=true` semantics.
- Each command item exposes `contentDescription` with shortcut hint.
- Provide `TalkBack` order: search field → results list → footer hints.

## Test Matrix
| Test ID | Scenario | Assertions |
|---------|----------|------------|
| `ShellViewModel_openMode_closesDrawers` | Left drawer open, user selects Chat | Drawer closes, palette hidden, activeMode=CHAT, nav command emitted once |
| `ShellViewModel_toggleRightDrawer_progressPanel` | Right drawer closed, user taps progress icon | Drawer opens with `panel=PROGRESS_CENTER`, preserves left drawer state |
| `ShellViewModel_queueGeneration_offline` | Offline status, user presses Generate | `ProgressJob` added with status=PENDING, toast event fired, offline banner count increments |
| `ShellViewModel_completeJob_emitsUndo` | Job completes with undoable action | Job removed, snackbar event with undo, `pendingUndoAction` populated |
| `ShellViewModel_updateConnectivity_flushesQueue` | Jobs waiting, connectivity transitions OFFLINE→ONLINE | Queue flush invoked, offline banner hidden, status=ONLINE |

## Offline & Progress Center Scenarios

### UI Behavior Contracts
| Scenario | Given | When | Then |
|----------|-------|------|------|
| `OfflineBanner_showsWithQueuedActions` | Device transitions to OFFLINE, there are 2 queued jobs | Connectivity monitor emits `OFFLINE` | Banner visible with message "Working offline" and CTA "View queue (2)"; right drawer badge increments |
| `OfflineBanner_dismissPersists` | Banner shown, user dismisses within session | User taps dismiss | Banner hidden for 30 minutes; DataStore records `lastDismissedAt`; reappears immediately if more jobs added |
| `ProgressCenter_listsQueuedJobs` | Two downloads paused due to offline | User opens progress center | List displays each job with progress 0%, status "Waiting for connection", retry button disabled |
| `Reconnect_flushesQueue` | Offline jobs exist, network restores | Connectivity emits `ONLINE` | WorkManager enqueues pending jobs, banner replaced with "Syncing..." state, eventually hidden once queue empty |
| `Retry_failedJob` | Job failed due to quota | User taps retry in progress center | ViewModel checks quota (via repository). If allowed, re-enqueue and show toast "Retry scheduled"; otherwise display inline error |

### Testing & Implementation Notes
- ViewModel uses `stateIn` to expose `ShellLayoutState`; all intents executed via `viewModelScope.launch`.
- Drawer persistence stored via `UiStateRepository.persistShellState()` on each toggle debounced.
- Command palette results aggregated from `ModeRegistry`, `RecentActivityRepository`, and `ProgressRepository`.
- Provide fake `ConnectivityMonitor` + `ProgressRepository` to deterministically emit job states.
- Compose semantics IDs: `"connectivity_banner"`, `"progress_list"`, `"progress_retry_button"`.
- Banner MUST not appear when command palette is visible to avoid focus trap.
- Queue updates do not block UI thread; snapshot flows run on `Dispatchers.Default`.
- Log connectivity transition timestamps for macrobenchmark.
- Record queue length when palette opens to enable quick commands (non-blocking).
- Failure Handling: Tests fail if open latency exceeds threshold (use `SystemClock.elapsedRealtime()` instrumentation).
