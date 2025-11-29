# Feature ViewModel State Contract

Standard state-management contract for feature ViewModels. Supplements `AGENTS.md`.

## ViewModel Classification

### Feature ViewModels (MUST extend `ViewModelStateHost`)
Handle user-facing functionality with consistent state/event handling.

**Examples:** `ChatViewModel`, `ImageGenerationViewModel`, `SettingsViewModel`, `ModelLibraryViewModel`

### Shell/Container ViewModels (MAY be plain `ViewModel`)
Handle navigation, connectivity, and shell orchestration.

**Examples:** `AppViewModel`, `ShellViewModel`, `NavigationCoordinator`, `ConnectivityCoordinator`

## Core Principles

1. **Single Source of Truth** – Expose UI state via `ViewModelStateHost<S, E>`
2. **Immutable Snapshots** – Use `data class` with `copy()` for state updates
3. **Typed Events** – One-off side effects through `events` shared flow
4. **Scoped Dispatchers** – Inject dispatcher for deterministic tests
5. **Error Parity** – Update state AND emit event for recoverable errors

## Required Structure

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    @MainImmediateDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModelStateHost<FeatureUiState, FeatureUiEvent>(
    initialState = FeatureUiState(),
    dispatcher = dispatcher,
) {
    fun doSomething() {
        viewModelScope.launch(dispatcher) {
            updateState { copy(isLoading = true) }
            runCatching { repository.fetch() }
                .onSuccess { updateState { copy(isLoading = false, data = it) } }
                .onFailure { publishError(it) }
        }
    }
}

data class FeatureUiState(
    val isLoading: Boolean = false,
    val data: List<Item> = emptyList(),
) : NanoAIViewState

sealed interface FeatureUiEvent : NanoAIViewEvent {
    data class ErrorRaised(val message: String) : FeatureUiEvent
}
```

## Layer Responsibilities

| Layer | Responsibility |
|-------|---------------|
| Composable | Collect `state` via `collectAsStateWithLifecycle()`, handle `events` in `LaunchedEffect` |
| ViewModel | Never expose raw `MutableStateFlow`/`MutableSharedFlow` |
| UseCase/Repository | Return `NanoAIResult` for one-shot, `Flow<T>` for reactive |
