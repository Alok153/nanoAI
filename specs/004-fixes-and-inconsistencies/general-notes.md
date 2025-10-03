# General Notes: Architecture & Best Practices

## Overall Architecture
- **Clean Architecture**: Follows MVVM with strict layering: domain (use cases, models), data (repositories, DAOs), presentation (VMs, Composables). Entities are domain-focused; UI models derive from them.
- **Dependency Injection**: Hilt for singletons (e.g., databases, runtimes); qualifiers for dispatchers (e.g., `@IoDispatcher`). Modules are focused (e.g., `DatabaseModule`, `NetworkModule`).
- **Offline-First**: Room for local persistence; WorkManager for background sync/downloads. Connectivity checked via `ConnectivityStatusProvider`.
- **State Management**: `StateFlow` for UI state; combine flows in VMs for derived state. Avoid mutable state in Composables.
- **Error Handling**: Sealed classes (e.g., `InferenceResult`, `ChatError`) for domain errors; `Result<T>` for suspend funcs. VMs emit errors via shared flows for Snackbar display.

## Key Strengths
- **Modularity**: Features isolated (e.g., `feature/chat`, `feature/library`); easy to add multimodal via new use cases.
- **Privacy-Focused**: No telemetry; local encryption for creds (EncryptedSharedPreferences); unencrypted exports with warnings.
- **Testability**: Contract tests for APIs; unit tests for use cases/VMs; UI tests with Compose semantics.
- **Material 3 Compliance**: Dynamic colors, adaptive layouts; semantics for accessibility.

## Best Practices Observed
- **Coroutines**: Structured concurrency in VMs; dispatchers for IO/background.
- **Compose**: Reusable components (e.g., `PrimaryActionCard`); remember states; avoid side effects in Composables.
- **Performance**: Lazy loading in lists; baseline profiles planned; model selection favors local first.
- **Security**: API keys in keystore; input validation in models (e.g., `sanitizePinnedTools`).
- **Internationalization**: English-only UI; structured for future localization.

## Recommendations
- **Refactor Bloat**: Split large classes (e.g., `UserProfileRepository`); use delegation.
- **Enhance Testing**: Add integration tests for offline flows; mock runtimes.
- **Monitoring**: Instrument Timber for local logs; prepare for Crashlytics (opt-in).
- **Scalability**: Abstract runtimes fully; prepare for model sharding in low-RAM scenarios.

This foundation supports ambitious goals: multimodal, cross-platform. Focus next on deferred features per specs.

Last Updated: 2025-10-03
