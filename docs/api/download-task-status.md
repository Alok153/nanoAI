# Download Task Status

## Schema

Download tasks track model download progress.

```kotlin
data class DownloadTask(
  val taskId: UUID,
  val modelId: String,
  val progress: Float = 0f,
  val status: DownloadStatus,
  val bytesDownloaded: Long = 0L,
  val startedAt: Instant? = null,
  val finishedAt: Instant? = null,
  val errorMessage: String? = null,
)
```

## Enums

```kotlin
enum class DownloadStatus {
    QUEUED,        // Download is queued and waiting to start
    DOWNLOADING,   // Download is actively in progress
    PAUSED,        // Download is paused (can be resumed)
    COMPLETED,     // Download completed successfully
    FAILED,        // Download failed with an error
    CANCELLED      // Download was cancelled by user
}
```

## State Transitions

```
QUEUED → DOWNLOADING → COMPLETED
         ↓           ↓
       PAUSED     FAILED
         ↓           ↓
      CANCELLED   CANCELLED
```

## Example

```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440200",
  "modelId": "gemini-nano-2b",
  "progress": 0.45,
  "status": "DOWNLOADING",
  "bytesDownloaded": 966367641,
  "startedAt": "2025-10-01T15:00:00Z",
  "finishedAt": null,
  "errorMessage": null
}
