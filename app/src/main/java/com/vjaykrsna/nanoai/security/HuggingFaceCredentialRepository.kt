package com.vjaykrsna.nanoai.security

import com.vjaykrsna.nanoai.security.model.CredentialScope
import com.vjaykrsna.nanoai.security.model.SecretCredential
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Instant

/** Contract for exposing Hugging Face access tokens to network interceptors. */
interface HuggingFaceTokenProvider {
  fun accessToken(): String?
}

/** Repository interface for Hugging Face credential management. */
interface HuggingFaceCredentialRepository : HuggingFaceTokenProvider {
  fun credential(): SecretCredential?

  fun saveAccessToken(token: String, rotatesAfter: Instant?, metadata: Map<String, String>)

  fun clearAccessToken()

  fun hasAccessToken(): Boolean

  companion object {
    const val PROVIDER_ID = "huggingface"
  }
}

/** Persistent storage for Hugging Face credentials backed by [EncryptedSecretStore]. */
@Singleton
class HuggingFaceCredentialRepositoryImpl
@Inject
constructor(private val secretStore: EncryptedSecretStore) : HuggingFaceCredentialRepository {

  override fun accessToken(): String? = credential()?.encryptedValue?.takeIf { it.isNotBlank() }

  override fun credential(): SecretCredential? =
    secretStore.getCredential(HuggingFaceCredentialRepository.PROVIDER_ID)

  override fun saveAccessToken(
    token: String,
    rotatesAfter: Instant?,
    metadata: Map<String, String>,
  ) {
    val mergedMetadata = DEFAULT_METADATA + metadata

    secretStore.saveCredential(
      providerId = HuggingFaceCredentialRepository.PROVIDER_ID,
      encryptedValue = token,
      scope = CredentialScope.TEXT_INFERENCE,
      rotatesAfter = rotatesAfter,
      metadata = mergedMetadata,
    )
  }

  override fun clearAccessToken() {
    secretStore.deleteCredential(HuggingFaceCredentialRepository.PROVIDER_ID)
  }

  override fun hasAccessToken(): Boolean = !accessToken().isNullOrBlank()

  companion object {
    private val DEFAULT_METADATA = mapOf("issuer" to "huggingface")
  }
}
