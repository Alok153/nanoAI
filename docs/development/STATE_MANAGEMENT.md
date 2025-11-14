# Feature ViewModel State Contract

This document captures the standard state-management contract enforced across feature layers. It supplements the high-level guidance in `AGENTS.md` and gives concrete expectations for implementers.

## Core Principles

1. **Single Source of Truth** – every feature ViewModel must expose UI state via `ViewModelStateHost<S, E>` where `S : NanoAIViewState` and `E : NanoAIViewEvent`.
2. **Immutable Snapshots** – `S` must be an immutable `data class` with copy semantics. State updates go through `updateState { copy(...) }` to keep reducers deterministic and observable.
3. **Typed Events** – One-off side effects (toasts, navigation, dialogs) travel through the `events` shared flow returned by `ViewModelStateHost`. Each event type implements `NanoAIViewEvent` for consistency and tooling support.
4. **Scoped Dispatchers** – `ViewModelStateHost` requires an injected `CoroutineDispatcher`. All long-running work should run on that dispatcher to keep tests deterministic.
5. **Error Parity** – recoverable issues update the state with user-facing context *and* emit a dedicated event payload so UI can react without re-deriving the error message.

## Required Structure

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    @MainImmediateDispatcher private val dispatcher: CoroutineDispatcher,
    // ...other dependencies
) : ViewModelStateHost<FeatureUiState, FeatureUiEvent>(
    initialState = FeatureUiState(),
    dispatcher = dispatcher,
) {
    fun doSomething() {
        viewModelScope.launch(dispatcher) {
            updateState { copy(isLoading = true) }
            runCatching { repository.fetch() }
                .onSuccess { data -> updateState { copy(isLoading = false, data = data) } }
                .onFailure { error -> publishError(error) }
        }
    }

    private suspend fun publishError(error: Throwable) {
        updateState { copy(isLoading = false, errorMessage = error.message) }
        emitEvent(FeatureUiEvent.ErrorRaised(error.message ?: "Unexpected error"))
    }
}

data class FeatureUiState(
    val isLoading: Boolean = false,
    val data: List<Item> = emptyList(),
    val errorMessage: String? = null,
) : NanoAIViewState

sealed interface FeatureUiEvent : NanoAIViewEvent {
    data class ErrorRaised(val message: String) : FeatureUiEvent
}
```

## Layer Responsibilities

- **Composable layer** collects `state` via `collectAsStateWithLifecycle()` and handles `events` inside a single `LaunchedEffect(stateHost.events)` block.
- **ViewModel layer** never exposes raw `MutableStateFlow`/`MutableSharedFlow` instances. Specialized flows (e.g., `InstalledModelsFlow`) should be mapped to state properties or adapters.
- **UseCase / Repository layers** surface typed results (`NanoAIResult` for one-shot operations, `Flow<T>` for reactive streams). ViewModels adapt them into the state contract.

## Migration Strategy

1. Replace legacy `ViewModel` subclasses with `ViewModelStateHost` wrappers.
2. Move any `MutableStateFlow` properties into the `state` data class.
3. Define typed `UiEvent` containers instead of exposing `SharedFlow<Error>`/`SharedFlow<Boolean>`.
4. Update UI collectors to rely on the standardized `state`/`events` API only.
5. Backfill or update tests to assert state reducers and event emissions via the new host abstraction.

## Adopted ViewModels (Nov 2025)

- `ImageGenerationViewModel`
- `AudioViewModel`
- `MessageComposerViewModel`
- `ImageGalleryViewModel`

Each of the above now emits errors through `Image*UiEvent.ErrorRaised` carrying a `NanoAIErrorEnvelope`. Compose surfaces (`ImageGenerationScreen`, `AudioScreen`, `ImageGalleryScreen`) collect a single `events` flow to coordinate snackbars and inline messaging.
