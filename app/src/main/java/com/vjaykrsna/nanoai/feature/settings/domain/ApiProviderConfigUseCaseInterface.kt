package com.vjaykrsna.nanoai.feature.settings.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig

/** Interface for API provider configuration operations. */
interface ApiProviderConfigUseCaseInterface {
  suspend fun getAllProviders(): NanoAIResult<List<APIProviderConfig>>

  suspend fun getProvider(providerId: String): NanoAIResult<APIProviderConfig?>

  suspend fun addProvider(config: APIProviderConfig): NanoAIResult<Unit>

  suspend fun updateProvider(config: APIProviderConfig): NanoAIResult<Unit>

  suspend fun deleteProvider(providerId: String): NanoAIResult<Unit>
}
