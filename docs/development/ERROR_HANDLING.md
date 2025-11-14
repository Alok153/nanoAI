# Error Handling Alignment Plan

This note captures the current gaps called out in `Personal/issues.md` and the plan for a cohesive error-handling story across layers.

## Observed Patterns

- **ViewModels** mix three approaches: `MutableSharedFlow` of exceptions, `state.pendingErrorMessage` without events, and `ViewModelStateHost` + `NanoAIViewEvent` combinations. Consumers have to remember which surface to observe.
- **UseCases / repositories** return `NanoAIResult` but many sites immediately unwrap with ad-hoc `result.message.ifBlank { ... }` logic, so telemetry context and retry hints are often dropped.
- **UI** surfaces subscribe to bespoke flows (snackbar collectors, `LaunchedEffect` blocks per error type) resulting in duplicated plumbing and inconsistent accessibility messages.

## Guardrails Moving Forward

1. **Single Channel** – Feature ViewModels expose errors through `ViewModelStateHost.events` using typed payloads (e.g., `FooUiEvent.ErrorRaised(envelope)`). UI collects exactly one flow for events.
2. **Structured Payloads** – Introduce a reusable `NanoAIErrorEnvelope` that carries user copy, retry metadata, and telemetry IDs. Mapping from `NanoAIResult`/`Throwable` to the envelope happens in the ViewModel so UI always receives consistent data.
3. **State Reflection** – Alongside emitting an event, ViewModels update their `state` with the same user-facing message so accessibility services can read it without relying on snackbars.
4. **Fallback Consistency** – Shared formatter ensures blank or null messages degrade gracefully to opinionated copy ("Failed to send message", "Unable to delete image", etc.).

These guidelines will be enforced incrementally starting with the Image, Audio, and Chat feature surfaces touched in this refactor. Subsequent passes can expand the envelope adoption to remaining ViewModels and UseCases.
