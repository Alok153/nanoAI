package com.vjaykrsna.nanoai.feature.settings.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import javax.inject.Inject
import javax.inject.Singleton

/** Use case for API provider configuration operations. */
@Singleton
class ApiProviderConfigUseCase
@Inject
constructor(private val apiProviderConfigRepository: ApiProviderConfigRepository) {
  /** Get all API providers. */
  suspend fun getAllProviders(): NanoAIResult<List<APIProviderConfig>> {
    return try {
      val providers = apiProviderConfigRepository.getAllProviders()
      NanoAIResult.success(providers)
    } catch (e: Exception) {
      NanoAIResult.recoverable(message = "Failed to get all API providers", cause = e)
    }
  }

  /** Get a specific provider by ID. */
  suspend fun getProvider(providerId: String): NanoAIResult<APIProviderConfig?> {
    return try {
      val provider = apiProviderConfigRepository.getProvider(providerId)
      NanoAIResult.success(provider)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to get API provider $providerId",
        cause = e,
        context = mapOf("providerId" to providerId),
      )
    }
  }

  /** Add a new API provider. */
  suspend fun addProvider(config: APIProviderConfig): NanoAIResult<Unit> {
    return try {
      apiProviderConfigRepository.addProvider(config)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to add API provider ${config.providerId}",
        cause = e,
        context = mapOf("providerId" to config.providerId, "providerName" to config.providerName),
      )
    }
  }

  /** Update an existing API provider. */
  suspend fun updateProvider(config: APIProviderConfig): NanoAIResult<Unit> {
    return try {
      apiProviderConfigRepository.updateProvider(config)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to update API provider ${config.providerId}",
        cause = e,
        context = mapOf("providerId" to config.providerId, "providerName" to config.providerName),
      )
    }
  }

  /** Delete an API provider. */
  suspend fun deleteProvider(providerId: String): NanoAIResult<Unit> {
    return try {
      apiProviderConfigRepository.deleteProvider(providerId)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to delete API provider $providerId",
        cause = e,
        context = mapOf("providerId" to providerId),
      )
    }
  }
}
