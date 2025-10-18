# Feature Specification: Offline Multimodal nanoAI Assistant

**Feature Branch**: `001-foundation`  
**Created**: 2025-09-30  
**Status**: Draft  
**Input**: See `overview.md` for detailed user requirements and design vision

## User Scenarios & Testing

### Primary User Story
A privacy-conscious mobile professional installs nanoAI, downloads an on-device model, and chats with the assistant using text, voice, or images while reviewing past conversations from a sidebar history.

### Acceptance Scenarios
1. **Given** the user has opened the chat view, **When** they send a text prompt, **Then** the assistant returns a response within 2 seconds using the selected local or cloud model and displays it in the conversation thread.
2. **Given** the user opens the model library, **When** they tap "Download" on an available on-device model, **Then** the app shows progress with pause/resume options and adds the model to the usable list upon completion.
3. **Given** the device is offline and a local model is available, **When** the user submits an audio prompt, **Then** the app processes it locally, returns the configured response (text or audio), and surfaces a notice that cloud APIs are unavailable.
4. **Given** the user opens the settings export screen, **When** they trigger the universal export, **Then** the app generates a single unencrypted archive containing personas, credentials, and configuration with a clear warning for safekeeping.
5. **Given** the user submits a cloud request while offline, **When** connectivity resumes, **Then** the app automatically syncs the pending request and notifies the user of completion.
6. **Given** it's the user's first app launch, **When** they open the chat view, **Then** a disclaimer dialog appears explaining content responsibility and lack of automated filters.
7. **Given** the user views a model in the library, **When** they tap for details, **Then** contextual help displays recommended RAM/storage requirements.

### Edge Cases
- Device storage is insufficient for the selected model download.
- Cloud API keys are invalid or rate-limited while a session is active.
- Long-running multimodal responses (e.g., image generation) exceed performance budgets and need graceful timeout handling.
- User switches personas mid-conversation and expects the state to align with new persona settings.
- Accessibility users rely on TalkBack or captions when interacting with audio/image content.
- Users produce uncensored content; app must warn and allow manual moderation.
- User device has <4 GB RAM; offline inference may be slow or fail gracefully with suggestions to use cloud APIs.
- User initiates multiple model downloads; queued transfers and configurable concurrency must behave predictably.
- Active personas reference a model being deleted; running sessions should stop and allow model removal without corruption.
- Power users enable two concurrent models (e.g., text + image) and expect stable resource allocation.
- Pending cloud requests fail to sync after reconnection; user receives error notification with retry.
- First-launch disclaimer is dismissed; it does not reappear on future launches.
- Model sizing recommendations exceed device capabilities; user is advised to use cloud alternatives.

---

## Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: App MUST let users conduct multimodal chat sessions (text input/output at launch, scalable to image/audio modalities) with Material-compliant feedback within 100ms for touch responses.
- **FR-002**: App MUST persist chat history, selected model, and persona configurations using encrypted storage so state restores after process death.
- **FR-003**: App MUST surface accessibility affordances (TalkBack labels, dynamic type) for every interactive element and prepare copy/UX hooks for future audio captioning.
- **FR-004**: App MUST support offline inference by executing on-device models when no network is available and sync pending cloud requests once connectivity resumes.
- **FR-005**: App MUST enforce runtime microphone, camera, and storage permissions with explicit consent messaging before capturing or storing user data, using Android keystore-backed encryption for sensitive stored data.
- **FR-006**: App MUST provide a model library UI with search, size metadata, download progress, pause/resume, and delete controls, supporting user-configurable download concurrency (default 1, max 3).
- **FR-007**: App MUST allow users to configure multiple personas (tone, instructions, default model/API preference) and switch between them within two taps from the sidebar.
- **FR-008**: App MUST integrate cloud APIs (OpenAI, Gemini, and user-configured endpoints) with per-provider authentication and model selection.
- **FR-009**: App MUST offer quick toggles in the sidebar for switching between local/cloud models, muting future audio output, reserving space for image generation controls (future), and clearing conversation context. Note: Advanced users may enable concurrent models via settings.
- **FR-010**: App SHOULD meet reasonable performance budgets: cold start < 2s on reference device, local response < 3s median, and maintain <10% dropped frames during rendering.
- **FR-011**: App MUST track and display API usage status (quota, failures) and notify the user when limits are reached, with automatic quota reset tracking synced from provider APIs.
- **FR-012**: App MUST provide a setup flow to validate external API configurations and fail gracefully with actionable error messages.
- **FR-013**: App MUST enable exporting/importing personas and model configurations for backup or migration via a universal export/import flow, providing clear warnings that the generated archive is unencrypted and should be stored securely by the user.
- **FR-014**: App MUST log consent choices and data retention preferences in an encrypted privacy dashboard accessible from settings.
- **FR-015**: App MUST document user-facing fallback behavior when requested modality is unsupported by the selected model.
- **FR-016**: App MUST operate without authentication; all features remain available in a local-first experience without user accounts.
- **FR-017**: App MUST retain chat transcripts indefinitely until the user explicitly deletes a thread or clears history.
- **FR-018**: App MUST provide clear disclosure that no automated safety filters are applied to prompts or responses; users manage moderation manually, with audio output deferred to a later release.
- **FR-019**: App MUST present a first-launch message reminding users they are responsible for generated content without blocking access.
- **FR-020**: App MUST surface a contextual “model sizing help” action explaining recommended RAM/storage requirements for third-party models, without enforcing download limits.
- **FR-021**: App MUST store third-party API credentials locally using Android keystore-backed encryption and include them in the universal export bundle; no cloud backup occurs.
- **FR-022**: App MUST allow model downloads up to at least 3 GB and provide live progress with pause/resume controls without additional storage warnings, with user-configurable concurrency limits (default 1, max 3).
- **FR-023**: App MUST default to a single concurrent model download, queue additional requests, and allow users to raise the limit (e.g., up to 3) via settings.
- **FR-024**: App MUST restrict active inference to one model at a time by default and offer an advanced setting to enable two concurrent models with clear performance caveats (future calling/voice features to build atop this toggle).
- **FR-025**: App MUST stop active inference gracefully and remove assets when a user deletes an in-use local model, notifying them that the session ended.
- **FR-026**: App MUST operate with English-only UI copy while structuring content for future localization.
- **FR-027**: App MUST avoid collecting analytics or telemetry; only local logs necessary for debugging remain on-device.
- **FR-028**: App MUST provide a manual export option for chat transcripts so users can back up or share conversations without automated multi-device sync.
- **FR-029**: Personas must remain model-agnostic; selecting a persona cannot auto-lock a model, though personas may suggest defaults.
- **FR-030**: Offline inference MUST launch with MediaPipe Generative (LiteRT) runtime support, while the research backlog tracks TensorFlow Lite, MLC LLM, and ONNX Runtime Mobile for successive integrations.
- **FR-031**: Image generation is deferred at launch; the roadmap MUST evaluate on-device Stable Diffusion-class models and external services (e.g., Automatic1111, ComfyUI, OpenAI, Gemini) for phased integration once the text-only prototype is stable.
- **FR-032**: When users change personas mid-thread, the app MUST prompt whether to continue in the same chat or split into a new thread, following a user-configurable default stored in settings.
- **FR-033**: App MUST resume pending cloud requests once network connectivity is restored and display sync status to the user.
- **FR-034**: App MUST display a first-launch disclaimer dialog reminding users they are responsible for generated content without automated safety filters.
- **FR-035**: App MUST provide contextual help for model sizing requirements, including recommended RAM/storage for each model in the library UI.
- **FR-036**: App MUST gracefully handle model deletion by stopping active inferences, notifying users of session interruptions, and preventing data corruption.
- **FR-037**: App MUST include manual content moderation warnings in the chat UI for responses that may be uncensored or inappropriate.

### First-Launch Disclaimer & Data Management (002-disclaimer-and-fixes)
- **FR-038**: The product MUST display a first-launch disclaimer dialog reminding users they are responsible for generated content, with an acknowledge action that records consent and prevents unnecessary repetition.
- **FR-039**: The product MUST support importing/exporting personas, API configurations, and settings via a documented JSON schema (ZIP optional where justified). Backups MUST be accompanied by a clear user-facing warning if they are stored unencrypted.
- **FR-040**: The product MUST provide UI controls for switching between local and cloud inference modes and for clearing conversation context; the controls MUST persist user preference.
- **FR-041**: The product MUST enforce automated quality gates: static analysis and test targets defined in the feature plan must run in CI and pass before merge; the plan must list the exact commands that constitute the gate.
- **FR-042**: The product MUST follow Material design accessibility expectations for any new UI elements (labels, semantics, and contrast). Performance-related guidance should be documented in the plan when measurable targets are required.

### UI/UX Polish & Navigation (003-UI-UX)
- **FR-043**: App MUST present a home hub as the central entry point, featuring a grid of mode cards (Chat, Image Generation, Audio Processing, Code Assistance, Translation) with clear icons and labels for quick access to any mode within two interactions.
- **FR-044**: Primary navigation MUST use a left-side persistent sidebar (collapsible on mobile) with sections for Home, History, Library, Tools, and Settings, ensuring consistent and discoverable navigation across all modes.
- **FR-045**: Interface MUST maintain visual consistency with a documented design system including reusable components (Button, Card, ListItem, etc.), neutral color palette, generous whitespace, and subtle motion to convey polish without clutter.
- **FR-046**: App MUST support light and dark themes with manual toggle and system sync, providing fully specified color tokens for backgrounds, surfaces, text, and accents, with instantaneous theme switches and contrast compliance.
- **FR-047**: Performance MUST meet targets: First Meaningful Paint ≤ 300ms on mid-range devices, perceived interaction latency ≤ 100ms, progressive loading skeletons within 150ms for network content, and automated performance smoke tests validating these metrics.
- **FR-048**: Offline UX MUST show cached content where available, disable unavailable features with informative messaging, queue user actions for sync, and provide graceful banners for connectivity status.
- **FR-049**: Settings MUST be organized into logical sections (General, Appearance, Privacy, etc.) with concise labels, inline help text, and persistent Save/Undo for destructive changes.
- **FR-050**: Error handling MUST display clear, actionable messaging for failures (e.g., connectivity issues), with inline remedies and optional undo for safe operations.
- **FR-051**: Onboarding MUST include a minimal single-screen highlight of primary modes with a clear CTA, unobtrusive skip control, and persistent Help to re-open onboarding.
- **FR-052**: Contextual help MUST provide lightweight, dismissible tooltips for discoverable features, with 'Don't show again' options and re-openable from Help menu.
- **FR-053**: Layout MUST adapt to screen size classes (compact/phone, regular/tablet, expanded/desktop) with explicit spacing and column rules to ensure usability across devices.

### Code Quality & Stabilization (004-fixes-and-inconsistencies)
- **FR-054**: The codebase MUST be brought to a state where CI's ktlint and Detekt checks pass for the rules marked as blocking in `config/detekt/detekt.yml`. Specifically: no remaining offenses that exceed the configured thresholds for TooManyFunctions, LongMethod, CyclomaticComplexMethod, or LongParameterList for production code. All naming and format violations affecting readability should be fixed.
- **FR-055**: Implement or provide a documented and tested alternative for critical TODOs identified in `docs/inconsistencies.md` before Phase 4. This includes at minimum: local inference runtime placeholder in `MediaPipeLocalModelRuntime.kt`, model download checksum verification in `ModelDownloadWorker.kt`, and secure storage of provider/API keys (see FR-056).
- **FR-056**: All API keys, provider credentials, and similar secrets MUST be stored encrypted at rest (Jetpack Security / EncryptedSharedPreferences or keystore-backed solution). No plaintext secrets in source, test fixtures, or CI logs. Provide migration and key-rotation notes if applicable.
- **FR-057**: Model downloads MUST be validated using an authentic manifest and checksum (e.g., SHA256) stored in the package or retrieved from a signed catalog. The download worker must verify integrity before installation and expose retry/backoff behavior on failure.
- **FR-058**: Critical user flows listed in `docs/inconsistencies.md` and `docs/todo-next.md` (offline persona flow, disclaimer dialog, model library flow, cloud fallback) MUST have deterministic tests (unit or instrumented) that run in CI. Any remaining manual/integration tests must be documented with a tracking ticket.
- **FR-059**: Large composables and classes flagged (e.g., `NavigationScaffold`, `HomeScreen`, `UserProfileRepository`) MUST be refactored into smaller units with preserved behavior and covered by tests. Public APIs should remain stable or have migration notes.
- **FR-060**: Standardize error handling across domain layers using sealed result types or a Result wrapper. No unchecked exceptions should bubble to the UI in normal error scenarios.
- **FR-061**: Remove or document dead/unused code (e.g., legacy onboarding placeholders, unused `savedStateHandle` instances) and remove hardcoded URLs or secrets. Each removal must include a short rationale in the commit message.
- **FR-062**: Update `docs/inconsistencies.md` and `docs/todo-next.md` to reflect completed fixes and add explicit tracking issues for deferred items. Add a changelog entry for this stabilization pass.
- **FR-063**: For any runtime changes (local inference), measure and document median P95 response times on representative hardware; local response should target <2s per prior spec, or the spec must include a justification and fallback strategy.

### Test Coverage & Quality Assurance (005-improve-test-coverage)
- **FR-064**: The program MUST publish a consolidated coverage summary for ViewModel, UI, and data layers after every CI build so stakeholders can assess readiness.
- **FR-065**: Automated verification MUST exist for all critical ViewModel state transitions (happy path, error, loading) referenced in docs/todo-next.md, ensuring regressions are detected before release.
- **FR-066**: Critical Compose UI flows (conversation list, chat detail, message composition) MUST have automated scenarios that validate user-facing behavior, accessibility, and Material design compliance.
- **FR-067**: Data access paths (Room DAOs, repositories, caching rules) MUST have automated checks that confirm read/write integrity, error propagation, and offline resilience promises.
- **FR-068**: Coverage reporting MUST surface trend data and highlight areas below the target threshold so leadership can prioritize subsequent hardening work.
- **FR-069**: Coverage targets MUST meet or exceed ViewModel 75%, UI 65%, and Data 70%.

### Key Entities *(consolidated from all branches)*
#### Core Chat & Inference Entities
- **ChatThread**: Conversation session with messages, persona, and model associations
- **Message**: Content payloads with modality metadata and generation source tracking
- **PersonaProfile**: Persona configurations with system prompts and model preferences
- **PersonaSwitchLog**: Audit trail of persona changes within conversations

#### Model Management Entities
- **ModelPackage**: Downloadable AI models with integrity verification and metadata
- **DownloadTask**: Transfer progress tracking with pause/resume capabilities
- **DownloadManifest**: Signed manifests for model integrity verification
- **SecretCredential**: Encrypted storage for API keys and provider credentials

#### User Preferences & Privacy
- **PrivacyPreference**: Consent tracking and data retention preferences
- **UiPreferenceSnapshot**: Theme, density, and accessibility settings

#### Quality Assurance & Maintenance
- **RepoMaintenanceTask**: Stabilization work tracking with priority levels
- **CodeQualityMetric**: Static analysis violation monitoring and trends
- **CoverageSummary**: Test coverage reporting with layer-specific metrics
- **CoverageTrendPoint**: Historical coverage data for trend analysis
- **TestSuiteCatalogEntry**: Test suite inventory with coverage contributions
- **RiskRegisterItem**: Unmitigated coverage gaps with mitigation plans

#### Runtime State Management
- **ShellLayoutState**: UI navigation and connectivity state (runtime only)
- **ProgressJob**: Background task progress tracking (runtime only)
- **RecentActivityItem**: Recent conversation and generation activity (runtime only)
- **ErrorEnvelope**: Standardized error payloads with retry policies (runtime only)
- **ImportJob**: Import operation progress tracking (transient)

---
*Align with Constitution v1.0.0 (see `.specify/memory/constitution.md` for principles)*
---
