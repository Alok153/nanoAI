package com.vjaykrsna.nanoai.core.security

import com.vjaykrsna.nanoai.core.security.model.CredentialScope
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade that persists provider credentials inside [EncryptedSecretStore] and exposes opaque IDs
 * for lookups.
 */
@Singleton
class ProviderCredentialStore @Inject constructor(private val secretStore: EncryptedSecretStore) {
  /** Persist or update the credential for the given provider and return the opaque key ID. */
  fun save(providerId: String, credentialValue: String, existingCredentialId: String?): String {
    val credentialId = existingCredentialId ?: newCredentialId(providerId)
    secretStore.saveCredential(
      providerId = credentialId,
      encryptedValue = credentialValue,
      scope = CredentialScope.TEXT_INFERENCE,
      metadata = mapOf("providerId" to providerId),
    )
    return credentialId
  }

  /** Delete the credential represented by [credentialId], if present. */
  fun delete(credentialId: String?) {
    credentialId ?: return
    secretStore.deleteCredential(credentialId)
  }

  /** Resolve the plaintext credential associated with [credentialId], if available. */
  fun resolve(credentialId: String?): String? {
    credentialId ?: return null
    return secretStore.getCredential(credentialId)?.encryptedValue
  }

  private fun newCredentialId(providerId: String): String =
    "provider-$providerId-${UUID.randomUUID()}"
}
