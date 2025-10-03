package com.vjaykrsna.nanoai.contracts

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Contract test for GET /catalog/models/{modelId}/manifest response body.
 *
 * TDD note: this test intentionally fails until the manifest contract guarantees
 * signed checksums, HTTPS download URLs, and a populated signature field.
 */
class ModelManifestContractTest {
    private val sampleManifestResponse =
        mapOf(
            "modelId" to "persona-text-delta",
            "version" to "1.2.0",
            "checksumSha256" to "abc123",
            "sizeBytes" to 524_288_000L,
            "downloadUrl" to "http://cdn.nanoai.app/model/persona-text-delta-1.2.0.tgz",
            "signature" to "",
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
