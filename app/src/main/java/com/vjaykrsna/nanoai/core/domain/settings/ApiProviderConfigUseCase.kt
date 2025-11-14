package com.vjaykrsna.nanoai.core.domain.settings

import android.database.sqlite.SQLiteException
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.ProviderCredentialMutation
import com.vjaykrsna.nanoai.core.domain.repository.ApiProviderConfigRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/** Use case for API provider configuration operations. */
@Singleton
class ApiProviderConfigUseCase
@Inject
constructor(private val apiProviderConfigRepository: ApiProviderConfigRepository) :
  ApiProviderConfigUseCaseInterface {
  /** Observe all API providers. */
  fun observeAllProviders(): Flow<List<APIProviderConfig>> =
    apiProviderConfigRepository.observeAllProviders()

  /** Get all API providers. */
  override suspend fun getAllProviders(): NanoAIResult<List<APIProviderConfig>> =
    guardRepositoryCall(message = "Failed to get all API providers") {
      val providers = apiProviderConfigRepository.getAllProviders()
      NanoAIResult.success(providers)
    }

  /** Get a specific provider by ID. */
  override suspend fun getProvider(providerId: String): NanoAIResult<APIProviderConfig?> =
    guardRepositoryCall(
      message = "Failed to get API provider $providerId",
      context = mapOf("providerId" to providerId),
    ) {
      val provider = apiProviderConfigRepository.getProvider(providerId)
      NanoAIResult.success(provider)
    }

  /** Add a new API provider. */
  override suspend fun addProvider(
    config: APIProviderConfig,
    credentialMutation: ProviderCredentialMutation,
  ): NanoAIResult<Unit> =
    guardRepositoryCall(
      message = "Failed to add API provider ${config.providerId}",
      context = mapOf("providerId" to config.providerId, "providerName" to config.providerName),
    ) {
      apiProviderConfigRepository.addProvider(config, credentialMutation)
      NanoAIResult.success(Unit)
    }

  /** Update an existing API provider. */
  override suspend fun updateProvider(
    config: APIProviderConfig,
    credentialMutation: ProviderCredentialMutation,
  ): NanoAIResult<Unit> =
    guardRepositoryCall(
      message = "Failed to update API provider ${config.providerId}",
      context = mapOf("providerId" to config.providerId, "providerName" to config.providerName),
    ) {
      apiProviderConfigRepository.updateProvider(config, credentialMutation)
      NanoAIResult.success(Unit)
    }

  /** Delete an API provider. */
  override suspend fun deleteProvider(providerId: String): NanoAIResult<Unit> =
    guardRepositoryCall(
      message = "Failed to delete API provider $providerId",
      context = mapOf("providerId" to providerId),
    ) {
      apiProviderConfigRepository.deleteProvider(providerId)
      NanoAIResult.success(Unit)
    }

  private inline fun <T> guardRepositoryCall(
    message: String,
    context: Map<String, String> = emptyMap(),
    block: () -> NanoAIResult<T>,
  ): NanoAIResult<T> {
    return try {
      block()
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (sqliteException: SQLiteException) {
      NanoAIResult.recoverable(message = message, cause = sqliteException, context = context)
    } catch (ioException: IOException) {
      NanoAIResult.recoverable(message = message, cause = ioException, context = context)
    } catch (illegalStateException: IllegalStateException) {
      NanoAIResult.recoverable(message = message, cause = illegalStateException, context = context)
    } catch (illegalArgumentException: IllegalArgumentException) {
      NanoAIResult.recoverable(
        message = message,
        cause = illegalArgumentException,
        context = context,
      )
    }
  }
}
