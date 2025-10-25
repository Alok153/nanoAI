# Rate Limiting

## Retry Logic

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

## Rate Limit Headers

Track rate limits from API responses:

```kotlin
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1696156800
```

Store in `ApiProviderConfig.quotaResetAt`.
