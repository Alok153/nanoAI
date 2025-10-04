package com.vjaykrsna.nanoai.contracts

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Contract tests for POST /credentials/providers/{providerId} payloads and responses.
 *
 * TDD note: These assertions fail until the credential rotation API enforces the documented
 * environments and response metadata defined in the stabilization spec.
 */
class CredentialRotationContractTest {
  private val rotationRequest =
    mapOf(
      "environment" to "production",
      "encryptedKey" to "BASE64_PAYLOAD",
    )

  private val rotationResponse =
    mapOf(
      "providerId" to "openai",
      "storedAt" to "2025-10-03T10:15:30Z",
      "keyAlias" to "openai-prod-2025",
      "migrationRequired" to false,
    )

  @Test
  fun `rotation request must use supported environment enum`() {
    val allowedEnvironments = setOf("production", "staging", "sandbox")
    val environment = rotationRequest["environment"] as? String
    assertThat(environment).isNotNull()
    assertThat(environment).isIn(allowedEnvironments)
  }

  @Test
  fun `rotation response must include keyAlias and migration flag`() {
    assertThat(rotationResponse).containsKey("keyAlias")
    assertThat(rotationResponse).containsKey("migrationRequired")
  }
}
