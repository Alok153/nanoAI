# Reactive vs One-Shot Data Contracts

To address the inconsistency raised in `Personal/issues.md`, data-access APIs are now labeled with explicit contracts:

- `@ReactiveStream` marks infinite streams that must be collected (e.g., `ConversationUseCase.getAllThreadsFlow`). These functions never wrap their payloads in `NanoAIResult`; upstream failures are surfaced via the stream itself.
- `@OneShot` marks suspend functions that complete exactly once and return a `NanoAIResult`. They are safe to invoke from coroutines and can be retried with the metadata inside the accompanying `NanoAIErrorEnvelope`.

## Usage Guidelines

1. **Expose intent** – Annotate every public repository/use-case function so call sites know whether to `collect` or `await`.
2. **Avoid hybrids** – Do not return `Flow<NanoAIResult<T>>`. Choose either a reactive stream (pure `Flow<T>`) or a one-shot `suspend` function that returns `NanoAIResult<T>`.
3. **Bridge at ViewModel layer** – ViewModels convert `Flow` contracts into their state (e.g., `stateIn`) and marshal `NanoAIResult` responses through the new `NanoAIErrorEnvelope` helper.

Existing modules (Image Gallery, Chat) now use these annotations. New APIs must opt into the same contract before landing.
