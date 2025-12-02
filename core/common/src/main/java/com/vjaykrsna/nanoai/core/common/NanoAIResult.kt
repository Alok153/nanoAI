package com.vjaykrsna.nanoai.core.common

/**
 * Unified success/error result contract that propagates telemetry context and retry guidance
 * through repository → use case → UI layers.
 */
sealed class NanoAIResult<out T> {
  /** Successful outcome optionally carrying a payload. */
  data class Success<T>(val value: T, val metadata: Map<String, String> = emptyMap()) :
    NanoAIResult<T>()

  /** Recoverable error that surfaces retry guidance and telemetry correlation identifiers. */
  data class RecoverableError(
    val message: String,
    val retryAfterSeconds: Long?,
    val telemetryId: String?,
    val cause: Throwable? = null,
    val context: Map<String, String> = emptyMap(),
  ) : NanoAIResult<Nothing>()

  /** Fatal error requiring escalation with support contact metadata. */
  data class FatalError(
    val message: String,
    val supportContact: String?,
    val telemetryId: String? = null,
    val cause: Throwable? = null,
    val context: Map<String, String> = emptyMap(),
  ) : NanoAIResult<Nothing>()

  companion object {
    fun success(): Success<Unit> = Success(Unit)

    fun <T> success(value: T): Success<T> = Success(value)

    fun recoverable(
      message: String,
      retryAfterSeconds: Long? = null,
      telemetryId: String? = null,
      cause: Throwable? = null,
      context: Map<String, String> = emptyMap(),
    ): RecoverableError = RecoverableError(message, retryAfterSeconds, telemetryId, cause, context)

    fun fatal(
      message: String,
      supportContact: String?,
      telemetryId: String? = null,
      cause: Throwable? = null,
      context: Map<String, String> = emptyMap(),
    ): FatalError = FatalError(message, supportContact, telemetryId, cause, context)
  }
}
