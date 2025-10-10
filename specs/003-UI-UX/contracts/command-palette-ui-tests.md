# Contract: Command Palette Compose Tests

## Objective
Guarantee keyboard-first navigation, quick action discovery, and correct execution for the global command palette overlay.

## Test Environment
- Compose UI test using `createAndroidComposeRule<ComponentActivity>()`.
- Inject test doubles for `CommandPaletteStateProvider` and `NavController` via Hilt test components.
- Palette overlay rendered above unified shell using test tag `"command_palette"`.

## Required Tests
| Test ID | Description | Steps | Assertions |
|---------|-------------|-------|------------|
| `Palette_opensWithShortcut` | Ensure palette opens within <1 frame after shortcut | 1. Launch shell content. 2. Send `KeyEvent` for `Ctrl+K`. | Palette node exists, `searchField` focused, open timestamp - event timestamp < 16 ms. |
| `Palette_filtersActions` | Verify query filters available commands | 1. Open palette. 2. Input "ima". | Results list shows Image Generation command first, others filtered out. |
| `Palette_keyboardNavigation` | Ensure arrow keys cycle results with wrap-around | 1. Open palette. 2. Send `ArrowDown` thrice. | `selectedIndex` cycles across results, `aria-selected` semantics updated. |
| `Palette_executesAction_closes` | Command execution navigates and closes palette | 1. Select "New Chat" command. 2. Press Enter. | NavController received `navigate(ModeId.CHAT)`, palette hidden, left drawer closed. |
| `Palette_disabledMode_showsError` | Disabled actions surface inline error | 1. Mark `CommandAction.enabled=false`. 2. Attempt execution. | Snackbar/toast shows "Unavailable offline", palette remains open with same selection. |

## Accessibility Expectations
- Palette root must set `Role.Dialog` and `aria-modal=true` semantics.
- Each command item exposes `contentDescription` with shortcut hint.
- Provide `TalkBack` order: search field → results list → footer hints.

## Failure Handling
- Tests fail if open latency exceeds threshold (use `SystemClock.elapsedRealtime()` instrumentation).
- Tests fail if more than one navigation command emitted per execution.
