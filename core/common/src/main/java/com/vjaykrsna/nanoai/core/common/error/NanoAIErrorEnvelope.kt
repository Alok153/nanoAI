package com.vjaykrsna.nanoai.core.common.error

import com.vjaykrsna.nanoai.core.common.NanoAIResult

/** Canonical user-facing error payload propagated from repositories → use cases → UI layers. */
data class NanoAIErrorEnvelope(
  val userMessage: String,
  val retryAfterSeconds: Long? = null,
  val telemetryId: String? = null,
  val cause: Throwable? = null,
  val context: Map<String, String> = emptyMap(),
) {
  val isRetryable: Boolean = retryAfterSeconds != null
}

object NanoAIErrorFormatter {
  fun fromResult(result: NanoAIResult<*>, fallbackMessage: String): NanoAIErrorEnvelope =
    when (result) {
      is NanoAIResult.Success -> NanoAIErrorEnvelope(fallbackMessage)
      is NanoAIResult.RecoverableError ->
        NanoAIErrorEnvelope(
          userMessage = result.message.ifBlank { fallbackMessage },
          retryAfterSeconds = result.retryAfterSeconds,
          telemetryId = result.telemetryId,
          cause = result.cause,
          context = result.context,
        )
      is NanoAIResult.FatalError ->
        NanoAIErrorEnvelope(
          userMessage = result.message.ifBlank { fallbackMessage },
          telemetryId = result.telemetryId,
          cause = result.cause,
          context = result.context,
        )
    }

  fun fromThrowable(error: Throwable?, fallbackMessage: String): NanoAIErrorEnvelope =
    NanoAIErrorEnvelope(
      userMessage = error?.message?.ifBlank { fallbackMessage } ?: fallbackMessage,
      cause = error,
    )
}

fun NanoAIResult<*>.toErrorEnvelope(fallbackMessage: String): NanoAIErrorEnvelope =
  NanoAIErrorFormatter.fromResult(this, fallbackMessage)

fun Throwable?.toErrorEnvelope(fallbackMessage: String): NanoAIErrorEnvelope =
  NanoAIErrorFormatter.fromThrowable(this, fallbackMessage)
