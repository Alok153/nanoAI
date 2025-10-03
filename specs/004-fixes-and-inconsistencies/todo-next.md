# Next Development Phase: Post-MVP Enhancements

Based on codebase analysis and specs (001-foundation, 002-disclaimer-and-fixes, 003-UI-UX), the MVP provides a solid text-based chat foundation with offline inference, model library, and basic UI/UX. The next phase focuses on multimodal expansion, polish, and scalability. Prioritize by impact: core functionality first, then UX refinements.

## Phase 4: Multimodal & Advanced Features (High Priority)
- [ ] Integrate image input/output: Add camera/gallery picker in chat; support vision models (e.g., MediaPipe Vision) for local processing; fallback to cloud (Gemini/OpenAI Vision APIs). Update `Message` model with `imageUri` handling and UI rendering.
- [ ] Enable audio I/O: Implement speech-to-text (on-device via MediaPipe or cloud) and TTS output (Android TTS or ElevenLabs API). Add microphone permission flow and audio message playback in `ChatScreen`.
- [ ] Expand runtimes: Integrate TensorFlow Lite as secondary local option; evaluate MLC LLM/ONNX for broader model support. Abstract via `LocalModelRuntime` interface.
- [ ] Add image generation: Research on-device Stable Diffusion (via ExecuTorch); integrate cloud options (DALL-E, Imagen). Add generation UI in sidebar/tools panel.
- [ ] Implement concurrent models: Allow 2+ active inferences (e.g., text + vision); add resource monitoring to prevent OOM.

## Phase 5: UX Polish & Accessibility (Medium Priority)
- [ ] Enhance onboarding: Multi-step flow with interactive demos (per 003-UI-UX spec); add video/gif placeholders for multimodal features.
- [ ] Accessibility audit: Full WCAG 2.1 AA compliance; add screen reader support for dynamic content (e.g., chat bubbles, model cards); test with TalkBack/VoiceOver.
- [ ] Theme & density refinements: Support expanded visual density; add custom accent color picker. Ensure dynamic theming works on Android 12+.
- [ ] Sidebar enhancements: Add badges for unread threads; support drag-to-reorder pinned tools; collapsible sections for personas/models.
- [ ] Error & offline UX: Progressive enhancement for queued actions; add retry animations and detailed offline banners with estimated sync time.

## Phase 6: Testing & Quality (High Priority)
- [ ] Fix failing tests: Address incomplete implementations (e.g., `HomeScreenContractTest.kt`, `PersonaOfflineFlowTest.kt`); achieve 80% coverage.
- [ ] Add end-to-end tests: Espresso/Compose UI tests for full flows (onboarding → chat → export); include offline scenarios.
- [ ] Performance optimization: Integrate Baseline Profiles; target <2s local response, <300ms FMP. Add RUM metrics for prod monitoring.
- [ ] Security audit: Encrypt exports optionally; validate imports against schema; add credential masking in logs/UI.

## Phase 7: Scalability & Integrations (Low Priority)
- [ ] Multi-device sync: Add optional Firebase/own server for cross-device history (with E2E encryption).
- [ ] Community models: Expand library with user-submitted models; add validation workflow.
- [ ] Analytics (opt-in): Add privacy-safe local metrics for crashes/performance; no PII.

## Risks & Dependencies
- Multimodal: Hardware variance (e.g., low-RAM devices); test on Pixel 4a+.
- Testing: Flaky offline tests; use emulators with mocked connectivity.
- Timeline: 4-6 weeks for Phase 4; defer Phase 7 until MVP stable.

Estimated Effort: 8-12 weeks total. Track via tasks.md updates.

Last Updated: 2025-10-03
