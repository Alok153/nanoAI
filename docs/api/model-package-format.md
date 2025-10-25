# Model Package Format

## Schema

The `ModelPackage` entity describes downloadable AI models for local inference.

```kotlin
data class ModelPackage(
  val modelId: String,
  val displayName: String,
  val version: String,
  val providerType: ProviderType,
  val deliveryType: DeliveryType,
  val minAppVersion: Int,
  val sizeBytes: Long,
  val capabilities: Set<String>,
  val installState: InstallState,
  val downloadTaskId: UUID? = null,
  val manifestUrl: String,
  val checksumSha256: String? = null,
  val signature: String? = null,
  val createdAt: Instant,
  val updatedAt: Instant,
  // Enhanced metadata for consistency with HuggingFace models
  val author: String? = null,
  val license: String? = null,
  val languages: List<String> = emptyList(),
  val baseModel: String? = null,
  val architectures: List<String> = emptyList(),
  val modelType: String? = null,
  val summary: String? = null,
  val description: String? = null,
)
```

## Enums

```kotlin
enum class ProviderType {
    MEDIA_PIPE,      // Google MediaPipe (LiteRT)
    TFLITE,          // TensorFlow Lite
    MLC_LLM,         // MLC LLM runtime
    ONNX_RUNTIME,    // ONNX Runtime
    CLOUD_API        // Cloud API reference
}

enum class DeliveryType {
    DOWNLOAD,        // Download from URL
    BUNDLED,         // Included in APK
    DYNAMIC_MODULE   // Play Feature Delivery
}

enum class InstallState {
    NOT_INSTALLED,   // Not downloaded
    DOWNLOADING,     // In progress
    INSTALLED,       // Ready to use
    FAILED           // Download failed
}
```

## Model Manifest (JSON)

Models are distributed with a `model-manifest.json` file that may vary based on provider. The following is the official manifest schema:

```json
{
  "model_id": "gemini-nano-2b",
  "display_name": "Gemini Nano 2B",
  "version": "1.0.0",
  "runtime": "MEDIA_PIPE",
  "artifact_url": "https://storage.googleapis.com/nanoai-models/gemini-nano-2b.tflite",
  "size_bytes": 2147483648,
  "checksum": {
    "algorithm": "SHA256",
    "value": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
  },
  "capabilities": ["TEXT_GEN", "IMAGE_GEN"],
  "min_ram_mb": 4096,
  "notes": ["Optimized for mobile inference", "Requires Android API 31+"]
}
```

## Example Models

Models are described with capabilities, size, and provider-specific metadata:

```json
{
  "modelId": "gemini-nano-2b",
  "displayName": "Gemini Nano 2B",
  "version": "1.0.0",
  "providerType": "MEDIA_PIPE",
  "deliveryType": "DOWNLOAD",
  "minAppVersion": 1,
  "sizeBytes": 2147483648,
  "capabilities": ["TEXT_GENERATION"],
  "installState": "NOT_INSTALLED",
  "downloadTaskId": null,
  "manifestUrl": "https://api.nanoai.app/catalog/models/gemini-nano-2b/manifest",
  "checksumSha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "signature": "MEUCIQD...",
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": "2025-01-01T00:00:00Z",
  "author": "Google",
  "license": "Apache-2.0",
  "languages": ["en", "es", "fr", "de"],
  "architectures": ["transformer"],
  "modelType": "text-generation",
  "summary": "Lightweight multimodal model for on-device inference",
  "description": "Gemini Nano 2B is optimized for mobile and edge devices..."
}
```

Other examples include Gemma 7B (7.5GB, FP16), Stable Diffusion Turbo (5GB, ONNX), and various quantization levels based on device capabilities.
