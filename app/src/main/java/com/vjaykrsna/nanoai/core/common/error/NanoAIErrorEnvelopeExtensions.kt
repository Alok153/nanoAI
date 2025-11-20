package com.vjaykrsna.nanoai.core.common.error

/**
 * Ensures the envelope carries a user-facing message that includes the provided [fallback].
 *
 * Older call sites built ad-hoc helpers that appended fallback strings. Centralising the logic
 * keeps snackbar copy consistent across features.
 */
fun NanoAIErrorEnvelope.withFallbackMessage(fallback: String): NanoAIErrorEnvelope {
  if (userMessage.isBlank()) {
    return copy(userMessage = fallback)
  }
  return if (userMessage.contains(fallback)) {
    this
  } else {
    copy(userMessage = "$fallback: $userMessage")
  }
}
