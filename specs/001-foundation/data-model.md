# Phase 1 Data Model: Offline Multimodal nanoAI Assistant

## Overview
This consolidated data model includes core entities from the foundation specification plus specialized entities from subsequent feature branches, creating a comprehensive schema for the entire nanoAI application.

## Core Chat & Inference Entities

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
  - One-to-many with `PersonaSwitchLog` (threadId foreign key).

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

### PersonaSwitchLog
- **Primary Key**: logId (UUID)
- **Attributes**:
  - threadId (UUID)
  - previousPersonaId (UUID?)
  - newPersonaId (UUID)
  - actionTaken (Enum: CONTINUE_THREAD, START_NEW_THREAD)
  - createdAt (Instant)

## Model Management Entities

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
  - manifestUrl (URI?) – for integrity verification
  - signature (String?) – Base64 signature when manifest signed
  - deliveryType (Enum: LOCAL_ARCHIVE, PLAY_ASSET, CLOUD_FALLBACK) – default LOCAL_ARCHIVE
  - minAppVersion (Int?) – versionCode required, default null
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

### DownloadManifest
- **Primary Key**: manifestId (String) – composite of modelId + version
- **Attributes**:
  - modelId (String)
  - version (String)
  - checksumSha256 (String) – 64 hex characters
  - sizeBytes (Long)
  - downloadUrl (URI)
  - expiry (Instant?) – optional TTL
  - releaseNotes (String?)

## API & Credentials Entities

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

### SecretCredential
- **Primary Key**: credentialId (String) – composite of providerId + keyAlias
- **Attributes**:
  - providerId (String)
  - keyAlias (String) – alias in EncryptedSharedPreferences
  - storedAt (Instant)
  - rotatesAfter (Instant?)
  - scope (Enum: TEXT_INFERENCE, VISION, AUDIO, EXPORT)
  - metadata (Map<String, String>) – e.g., environment, contact email

## User Preferences & Privacy

### PrivacyPreference
- **Primary Key**: preferenceId (Int = 1) – singleton
- **Attributes**:
  - exportWarningsDismissed (Boolean)
  - telemetryOptIn (Boolean, default false)
  - consentAcknowledgedAt (Instant?)
  - disclaimerShownCount (Int, default 0)
  - retentionPolicy (Enum: INDEFINITE, MANUAL_PURGE_ONLY)

### UiPreferenceSnapshot
- **Primary Key**: preferenceId (Int = 1) – singleton
- **Attributes**:
  - theme (Enum: LIGHT, DARK, SYSTEM)
  - density (Enum: COMPACT, COMFORTABLE)
  - fontScale (Float, default 1.0)
  - onboardingCompleted (Boolean, default false)
  - dismissedTooltips (Set<String>) – global store for "Don't show again"

## UI State Management Entities

### ShellLayoutState (Runtime Only)
- **Attributes**:
  - windowSizeClass (WindowSizeClass) – calculated
  - isLeftDrawerOpen (Boolean)
  - isRightDrawerOpen (Boolean)
  - activeMode (ModeId) – HOME, CHAT, IMAGE, AUDIO, CODE, TRANSLATE, HISTORY, LIBRARY, SETTINGS, TOOLS
  - showCommandPalette (Boolean)
  - connectivity (ConnectivityStatus) – ONLINE, OFFLINE, LIMITED
  - pendingUndoAction (UndoPayload?)

### RecentActivityItem (Runtime Only)
- **Attributes**:
  - id (String) – from ChatThreadEntity or ImageGenerationEntity
  - modeId (ModeId)
  - title (String)
  - timestamp (Instant)
  - status (Enum: COMPLETED, IN_PROGRESS, FAILED)

## Progress & Job Management

### ProgressJob (Runtime Only)
- **Attributes**:
  - jobId (UUID)
  - type (Enum: IMAGE_GENERATION, AUDIO_RECORDING, MODEL_DOWNLOAD)
  - status (Enum: PENDING, RUNNING, PAUSED, FAILED, COMPLETED)
  - progress (Float 0f–1f)
  - eta (Duration?)
  - canRetry (Boolean)
  - queuedAt (Instant)

### ImportJob (Transient)
- **Attributes**:
  - jobId (UUID)
  - status (Enum: PENDING, RUNNING, COMPLETED, FAILED)
  - errorMessage (String?)
  - createdAt (Instant)

## Quality Assurance & Maintenance

### RepoMaintenanceTask
- **Primary Key**: taskId (String) – UUID or deterministic slug
- **Attributes**:
  - title (String) – max 120 chars
  - description (String)
  - category (Enum: STATIC_ANALYSIS, SECURITY, TESTING, RUNTIME, DOCS)
  - priority (Enum: CRITICAL, HIGH, MEDIUM, LOW)
  - status (Enum: IDENTIFIED, IN_PROGRESS, IN_REVIEW, VERIFIED, BLOCKED)
  - owner (String?)
  - blockingRules (List<String>)
  - linkedArtifacts (List<URI>)
  - createdAt (Instant)
  - updatedAt (Instant)

### CodeQualityMetric
- **Primary Key**: metricId (String) – ruleId + file path hash
- **Attributes**:
  - ruleId (String) – Detekt rule name
  - filePath (String) – relative path
  - severity (Enum: WARNING, ERROR)
  - occurrences (Int)
  - threshold (Int)
  - firstDetectedAt (Instant)
  - resolvedAt (Instant?)
  - notes (String?)

## Test Coverage & Risk Management

### CoverageSummary
- **Primary Key**: buildId (String)
- **Attributes**:
  - timestamp (Instant)
  - layerMetrics (Map<TestLayer, CoverageMetric>)
  - thresholds (Map<TestLayer, Double>) – VM: 75, UI: 65, Data: 70
  - trendDelta (Map<TestLayer, Double>)
  - riskItems (List<RiskRegisterItemRef>)

### CoverageTrendPoint
- **Primary Key**: trendId (String) – composite buildId + layer
- **Attributes**:
  - buildId (String)
  - layer (TestLayer) – VIEW_MODEL, UI, DATA
  - coverage (Double)
  - threshold (Double)
  - recordedAt (Instant)

### TestSuiteCatalogEntry
- **Primary Key**: suiteId (String)
- **Attributes**:
  - owner (String)
  - layer (TestLayer)
  - journey (String)
  - coverageContribution (Double)
  - riskTags (Set<String>)

### RiskRegisterItem
- **Primary Key**: riskId (String)
- **Attributes**:
  - layer (TestLayer)
  - description (String)
  - severity (Enum: LOW, MEDIUM, HIGH, CRITICAL)
  - targetBuild (String?)
  - status (Enum: OPEN, IN_PROGRESS, RESOLVED, DEFERRED)
  - mitigation (String)

## Error Handling

### ErrorEnvelope (Runtime Only)
- **Attributes**:
  - code (Enum: NETWORK_UNAVAILABLE, INTEGRITY_FAILURE, AUTH_REQUIRED, OUT_OF_MEMORY, UNKNOWN)
  - message (String) – localized key
  - cause (Throwable?) – debug only
  - retryPolicy (Enum: RETRYABLE, MANUAL_RETRY, DO_NOT_RETRY)
  - telemetryId (String?)
  - timestamp (Instant)
  - context (Map<String, String>)

## Supporting Types & Enums

### ModeId
Sealed hierarchy: HOME, CHAT, IMAGE, AUDIO, CODE, TRANSLATE, HISTORY, LIBRARY, SETTINGS, TOOLS

### TestLayer
Enum: VIEW_MODEL, UI, DATA

### ConnectivityStatus
Enum: ONLINE, OFFLINE, LIMITED

### CommandAction (Runtime Only)
- label (String)
- icon (ImageVector)
- shortcut (String?)
- destination (CommandDestination)

### BackupBundle (Logical)
- personas (List<PersonaProfile>)
- apiProviders (List<APIProviderConfig>)
- settings (Map<String, Any>)
- format: JSON root object; optionally ZIP-wrapped

## Relationships Diagram (Textual)

### Core Chat Flow
```
ChatThread 1---* Message
ChatThread 1---* PersonaSwitchLog
PersonaProfile 1---* ChatThread (default reference)
```

### Model Management
```
ModelPackage 1---0..1 DownloadTask
ModelPackage 1---0..1 DownloadManifest
ModelPackage *---* SecretCredential (via providerId)
```

### API & Credentials
```
APIProviderConfig 1---* SecretCredential
APIProviderConfig independent (runtime config)
```

### Quality & Testing
```
CoverageSummary 1---* CoverageTrendPoint
CoverageSummary *---* RiskRegisterItem
TestSuiteCatalogEntry *---* RiskRegisterItem
RepoMaintenanceTask 1---* CodeQualityMetric
```

### UI State (Runtime)
```
UiPreferenceSnapshot singleton
ShellLayoutState aggregates from multiple sources
ProgressJob from WorkManager + Room flows
```

## Storage Strategy
- **Room Entities**: ChatThread, Message, PersonaProfile, ModelPackage, DownloadTask, APIProviderConfig, PersonaSwitchLog, RepoMaintenanceTask, CodeQualityMetric, DownloadManifest, CoverageSummary, CoverageTrendPoint, TestSuiteCatalogEntry, RiskRegisterItem
- **DataStore/EncryptedSharedPreferences**: PrivacyPreference, UiPreferenceSnapshot, SecretCredential
- **Runtime Only**: ShellLayoutState, ProgressJob, ErrorEnvelope, ImportJob, RecentActivityItem, CommandAction
- **Logical**: BackupBundle (transient during import/export)

## Migration Notes
- All new entities default to safe values for existing databases
- Encrypted fields use EncryptedSharedPreferences with migration helpers
- UUIDs stored as Strings with Room converters
- Enums use TypeConverters for forward compatibility
- Future audio/image attachments stored as URIs in app-specific storage

## Validation Rules
- All foreign keys cascade appropriately
- Checksum fields must be valid hex/SHA256 when present
- Encrypted fields never logged in plaintext
- Privacy data retention respects user preferences
- Coverage metrics bounded 0-100 with proper status derivation
