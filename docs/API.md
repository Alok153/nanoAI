# API Documentation

## API Provider Configuration

### Schema

The `ApiProviderConfig` entity stores cloud API endpoint configurations for fallback inference.

```kotlin
data class ApiProviderConfig(
    val id: UUID,
    val name: String,              // Display name (e.g., "OpenAI GPT-4")
    val baseUrl: String,           // API endpoint (e.g., "https://api.openai.com/v1")
    val apiKey: String?,           // Optional API key (TODO: encrypt with Jetpack Security)
    val isDefault: Boolean,        // Whether this is the default provider
    val status: ProviderStatus,    // Current status (OK, UNAUTHORIZED, RATE_LIMITED, ERROR, UNKNOWN)
    val quotaResetAt: Instant?,    // When rate limit resets
    val createdAt: Instant         // Creation timestamp
)

enum class ProviderStatus {
    OK,              // Provider is operational
    UNAUTHORIZED,    // API key invalid or expired
    RATE_LIMITED,    // Rate limit exceeded
    ERROR,           // General error
    UNKNOWN          // Status not yet checked
}
```

### Example Configurations

#### OpenAI

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "OpenAI GPT-4",
  "baseUrl": "https://api.openai.com/v1",
  "apiKey": "sk-proj-...",
  "isDefault": true,
  "status": "OK",
  "quotaResetAt": null,
  "createdAt": "2025-10-01T10:00:00Z"
}
```

#### Google Gemini

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "name": "Google Gemini Pro",
  "baseUrl": "https://generativelanguage.googleapis.com/v1",
  "apiKey": "AIza...",
  "isDefault": false,
  "status": "OK",
  "quotaResetAt": null,
  "createdAt": "2025-10-01T10:05:00Z"
}
```

#### Custom Endpoint

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440002",
  "name": "Self-Hosted LLM",
  "baseUrl": "http://localhost:8000/v1",
  "apiKey": null,
  "isDefault": false,
  "status": "UNKNOWN",
  "quotaResetAt": null,
  "createdAt": "2025-10-01T10:10:00Z"
}
```

### API Endpoints

Providers must support OpenAI-compatible endpoints:

#### POST /v1/completions

```json
Request:
{
  "model": "gpt-4",
  "prompt": "Hello, how are you?",
  "temperature": 0.7,
  "max_tokens": 100,
  "top_p": 1.0
}

Response:
{
  "id": "cmpl-123",
  "object": "text_completion",
  "created": 1696156800,
  "model": "gpt-4",
  "choices": [
    {
      "text": "I'm doing well, thank you!",
      "index": 0,
      "finish_reason": "stop"
    }
  ]
}
```

#### GET /v1/models

```json
Response:
{
  "object": "list",
  "data": [
    {
      "id": "gpt-4",
      "object": "model",
      "created": 1696156800,
      "owned_by": "openai"
    }
  ]
}
```

---

## User Profile Metadata API

### GET /user/profile

Returns the UI personalization payload consumed by the nanoAI UI/UX feature set. The endpoint is idempotent and safe to cache; responses are merged with local Room/DataStore snapshots for offline continuity.

```http
GET /user/profile HTTP/1.1
Accept: application/json
Authorization: Bearer <token>
```

```json
Response 200
{
  "id": "user-123",
  "displayName": "Vijay",
  "themePreference": "SYSTEM",
  "visualDensity": "DEFAULT",
  "pinnedTools": ["summarize", "translator"],
  "dismissedTips": {"home_tools_tip": true},
  "savedLayouts": [
    {
      "id": "layout-01",
      "name": "Desk Setup",
      "lastOpenedScreen": "HOME",
      "pinnedTools": ["summarize"],
      "isCompact": false
    }
  ],
  "onboardingCompleted": true,
  "lastOpenedScreen": "HOME"
}
```

### Caching & Privacy

- Clients persist the response in Room (`UserProfileEntity`, `UIStateSnapshotEntity`) and DataStore (`UiPreferencesStore`) to serve offline sessions.
- Sensitive fields (display name, pinned tool IDs) remain on-device; telemetry egress is conditional on explicit consent captured in `privacy.telemetryOptIn`.
- Error responses should include 401 (revoked session), 403 (consent revoked), and 503 (maintenance). The repository wraps these into domain-level failure states surfaced to the Offline banner and Settings screen.

---

## Model Package Format

### Schema

The `ModelPackage` entity describes downloadable AI models for local inference.

```kotlin
data class ModelPackage(
    val id: UUID,
    val name: String,                      // Display name (e.g., "Gemini Nano 2B")
    val version: String,                   // Version string (e.g., "1.0.0")
    val description: String?,              // User-facing description
    val provider: ProviderType,            // Inference provider
    val capabilities: Set<ModelCapability>, // Supported features
    val sizeBytes: Long,                   // Download size
    val checksumSha256: String?,           // File integrity checksum
    val localPath: String?,                // Path after download
    val isInstalled: Boolean,              // Whether model is ready
    val downloadUrl: String?,              // Source URL
    val metadata: String                   // JSON metadata
)

enum class ProviderType {
    MEDIA_PIPE,      // Google MediaPipe (LiteRT)
    TFLITE,          // TensorFlow Lite
    MLC_LLM,         // MLC LLM runtime
    ONNX_RUNTIME,    // ONNX Runtime
    CLOUD_API        // Cloud API reference
}

enum class ModelCapability {
    TEXT_GENERATION,  // Text-to-text
    IMAGE_GENERATION, // Text-to-image
    AUDIO_INPUT,      // Speech-to-text
    AUDIO_OUTPUT,     // Text-to-speech
    MULTIMODAL        // Combined capabilities
}
```

### Model Manifest (JSON)

Models are distributed with a `model-manifest.json` file:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440003",
  "name": "Gemini Nano 2B",
  "version": "1.0.0",
  "description": "Lightweight Gemini model optimized for on-device inference",
  "provider": "MEDIA_PIPE",
  "capabilities": ["TEXT_GENERATION"],
  "sizeBytes": 2147483648,
  "checksumSha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "downloadUrl": "https://storage.googleapis.com/nanoai-models/gemini-nano-2b.tflite",
  "metadata": {
    "architecture": "Transformer",
    "parameters": "2B",
    "contextLength": 2048,
    "quantization": "INT8",
    "license": "Apache 2.0",
    "minApiLevel": 31,
    "requiredRam": "4GB",
    "avgLatency": "150ms",
    "supportedLanguages": ["en", "es", "fr", "de"],
    "loraSupport": true,
    "loraRanks": [4, 8, 16, 32]
  }
}
```

### Example Models

#### Gemini Nano (2B params)

```json
{
  "id": "gemini-nano-2b",
  "name": "Gemini Nano 2B",
  "version": "1.0.0",
  "provider": "MEDIA_PIPE",
  "capabilities": ["TEXT_GENERATION"],
  "sizeBytes": 2147483648,
  "metadata": {
    "parameters": "2B",
    "quantization": "INT8",
    "contextLength": 2048
  }
}
```

#### Gemma 7B Instruct

```json
{
  "id": "gemma-7b-instruct",
  "name": "Gemma 7B Instruct",
  "version": "1.1.0",
  "provider": "MEDIA_PIPE",
  "capabilities": ["TEXT_GENERATION"],
  "sizeBytes": 7516192768,
  "metadata": {
    "parameters": "7B",
    "quantization": "FP16",
    "contextLength": 8192
  }
}
```

#### Stable Diffusion Turbo

```json
{
  "id": "sd-turbo-512",
  "name": "Stable Diffusion Turbo",
  "version": "1.0.0",
  "provider": "ONNX_RUNTIME",
  "capabilities": ["IMAGE_GENERATION"],
  "sizeBytes": 5368709120,
  "metadata": {
    "resolution": "512x512",
    "steps": "1-4",
    "quantization": "FP16"
  }
}
```

---

## Export Backup Format

### Schema

The export backup is a JSON file containing all user data for migration/backup.

```typescript
interface ExportBackup {
  version: string;           // Export format version (e.g., "1.0")
  timestamp: string;         // ISO 8601 timestamp
  device: string;            // Device model/identifier
  conversations: Thread[];   // All chat threads
  personas: Persona[];       // All persona profiles
  apiProviders: ApiProvider[]; // All API configurations
  privacy: PrivacySettings;  // Privacy preferences
  modelCatalog: Model[];     // Installed model references
}

interface Thread {
  id: string;                // UUID
  title: string | null;
  personaId: string;         // UUID reference
  createdAt: string;
  updatedAt: string;
  isArchived: boolean;
  messages: Message[];
  switches: PersonaSwitch[];
}

interface Message {
  id: string;
  role: "user" | "assistant" | "system";
  content: string;
  timestamp: string;
  latencyMs: number | null;
  errorCode: string | null;
}

interface PersonaSwitch {
  id: string;
  fromPersonaId: string | null;
  toPersonaId: string;
  timestamp: string;
  action: "CONTINUE_THREAD" | "START_NEW_THREAD";
}

interface Persona {
  id: string;
  name: string;
  systemPrompt: string;
  temperature: number;
  topP: number;
  modelPreference: string | null;
  createdAt: string;
}

interface ApiProvider {
  id: string;
  name: string;
  baseUrl: string;
  apiKey: string | null;  // REDACTED in export by default
  isDefault: boolean;
  status: string;
  quotaResetAt: string | null;
  createdAt: string;
}

interface PrivacySettings {
  telemetryOptIn: boolean;
  retentionDays: number;
  consentTimestamp: string | null;
}

interface Model {
  id: string;
  name: string;
  version: string;
  provider: string;
  localPath: string;
}
```

### Example Export

```json
{
  "version": "1.0",
  "timestamp": "2025-10-01T15:30:00Z",
  "device": "Pixel 7 (Android 14)",
  "conversations": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440010",
      "title": "Recipe Ideas",
      "personaId": "550e8400-e29b-41d4-a716-446655440020",
      "createdAt": "2025-09-28T10:00:00Z",
      "updatedAt": "2025-09-28T10:15:00Z",
      "isArchived": false,
      "messages": [
        {
          "id": "550e8400-e29b-41d4-a716-446655440100",
          "role": "user",
          "content": "Suggest a quick dinner recipe",
          "timestamp": "2025-09-28T10:00:00Z",
          "latencyMs": null,
          "errorCode": null
        },
        {
          "id": "550e8400-e29b-41d4-a716-446655440101",
          "role": "assistant",
          "content": "Here's a 15-minute pasta primavera recipe...",
          "timestamp": "2025-09-28T10:00:05Z",
          "latencyMs": 150,
          "errorCode": null
        }
      ],
      "switches": []
    }
  ],
  "personas": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440020",
      "name": "Chef Assistant",
      "systemPrompt": "You are a helpful cooking assistant...",
      "temperature": 0.7,
      "topP": 0.9,
      "modelPreference": "gemini-nano-2b",
      "createdAt": "2025-09-25T08:00:00Z"
    }
  ],
  "apiProviders": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "OpenAI GPT-4",
      "baseUrl": "https://api.openai.com/v1",
      "apiKey": "REDACTED",
      "isDefault": true,
      "status": "OK",
      "quotaResetAt": null,
      "createdAt": "2025-09-20T12:00:00Z"
    }
  ],
  "privacy": {
    "telemetryOptIn": false,
    "retentionDays": 90,
    "consentTimestamp": "2025-09-20T12:00:00Z"
  },
  "modelCatalog": [
    {
      "id": "gemini-nano-2b",
      "name": "Gemini Nano 2B",
      "version": "1.0.0",
      "provider": "MEDIA_PIPE",
      "localPath": "/data/user/0/com.vjaykrsna.nanoai/cache/models/gemini-nano-2b.tflite"
    }
  ]
}
```

### Export Options

When exporting, users can choose:

1. **Full Export** (default)
   - All conversations and messages
   - All personas
   - API providers (keys REDACTED)
   - Privacy settings
   - Model references (not files)

2. **Minimal Export**
   - Only personas
   - Privacy settings
   - API provider configs (no keys)

3. **Conversations Only**
   - All threads and messages
   - Referenced personas
   - No API configs or models

### Security Considerations

- **API Keys**: REDACTED by default (user must manually re-enter after import)
- **Model Files**: Not included (too large; references only)
- **Encryption**: Export file is plain JSON (user can encrypt externally)
- **PII**: Contains user message content (warn before export)

### File Naming

Exports are saved with timestamp:

```
nanoai-backup-2025-10-01-153000.json
```

---

## Privacy Data Store

### Schema

Privacy preferences are stored in DataStore (key-value pairs).

```kotlin
data class PrivacyPreferences(
    val telemetryOptIn: Boolean = false,        // Default: opt-out
    val retentionDays: Int = 90,                // Message retention policy
    val consentTimestamp: Instant? = null       // When user accepted terms
)
```

### Keys

```kotlin
private val TELEMETRY_OPT_IN = booleanPreferencesKey("telemetry_opt_in")
private val RETENTION_DAYS = intPreferencesKey("retention_days")
private val CONSENT_TIMESTAMP = longPreferencesKey("consent_timestamp")
```

### Example

```json
{
  "telemetry_opt_in": false,
  "retention_days": 90,
  "consent_timestamp": 1696156800000
}
```

---

## Download Task Status

### Schema

Download tasks track model download progress.

```kotlin
data class DownloadTask(
    val id: UUID,
    val modelPackageId: UUID,
    val status: DownloadStatus,
    val progressPercent: Int,           // 0-100
    val downloadedBytes: Long,
    val totalBytes: Long,
    val workRequestId: String?,         // WorkManager UUID
    val errorMessage: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class DownloadStatus {
    PENDING,        // Queued, not started
    IN_PROGRESS,    // Actively downloading
    PAUSED,         // User paused
    COMPLETED,      // Successfully finished
    FAILED,         // Error occurred
    CANCELLED       // User cancelled
}
```

### State Transitions

```
PENDING → IN_PROGRESS → COMPLETED
            ↓              ↓
          PAUSED       FAILED
            ↓              ↓
        CANCELLED      CANCELLED
```

### Example

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440200",
  "modelPackageId": "gemini-nano-2b",
  "status": "IN_PROGRESS",
  "progressPercent": 45,
  "downloadedBytes": 966367641,
  "totalBytes": 2147483648,
  "workRequestId": "550e8400-e29b-41d4-a716-446655440300",
  "errorMessage": null,
  "createdAt": "2025-10-01T15:00:00Z",
  "updatedAt": "2025-10-01T15:05:00Z"
}
```

---

## Rate Limiting

### Retry Logic

Failed API calls use exponential backoff:

```kotlin
suspend fun retryWithBackoff(
    maxRetries: Int = 3,
    initialDelay: Long = 1000,
    maxDelay: Long = 10000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) throw e
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
}
```

### Rate Limit Headers

Track rate limits from API responses:

```kotlin
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1696156800
```

Store in `ApiProviderConfig.quotaResetAt`.

---

**For complete API contract specifications, see `/specs/001-foundation/contracts/`.**
