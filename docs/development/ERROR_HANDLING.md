# Error Handling Architecture

This document describes the standardized error-handling patterns across all layers of the application.

## Implementation Status: ✅ COMPLETE

All feature ViewModels now follow the unified error-handling architecture described below.

## Architecture Overview

### Single Channel Pattern
Feature ViewModels expose errors through `ViewModelStateHost.events` using typed payloads:

```kotlin
sealed interface ChatUiEvent : NanoAIViewEvent {
  data class ErrorRaised(val error: ChatError, val envelope: NanoAIErrorEnvelope) : ChatUiEvent
}
```

### NanoAIErrorEnvelope
A reusable payload carrying user copy, retry metadata, and telemetry IDs:

```kotlin
data class NanoAIErrorEnvelope(
  val userMessage: String,
  val retryAfterSeconds: Long? = null,
  val telemetryId: String? = null,
  val cause: Throwable? = null,
  val context: Map<String, String> = emptyMap(),
)
```

### Conversion Extensions
Mapping from `NanoAIResult`/`Throwable` to envelope happens in ViewModels:

```kotlin
fun NanoAIResult<*>.toErrorEnvelope(fallbackMessage: String): NanoAIErrorEnvelope
fun Throwable?.toErrorEnvelope(fallbackMessage: String): NanoAIErrorEnvelope
```

## Guardrails

1. **Single Channel** – Feature ViewModels expose errors through `ViewModelStateHost.events` using typed payloads (e.g., `FooUiEvent.ErrorRaised(envelope)`). UI collects exactly one flow for events.
2. **Structured Payloads** – `NanoAIErrorEnvelope` carries user copy, retry metadata, and telemetry IDs. Mapping from `NanoAIResult`/`Throwable` to the envelope happens in the ViewModel so UI always receives consistent data.
3. **State Reflection** – Alongside emitting an event, ViewModels update their `state` with the same user-facing message so accessibility services can read it without relying on snackbars.
4. **Fallback Consistency** – `NanoAIErrorFormatter` ensures blank or null messages degrade gracefully to opinionated copy ("Failed to send message", "Unable to delete image", etc.).

## Implemented ViewModels

All feature ViewModels follow this pattern:
- `ChatViewModel` – Chat interactions, persona switching, thread management
- `SettingsViewModel` – Preferences, backup/restore, API providers
- `ModelLibraryViewModel` – Model downloads, catalog management
- `HuggingFaceLibraryViewModel` – HuggingFace model browsing
- `ImageGenerationViewModel` – Image generation flows
- `AudioViewModel` – Audio processing
- `ShellViewModel` – Navigation, command palette, drawers
