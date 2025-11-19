package com.vjaykrsna.nanoai.core.domain.settings

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.ProviderCredentialMutation

/** Interface for API provider configuration operations. */
interface ApiProviderConfigUseCaseInterface {
  @OneShot("Fetch all API providers")
  suspend fun getAllProviders(): NanoAIResult<List<APIProviderConfig>>

  @OneShot("Fetch provider by identifier")
  suspend fun getProvider(providerId: String): NanoAIResult<APIProviderConfig?>

  @OneShot("Add API provider")
  suspend fun addProvider(
    config: APIProviderConfig,
    credentialMutation: ProviderCredentialMutation,
  ): NanoAIResult<Unit>

  @OneShot("Update API provider")
  suspend fun updateProvider(
    config: APIProviderConfig,
    credentialMutation: ProviderCredentialMutation,
  ): NanoAIResult<Unit>

  @OneShot("Delete API provider") suspend fun deleteProvider(providerId: String): NanoAIResult<Unit>
}
