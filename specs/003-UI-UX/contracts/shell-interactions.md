# Contract: Shell ViewModel Intents & State Reductions

## Scope
Defines the expected behavior for the unified navigation shell ViewModel that manages drawers, command palette visibility, connectivity banners, and mode routing.

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

## Intents
| Intent | Input | Preconditions | Expected State Change | Side Effects |
|--------|-------|---------------|-----------------------|--------------|
| `OpenMode(modeId: ModeId)` | Mode tap, command palette action | Mode must be registered in navigation graph | `activeMode = modeId`, `isLeftDrawerOpen = false`, `showCommandPalette = false` | Emit navigation command to NavController |
| `ToggleLeftDrawer()` | Menu icon, swipe gesture | none | Flips `isLeftDrawerOpen`; on compact screens ensures palette is hidden | Persist drawer state snapshot |
| `ToggleRightDrawer(panel: RightPanel)` | Command palette action or progress icon | Right panel types: `PROGRESS_CENTER`, `MODEL_SELECTOR`, `SETTINGS_SHORTCUT` | `isRightDrawerOpen = !isRightDrawerOpen` and sets contextual panel | Log analytics event (if enabled by consent) |
| `ShowCommandPalette(source: PaletteSource)` | Keyboard shortcut or search field | Drawer closed on compact devices | `showCommandPalette = true`, palette pre-populated with `source` results | focus request to palette field |
| `HideCommandPalette()` | Escape key, overlay dismiss | Palette visible | `showCommandPalette = false`, `selectedIndex` reset | Persist query to recent commands if executed |
| `QueueGeneration(job: ProgressJob)` | Generate button press while offline | `connectivity != ONLINE` OR local model busy | Adds job with `status = PENDING`, ensures WorkManager enqueue on reconnect | Show inline toast + update offline banner count |
| `CompleteJob(jobId)` | Job finishing callback | Job exists | Removes job or marks `status = COMPLETED` for display | Triggers snackbar "Saved" + optional undo payload |
| `UndoAction(payload: UndoPayload)` | Undo button press | `pendingUndoAction` not null | Reverts associated repository change, clears payload | Reschedules job if relevant |
| `UpdateConnectivity(status)` | OS network callback | none | Updates `connectivity`, toggles offline banner visibility | If transitioning to ONLINE, flush queued jobs |

## Error Handling
- Invalid `modeId` MUST fail fast (log error) and leave state unchanged.
- WorkManager enqueue failures raise `pendingUndoAction` with retry CTA.
- Palette actions referencing disabled modes should surface inline error toast and remain selected for alternative choice.

## Test Matrix
| Test ID | Scenario | Assertions |
|---------|----------|------------|
| `ShellViewModel_openMode_closesDrawers` | Left drawer open, user selects Chat | Drawer closes, palette hidden, activeMode=CHAT, nav command emitted once |
| `ShellViewModel_toggleRightDrawer_progressPanel` | Right drawer closed, user taps progress icon | Drawer opens with `panel=PROGRESS_CENTER`, preserves left drawer state |
| `ShellViewModel_queueGeneration_offline` | Offline status, user presses Generate | `ProgressJob` added with status=PENDING, toast event fired, offline banner count increments |
| `ShellViewModel_completeJob_emitsUndo` | Job completes with undoable action | Job removed, snackbar event with undo, `pendingUndoAction` populated |
| `ShellViewModel_updateConnectivity_flushesQueue` | Jobs waiting, connectivity transitions OFFLINE→ONLINE | Queue flush invoked, offline banner hidden, status=ONLINE |

## Implementation Notes
- ViewModel uses `stateIn` to expose `ShellLayoutState`; all intents executed via `viewModelScope.launch`.
- Drawer persistence stored via `UiStateRepository.persistShellState()` on each toggle debounced.
- Command palette results aggregated from `ModeRegistry`, `RecentActivityRepository`, and `ProgressRepository`.
