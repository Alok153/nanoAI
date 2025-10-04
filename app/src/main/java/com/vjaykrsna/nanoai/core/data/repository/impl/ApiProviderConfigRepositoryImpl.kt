package com.vjaykrsna.nanoai.core.data.repository.impl

import com.vjaykrsna.nanoai.core.data.db.daos.ApiProviderConfigDao
import com.vjaykrsna.nanoai.core.data.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.toDomain
import com.vjaykrsna.nanoai.core.domain.model.toEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of ApiProviderConfigRepository.
 *
 * Wraps ApiProviderConfigDao, converting between entities and domain models.
 */
@Singleton
class ApiProviderConfigRepositoryImpl
@Inject
constructor(
  private val apiProviderConfigDao: ApiProviderConfigDao,
) : ApiProviderConfigRepository {
  override suspend fun getAllProviders(): List<APIProviderConfig> =
    apiProviderConfigDao.getAll().map { it.toDomain() }

  override suspend fun getProvider(providerId: String): APIProviderConfig? =
    apiProviderConfigDao.getById(providerId)?.toDomain()

  override suspend fun addProvider(config: APIProviderConfig) {
    apiProviderConfigDao.insert(config.toEntity())
  }

  override suspend fun updateProvider(config: APIProviderConfig) {
    apiProviderConfigDao.update(config.toEntity())
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
}
