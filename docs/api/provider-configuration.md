# API Provider Configuration

## Schema

The `ApiProviderConfig` entity stores cloud API endpoint configurations for fallback inference.

```kotlin
data class ApiProviderConfig(
  val providerId: String,
  val providerName: String,
  val baseUrl: String,
  val apiKey: String,
  val apiType: APIType,
  val isEnabled: Boolean = true,
  val quotaResetAt: Instant? = null,
  val lastStatus: ProviderStatus = ProviderStatus.UNKNOWN,
)
```

## Enums

```kotlin
enum class APIType {
    OPENAI_COMPATIBLE,  // OpenAI-compatible API (supports /v1/completions, /v1/models endpoints)
    GEMINI,             // Google Gemini API (custom protocol)
    CUSTOM              // Custom API with proprietary protocol
}

enum class ProviderStatus {
    OK,              // Provider is operational
    UNAUTHORIZED,    // API key invalid or expired
    RATE_LIMITED,    // Rate limit exceeded
    ERROR,           // General error
    UNKNOWN          // Status not yet checked
}
```

## Example Configurations

### OpenAI

```json
{
  "providerId": "550e8400-e29b-41d4-a716-446655440000",
  "providerName": "OpenAI GPT-4",
  "baseUrl": "https://api.openai.com/v1",
  "apiKey": "sk-proj-...",
  "apiType": "OPENAI_COMPATIBLE",
  "isEnabled": true,
  "lastStatus": "OK",
  "quotaResetAt": null
}
```

### Google Gemini

```json
{
  "providerId": "550e8400-e29b-41d4-a716-446655440001",
  "providerName": "Google Gemini Pro",
  "baseUrl": "https://generativelanguage.googleapis.com/v1",
  "apiKey": "AIza...",
  "apiType": "GEMINI",
  "isEnabled": false,
  "lastStatus": "OK",
  "quotaResetAt": null
}
```

### Custom Endpoint

```json
{
  "providerId": "550e8400-e29b-41d4-a716-446655440002",
  "providerName": "Self-Hosted LLM",
  "baseUrl": "http://localhost:8000/v1",
  "apiKey": null,
  "apiType": "CUSTOM",
  "isEnabled": false,
  "lastStatus": "UNKNOWN",
  "quotaResetAt": null
}
```

## API Endpoints

Providers must support OpenAI-compatible endpoints:

### POST /v1/completions

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

### GET /v1/models

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
