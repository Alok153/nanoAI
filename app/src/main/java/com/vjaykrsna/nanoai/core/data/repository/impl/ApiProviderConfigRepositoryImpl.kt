package com.vjaykrsna.nanoai.core.data.repository.impl

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.data.db.daos.ApiProviderConfigDao
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.ProviderCredentialMutation
import com.vjaykrsna.nanoai.core.domain.model.toDomain
import com.vjaykrsna.nanoai.core.domain.model.toEntity
import com.vjaykrsna.nanoai.core.domain.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.security.ProviderCredentialStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of ApiProviderConfigRepository.
 *
 * Wraps ApiProviderConfigDao, converting between entities and domain models.
 */
@Singleton
@Suppress("UnusedPrivateProperty") // Will be used for future IO operations
class ApiProviderConfigRepositoryImpl
@Inject
constructor(
  private val apiProviderConfigDao: ApiProviderConfigDao,
  private val providerCredentialStore: ProviderCredentialStore,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ApiProviderConfigRepository {
  override suspend fun getAllProviders(): List<APIProviderConfig> =
    apiProviderConfigDao.getAll().map { it.toDomain() }

  override suspend fun getProvider(providerId: String): APIProviderConfig? =
    apiProviderConfigDao.getById(providerId)?.toDomain()

  override suspend fun addProvider(
    config: APIProviderConfig,
    credentialMutation: ProviderCredentialMutation,
  ) {
    val credentialId = mutateCredential(config.providerId, null, credentialMutation)
    apiProviderConfigDao.insert(config.copy(credentialId = credentialId).toEntity())
  }

  override suspend fun updateProvider(
    config: APIProviderConfig,
    credentialMutation: ProviderCredentialMutation,
  ) {
    val credentialId = mutateCredential(config.providerId, config.credentialId, credentialMutation)
    apiProviderConfigDao.update(config.copy(credentialId = credentialId).toEntity())
  }

  override suspend fun deleteProvider(providerId: String) {
    val config = apiProviderConfigDao.getById(providerId)
    if (config != null) {
      apiProviderConfigDao.delete(config)
    }
  }

  override suspend fun getEnabledProviders(): List<APIProviderConfig> =
    apiProviderConfigDao.getEnabled().map { it.toDomain() }

  override fun observeAllProviders(): Flow<List<APIProviderConfig>> =
    apiProviderConfigDao.observeAll().map { providers -> providers.map { it.toDomain() } }

  private fun mutateCredential(
    providerId: String,
    currentCredentialId: String?,
    mutation: ProviderCredentialMutation,
  ): String? =
    when (mutation) {
      ProviderCredentialMutation.None -> currentCredentialId
      ProviderCredentialMutation.Remove -> {
        providerCredentialStore.delete(currentCredentialId)
        null
      }
      is ProviderCredentialMutation.Replace ->
        providerCredentialStore.save(providerId, mutation.value, currentCredentialId)
    }
}
