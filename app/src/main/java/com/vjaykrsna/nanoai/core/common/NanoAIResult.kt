package com.vjaykrsna.nanoai.core.common

/**
 * Minimal sealed result contract used across the codebase.
 *
 * This file intentionally defines the small shape required by tests and can be extended later with
 * richer telemetry/error metadata.
 */
sealed class NanoAIResult {
  /** Success wrapper which may carry a value. */
  data class Success<T>(
    val value: T? = null,
  ) : NanoAIResult()

  /**
   * Recoverable error that includes retry guidance and a telemetry id. Fields match the test
   * expectations: message, retryAfterSeconds, telemetryId
   */
  data class RecoverableError(
    val message: String?,
    val retryAfterSeconds: Long?,
    val telemetryId: String?,
  ) : NanoAIResult()

  /** Non-recoverable failure. */
  data class Failure(
    val message: String?,
  ) : NanoAIResult()
}
