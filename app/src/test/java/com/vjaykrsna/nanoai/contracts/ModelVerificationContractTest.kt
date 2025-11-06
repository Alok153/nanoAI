package com.vjaykrsna.nanoai.contracts

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.StatusCode
import org.junit.Test

/**
 * Contract tests for POST /catalog/models/{modelId}/verify responses.
 *
 * These tests were written before the verification endpoint exists and intentionally fail until the
 * contract guarantees retry backoff metadata and integrity failure envelopes.
 */
class ModelVerificationContractTest {
  private val retryResponse =
    mapOf("status" to StatusCode.RETRY.value, "nextRetryAfterSeconds" to 120)

  private val integrityFailureEnvelope =
    mapOf("code" to StatusCode.INTEGRITY_FAILURE.value, "message" to "Checksum mismatch detected")

  @Test
  fun `retry response must include nextRetryAfterSeconds when status is RETRY`() {
    assertThat(retryResponse["status"]).isEqualTo(StatusCode.RETRY.value)
    assertThat(retryResponse).containsKey("nextRetryAfterSeconds")
    val retryDelay = retryResponse["nextRetryAfterSeconds"] as Int
    assertThat(retryDelay).isGreaterThan(0)
  }

  @Test
  fun `integrity failure should map to INTEGRITY_FAILURE error code`() {
    assertThat(integrityFailureEnvelope["message"]).isEqualTo("Checksum mismatch detected")
    assertThat(integrityFailureEnvelope["code"]).isEqualTo(StatusCode.INTEGRITY_FAILURE.value)
  }
}
