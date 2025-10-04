package com.vjaykrsna.nanoai.contracts

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Contract tests for POST /catalog/models/{modelId}/verify responses.
 *
 * TDD note: these tests are written before the verification endpoint exists and intentionally fail
 * until the contract guarantees retry backoff metadata and integrity failure envelopes.
 */
class ModelVerificationContractTest {
  private val retryResponse =
    mapOf(
      "status" to "RETRY",
    )

  private val integrityFailureEnvelope =
    mapOf(
      "code" to "UNKNOWN",
      "message" to "Checksum mismatch detected",
    )

  @Test
  fun `retry response must include nextRetryAfterSeconds when status is RETRY`() {
    assertThat(retryResponse["status"]).isEqualTo("RETRY")
    assertThat(retryResponse).containsKey("nextRetryAfterSeconds")
    val retryDelay = retryResponse["nextRetryAfterSeconds"] as Int
    assertThat(retryDelay).isGreaterThan(0)
  }

  @Test
  fun `integrity failure should map to INTEGRITY_FAILURE error code`() {
    assertThat(integrityFailureEnvelope["message"]).isEqualTo("Checksum mismatch detected")
    assertThat(integrityFailureEnvelope["code"]).isEqualTo("INTEGRITY_FAILURE")
  }
}
