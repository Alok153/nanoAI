# Phase 0 Research: Offline Multimodal nanoAI Assistant

## Mobile LLM Runtime Options

### MediaPipe Generative (LiteRT)
- **Decision**: Launch prototype with MediaPipe Generative (LiteRT) runtime for on-device inference.
- **Rationale**: Provides Google-supported optimized graph execution on Android with GPU/NNAPI delegates, plus built-in pipelines for multimodal inputs, aligning with FR-030.
- **Considerations**: Requires model conversion to LiteRT compatible format; good coverage for text/audio embeddings, roadmap for vision. Supports delegate fallback.
- **Latest Updates**: v0.10.14 (May 13, 2024) exposes LoRA rank controls, updates GenAI documentation, and improves calculator stability, so we must track API shifts when wiring adapters.[^1]
- **Risks**: Limited community documentation; need to track API updates. Establish abstraction layer to swap in alternatives.

### TensorFlow Lite + GPU/NNAPI delegates
- **Decision**: Evaluate for follow-up release once prototype stable.
- **Rationale**: Mature tooling, quantization support, broad hardware coverage, integrates with existing Android ML infra.
- **Research Tasks**:
  - Compare performance of 4-bit/8-bit quantized LLMs on 4 GB RAM devices.
  - Assess compatibility with persona-controlled prompt templates.

### MLC LLM (TVM-based)
- **Decision**: Investigate for modular packaging of community models.
- **Rationale**: Offers cross-platform runtime with precompiled artifacts, supports Vulkan-backed acceleration.
- **Implementation Notes**: Android SDK guide (rev. 2025) requires NDK 27.0.11718014, a physical device (no emulators), and supports bundling quantized weights in-app via `bundle_weight: true`, fitting our offline model library goals.[^3]
- **Risks**: Larger binary sizes; evaluate license constraints.

### ONNX Runtime Mobile / ExecuTorch
- **Decision**: Track as future integrations for Microsoft/Meta ecosystems.
- **Rationale**: ONNX Runtime 1.18.0 deprecates the lightweight `onnxruntime-mobile` packages in favor of full `onnxruntime-android`, brings 4-bit quant gains, and adds Android QNN acceleration plus GenAI model coverage.[^2]
- **Action**: Keep plugin architecture runtime-agnostic so new delegates can register without UI changes.

## Offline Data & Model Management
- Room chosen for chat, message, model metadata, personas (aligns with spec requirement).
- Download manager abstraction must support pause/resume, checksum verification, concurrency limits (default 1).
- Storage budgeting: warn only via help dialog; track free space before download for cancellation.

## Cloud API Integrations
- Support OpenAI and Gemini via Retrofit + Kotlin Serialization.
- Uncensored default: skip moderation filters, but include logging for failures and API quota statuses.
- Configurable generic OpenAI-compatible endpoints: allow user to define base URL + API key + model list.
- Track Google’s deprecation of the standalone Gemini Android SDK in favor of the unified Firebase/Vertex AI SDK path so the fallback client keeps parity with Gemini 2.x endpoints.[^4]

## UX & Accessibility (Foundation + Consolidated)
- Compose Material 3 UI with persistent left sidebar, command palette (Ctrl+K/Cmd+K), and adaptive layouts (compact/phone, regular/tablet, expanded/desktop).
- Home hub with mode grid cards (Chat, Image, Audio, Code, Translate) for quick access within two interactions.
- Accessibility: TalkBack labels, dynamic type scaling (0.8x-2.0x), high contrast mode, focus management, and 100ms tactile feedback requirement.
- Offline-first messaging: cached content display, unavailable feature banners, action queuing for sync, and visual differentiation between local/cloud responses.
- Error handling: clear actionable messaging, inline remedies, undo support for safe operations, and standardized error envelopes.
- Onboarding: single-screen mode introduction with skip option and persistent help access.
- Contextual help: dismissible tooltips with "Don't show again" options and re-openable from Help menu.

## Performance & Observability
- Performance budgets per constitution: cold start <1.5s, local response <2s median, FMP ≤300ms, interaction latency ≤100ms, dropped frames <5%.
- Progressive loading skeletons within 150ms for network content.
- Instrument macrobenchmark tests for cold start, scrolling stability, and baseline profile generation.
- Logging stays local; use structured files for debugging export with privacy preservation.

## Code Quality & Stabilization
- **Static Analysis**: ktlint for Kotlin formatting, Detekt for code quality with blocking rules (TooManyFunctions, LongMethod, CyclomaticComplexMethod, LongParameterList).
- **Security**: EncryptedSharedPreferences for API keys and credentials; SHA256 checksum verification for model downloads.
- **Error Handling**: Standardized Result types and error envelopes with retry policies and context preservation.
- **Refactoring**: Break down large composables (NavigationScaffold, HomeScreen) and god-classes into smaller, testable units.
- **Maintenance Tracking**: RepoMaintenanceTask system for tracking stabilization work with priority levels and blocking rules.

## Test Coverage & Quality Assurance
- **Coverage Targets**: ViewModel ≥75%, UI ≥65%, Data ≥70% with automated CI enforcement.
- **Coverage Reporting**: Post-CI summaries with trend analysis, risk flagging, and historical data retention.
- **Risk Register**: Track untested scenarios with severity levels (Critical/High/Medium/Low) and mitigation plans.
- **Test Infrastructure**: Automated verification for critical flows, contract tests, and accessibility validation.
- **CI Gates**: ktlint, Detekt, Android Lint, unit tests, instrumented tests, coverage verification, and macrobenchmarks.

## References
[^1]: MediaPipe v0.10.14 release notes (May 13, 2024) – https://github.com/google-ai-edge/mediapipe/releases/tag/v0.10.14
[^2]: ONNX Runtime v1.18.0 release notes (May 21, 2024) – https://github.com/microsoft/onnxruntime/releases/tag/v1.18.0
[^3]: MLC LLM Android SDK documentation (accessed Sep 30, 2025) – https://llm.mlc.ai/docs/deploy/android
[^4]: Google Generative AI Android SDK README (deprecated in favor of unified Firebase SDK) – https://raw.githubusercontent.com/google/generative-ai-android/main/README.md

## Open Questions Addressed
- Export archives remain unencrypted; warn user during export.
- Personas prompt to merge/split thread with configurable default.
- Audio output deferred; plan future TTS integration leveraging existing architecture.
- Image generation deferred; roadmap to evaluate Stable Diffusion (Lite), Automatic1111 API, ComfyUI bridges once baseline stabilized.
