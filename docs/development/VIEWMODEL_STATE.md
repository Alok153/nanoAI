# ViewModel State Standardization

## Why This Pattern
- Aligns with `AGENTS.md` clean architecture rules by routing UI → ViewModel → UseCase flows through a single, testable state host.
- Addresses the "ViewModel State Management Inconsistencies" issue recorded in `Personal/issues.md` by normalising how state and side effects are exposed.
- Protects coverage targets (ViewModel ≥75%, UI ≥65%) by making state deterministic and easy to exercise via the shared harness.

## Core Concepts

### Single Source of Truth
Each ViewModel owns exactly one `StateFlow<UiState>` derived from a data class that implements `NanoAIViewState`. Reductions must be pure and never mutate shared references.

```kotlin
@Stable
data class ChatUiState(
    val composerText: String = "",
    val messages: PersistentList<Message> = persistentListOf(),
    val isLoading: Boolean = false,
    val activeThread: ChatThreadSummary? = null,
    val activePersona: PersonaProfile? = null,
    val showModelPicker: Boolean = false,
    val attachments: ChatAttachments = ChatAttachments()
) : NanoAIViewState
```

### Event Streams
Non-idempotent one-off effects (navigation, snackbars, system prompts) surface through a typed `NanoAIEventChannel<E>` where `E : NanoAIViewEvent`. The event channel exposes a cold `SharedFlow` for observers.

```kotlin
sealed interface ChatEvent : NanoAIViewEvent {
    data class ShowSnackbar(val message: String) : ChatEvent
    data class Navigate(val destination: ChatDestination) : ChatEvent
}
```

### Reducer Helpers
`ViewModelStateHost` wraps a `MutableStateFlow` and provides suspend/inline helpers:

- `updateState { copy(isLoading = true) }`
- `launchAndReduce { emitLoading(); runUseCase(); emitResult(); }`
- `emitEvent(ChatEvent.ShowSnackbar("Switched model"))`

Reducers must be idempotent and free of side effects. Expensive work belongs in `viewModelScope` coroutines triggered from intent handlers.

### Test Harness
`ViewModelStateHostTestHarness` centralises Turbine collectors and dispatcher swapping so feature tests only express intents and expected state snapshots:

```kotlin
val harness = ViewModelStateHostTestHarness(viewModel)

harness.assertInitialState { it.isLoading is false }

harness.testStates {
    viewModel.onComposerTextChanged("Hello")
    viewModel.onSendMessage()
    awaitState { it.isLoading }
    awaitState { !it.isLoading && it.messages.last().text == "Hello" }
}

harness.testEvents {
    viewModel.onErrorAcknowledged()
    awaitEvent<ChatEvent.ShowSnackbar> { it.message.contains("error") }
}
```

## Implementation Checklist
1. Extend `ViewModelStateHost<UiState, Event>` and pass the initial state via the constructor.
2. Handle UI intents through public functions that launch work on the injected dispatcher.
3. Call `updateState` for state changes and `emitEvent` for one-off effects.
4. Keep ViewModel properties private; the UI only observes `state` and `events`.
5. Use immutable collections (`PersistentList`, `PersistentMap`) for multi-item state fields.
6. Surface errors via state when they drive UI, otherwise bubble through events.

## Testing Strategy
- Always pair a ViewModel test suite with the `ViewModelStateHostTestHarness` to exercise reducers and events.
- Use `MainDispatcherRule`/`MainDispatcherExtension` to control `viewModelScope` scheduling.
- Assert the entire `UiState` snapshot after significant interactions to maintain coverage depth.

## Migration Tips
- Start by modelling the desired `UiState` and making tests drive the migration.
- Replace multiple `MutableStateFlow`/`SharedFlow` instances with the host API incrementally.
- Ensure UI composables observe only the new unified state/event streams before deleting legacy fields.
- Update documentation (`docs/development/TESTING.md`, `docs/development/UI_COMPONENTS.md`) with feature-specific notes once migrated.

## Adoption Tracker
| Feature / ViewModel | Status | Next Steps |
| --- | --- | --- |
| `ModelLibraryViewModel` | ✅ Complete | Covered by T012–T017 in `specs/001-foundation/tasks.md`.
| `HuggingFaceLibraryViewModel` | ✅ Complete | Shared state/events aligned in T015.
| `SettingsViewModel` | ✅ Complete | Unified `SettingsUiState` + harnessed tests (T018–T021).
| `ChatViewModel` | ✅ Complete | Unified `ChatUiState` (composer text, attachments, persistent collections) with refreshed harness coverage (T007–T011).
| `HistoryViewModel` | ✅ Complete | Shared host adoption with persistent state collections and updated tests (T010–T011).
| Remaining feature ViewModels | 🔍 Assess | Audit per sprint; open follow-up tasks for audio, image, and settings submodules as needed.

## References
- `AGENTS.md` architecture and coverage requirements.
- `Personal/issues.md` refactoring backlog for inconsistent ViewModel state patterns.
- `docs/development/TESTING.md` for coverage thresholds and tooling expectations.
