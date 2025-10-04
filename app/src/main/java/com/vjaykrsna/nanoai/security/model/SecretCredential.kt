package com.vjaykrsna.nanoai.security.model

import kotlinx.datetime.Instant

/** Scope of credentials managed by [EncryptedSecretStore]. */
enum class CredentialScope {
  TEXT_INFERENCE,
  VISION,
  AUDIO,
  EXPORT,
}

/** Representation of a stored provider credential. */
data class SecretCredential(
  val providerId: String,
  val encryptedValue: String,
  val keyAlias: String,
  val storedAt: Instant,
  val rotatesAfter: Instant?,
  val scope: CredentialScope,
  val metadata: Map<String, String>,
)
