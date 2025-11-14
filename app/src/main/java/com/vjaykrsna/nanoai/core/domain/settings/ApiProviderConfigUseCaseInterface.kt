package com.vjaykrsna.nanoai.core.domain.settings

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.ProviderCredentialMutation

/** Interface for API provider configuration operations. */
interface ApiProviderConfigUseCaseInterface {
  suspend fun getAllProviders(): NanoAIResult<List<APIProviderConfig>>

  suspend fun getProvider(providerId: String): NanoAIResult<APIProviderConfig?>

  suspend fun addProvider(
    config: APIProviderConfig,
    credentialMutation: ProviderCredentialMutation,
  ): NanoAIResult<Unit>

  suspend fun updateProvider(
    config: APIProviderConfig,
    credentialMutation: ProviderCredentialMutation,
  ): NanoAIResult<Unit>

  suspend fun deleteProvider(providerId: String): NanoAIResult<Unit>
}
