# Phase 1 Data Model: Offline Multimodal nanoAI Assistant

## Entities

### ChatThread
- **Primary Key**: threadId (UUID)
- **Attributes**:
  - title (String?) – optional user-assigned title.
  - personaId (UUID?) – default persona when thread created.
  - activeModelId (String) – identifier of the model used for latest response (local or cloud).
  - createdAt (Instant)
  - updatedAt (Instant)
  - isArchived (Boolean, default false)
- **Relationships**:
  - One-to-many with `Message` (threadId foreign key).

### Message
- **Primary Key**: messageId (UUID)
- **Attributes**:
  - threadId (UUID)
  - role (Enum: USER, ASSISTANT, SYSTEM)
  - text (String?)
  - audioUri (String?) – placeholder for future audio clips.
  - imageUri (String?) – placeholder for future image generations.
  - source (Enum: LOCAL_MODEL, CLOUD_API)
  - latencyMs (Long?) – measured inference duration.
  - createdAt (Instant)
  - errorCode (String?) – populated when response fails.
- **Indexes**: composite on (threadId, createdAt) for ordering.

### PersonaProfile
- **Primary Key**: personaId (UUID)
- **Attributes**:
  - name (String)
  - description (String)
  - systemPrompt (Text)
  - defaultModelPreference (String?)
  - temperature (Float)
  - topP (Float)
  - defaultVoice (String?) – reserved for future audio support.
  - defaultImageStyle (String?) – reserved for future image support.
  - createdAt (Instant)
  - updatedAt (Instant)

### ModelPackage
- **Primary Key**: modelId (String)
- **Attributes**:
  - displayName (String)
  - version (String)
  - providerType (Enum: MEDIA_PIPE, TFLITE, MLC_LLM, ONNX_RUNTIME, CLOUD_API)
  - sizeBytes (Long)
  - capabilities (Set<String>) – e.g., TEXT_GEN, IMAGE_GEN, AUDIO_IN, AUDIO_OUT.
  - installState (Enum: NOT_INSTALLED, DOWNLOADING, INSTALLED, PAUSED, ERROR)
  - downloadTaskId (UUID?)
  - checksum (String?)
  - updatedAt (Instant)

### DownloadTask
- **Primary Key**: taskId (UUID)
- **Attributes**:
  - modelId (String)
  - progress (Float 0f–1f)
  - status (Enum: QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED)
  - bytesDownloaded (Long)
  - startedAt (Instant?)
  - finishedAt (Instant?)
  - errorMessage (String?)

### APIProviderConfig
- **Primary Key**: providerId (String)
- **Attributes**:
  - providerName (String)
  - baseUrl (String)
  - apiKey (Encrypted String)
  - apiType (Enum: OPENAI_COMPATIBLE, GEMINI, CUSTOM)
  - isEnabled (Boolean)
  - quotaResetAt (Instant?)
  - lastStatus (Enum: OK, UNAUTHORIZED, RATE_LIMITED, ERROR, UNKNOWN)

### PrivacyPreference
- **Primary Key**: preferenceId (Int = 1)
- **Attributes**:
  - exportWarningsDismissed (Boolean)
  - telemetryOptIn (Boolean, default false)
  - consentAcknowledgedAt (Instant)
  - retentionPolicy (Enum: INDEFINITE, MANUAL_PURGE_ONLY)

### PersonaSwitchLog
- **Primary Key**: logId (UUID)
- **Attributes**:
  - threadId (UUID)
  - previousPersonaId (UUID?)
  - newPersonaId (UUID)
  - actionTaken (Enum: CONTINUE_THREAD, START_NEW_THREAD)
  - createdAt (Instant)

## Relationships Diagram (Textual)
- `ChatThread` 1---* `Message`
- `ChatThread` 1---* `PersonaSwitchLog`
- `PersonaProfile` 1---* `ChatThread` (default persona reference only)
- `ModelPackage` 1---0..1 `DownloadTask`
- `APIProviderConfig` independent; referenced by runtime configuration.
- `PrivacyPreference` singleton table for consent + export flags.

## Notes
- All UUIDs stored as `String` in Room with converter.
- Use TypeConverters for enums and `Instant` (ISO strings).
- Future audio/image attachments will store binary references in app-specific storage; URIs resolved through repository.
