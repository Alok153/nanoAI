package com.vjaykrsna.nanoai.security

import com.vjaykrsna.nanoai.security.model.CredentialScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Instant

/** Contract for exposing Hugging Face access tokens to network interceptors. */
interface HuggingFaceTokenProvider {
  fun accessToken(): String?
}

/** Persistent storage for Hugging Face credentials backed by [EncryptedSecretStore]. */
@Singleton
class HuggingFaceCredentialRepository
@Inject
constructor(
  private val secretStore: EncryptedSecretStore,
) : HuggingFaceTokenProvider {

  override fun accessToken(): String? =
    secretStore.getCredential(PROVIDER_ID)?.encryptedValue?.takeIf { it.isNotBlank() }

  fun saveAccessToken(
    token: String,
    rotatesAfter: Instant? = null,
    metadata: Map<String, String> = emptyMap(),
  ) {
    secretStore.saveCredential(
      providerId = PROVIDER_ID,
      encryptedValue = token,
      scope = CredentialScope.TEXT_INFERENCE,
      rotatesAfter = rotatesAfter,
      metadata = metadata.ifEmpty { DEFAULT_METADATA },
    )
  }

  fun clearAccessToken() {
    secretStore.deleteCredential(PROVIDER_ID)
  }

  fun hasAccessToken(): Boolean = !accessToken().isNullOrBlank()

  companion object {
    const val PROVIDER_ID = "huggingface"

    private val DEFAULT_METADATA = mapOf("issuer" to "huggingface")
  }
}
