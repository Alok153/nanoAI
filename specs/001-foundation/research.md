# Phase 0 Research: Offline Multimodal nanoAI Assistant

## Mobile LLM Runtime Options

### MediaPipe Generative (LiteRT)
- **Decision**: Use MediaPipe Generative (LiteRT) for initial on-device inference.
- **Rationale**: Optimized for Android with delegate support; aligns with offline, small-model goals.
- **Notes**: Keep a thin abstraction so runtime can evolve without touching UI.

### TensorFlow Lite / MLC LLM / ONNX Runtime / ExecuTorch
- **Decision**: Keep as evaluated options for future releases.
- **Rationale**: Provide flexibility for different model formats and ecosystems.
- **Action**: Design runtime interfaces to be backend-agnostic.

## Offline Data & Model Management
- Use Room for chat, messages, models, personas.
- Download manager: pause/resume, checksum verification, concurrency limit (default 1).
- Only warn about storage where necessary; avoid blocking flows without reason.

## Cloud API Integrations
- Support OpenAI-compatible and Gemini endpoints via Retrofit + Kotlin Serialization.
- No dedicated mobile SDK coupling; talk directly to HTTP APIs.
- Allow user-defined endpoints (OpenAI-compatible) with encrypted API keys.

## UX & Accessibility
- Compose Material 3 with sidebar shell and command palette.
- Adaptive layouts for phones/tablets.
- Accessibility: TalkBack labels, dynamic type, contrast.
- Offline-first UX: banners, queued actions, clear distinction between local/cloud responses.

## Performance & Observability
- Budgets: cold start <1.5s, local response ~2s median, low jank.
- Macrobenchmarks for startup and key flows.
- Local-only logs; provide optional export for debugging.

## Code Quality & Security
- Formatting: Spotless with Kotlin style; Detekt for static analysis (see `config/quality`).
- Secrets: store via encrypted mechanisms; no plaintext secrets in repo or logs.
- Downloads: verify integrity where manifests/checksums exist.

## Test Coverage & CI
- Coverage targets: ViewModel ≥75%, UI ≥65%, Data ≥70%.
- CI: `./gradlew spotlessCheck detekt testDebugUnitTest verifyCoverageThresholds`.
- Contract tests validate OpenAPI and schemas in this directory.

These findings shape the implementation details in `plan.md` and the entities in `data-model.md`.
