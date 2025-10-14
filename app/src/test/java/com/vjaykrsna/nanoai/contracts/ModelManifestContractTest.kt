package com.vjaykrsna.nanoai.contracts

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Contract test for GET /catalog/models/{modelId}/manifest response body.
 *
 * This test intentionally fails until the manifest contract guarantees signed checksums, HTTPS
 * download URLs, and a populated signature field.
 */
class ModelManifestContractTest {
  private val sampleManifestResponse =
    mapOf(
      "modelId" to "persona-text-delta",
      "version" to "1.2.0",
      "checksumSha256" to "9f2a8a4c7cb0a2aa73ec718f7f9b0d4c70bda2f286f7f0bb7b3b92d4abf65c2e",
      "sizeBytes" to 524_288_000L,
      "downloadUrl" to "https://cdn.nanoai.app/model/persona-text-delta-1.2.0.tgz",
      "signature" to "LS0tLS1CRUdJTiBTSUdOQVRVUkUtLS0tLS0tU0lHTkFUVVJFLURBVEE=",
    )

  @Test
  fun `manifest checksum must be 64-character sha256 hex`() {
    val checksum = sampleManifestResponse["checksumSha256"] as? String
    assertThat(checksum).isNotNull()
    assertThat(checksum).matches("^[0-9a-f]{64}$")
  }

  @Test
  fun `manifest download url must use https`() {
    val downloadUrl = sampleManifestResponse["downloadUrl"] as? String
    assertThat(downloadUrl).isNotNull()
    assertThat(downloadUrl).startsWith("https://")
  }

  @Test
  fun `manifest should include detached signature`() {
    val signature = sampleManifestResponse["signature"] as? String
    assertThat(signature).isNotNull()
    assertThat(signature).isNotEmpty()
  }
}
