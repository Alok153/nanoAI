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

## UX & Accessibility
- Compose UI with sidebar navigation (Navigation Drawer + adaptive layout for tablets).
- Accessibility: ensure TalkBack labels, dynamic type scaling, focus order, optional vibration feedback.
- Offline-first messaging: differentiate local vs cloud responses visually; show status chips for queued online requests.

## Performance & Observability
- Performance budgets per constitution: cold start <1.5s, response <2s median, dropped frames <5%.
- Instrument macrobenchmark tests for cold start and scrolling.
- Logging stays local; use structured files for debugging export.

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
