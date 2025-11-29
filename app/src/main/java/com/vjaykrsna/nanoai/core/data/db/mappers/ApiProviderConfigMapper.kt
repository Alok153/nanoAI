package com.vjaykrsna.nanoai.core.data.db.mappers

import com.vjaykrsna.nanoai.core.data.db.entities.ApiProviderConfigEntity
import com.vjaykrsna.nanoai.core.domain.model.ApiProviderConfig

/** Maps API provider configurations between Room entities and domain models. */
internal object ApiProviderConfigMapper {
  /**
   * Converts an [ApiProviderConfigEntity] database entity to an [ApiProviderConfig] domain model.
   */
  fun toDomain(entity: ApiProviderConfigEntity): ApiProviderConfig =
    ApiProviderConfig(
      providerId = entity.providerId,
      providerName = entity.providerName,
      baseUrl = entity.baseUrl,
      apiType = entity.apiType,
      isEnabled = entity.isEnabled,
      quotaResetAt = entity.quotaResetAt,
      lastStatus = entity.lastStatus,
      credentialId = entity.credentialId,
    )

  /**
   * Converts an [ApiProviderConfig] domain model to an [ApiProviderConfigEntity] database entity.
   */
  fun toEntity(domain: ApiProviderConfig): ApiProviderConfigEntity =
    ApiProviderConfigEntity(
      providerId = domain.providerId,
      providerName = domain.providerName,
      baseUrl = domain.baseUrl,
      apiType = domain.apiType,
      isEnabled = domain.isEnabled,
      quotaResetAt = domain.quotaResetAt,
      lastStatus = domain.lastStatus,
      credentialId = domain.credentialId,
    )
}

/** Extension function to convert entity to domain model. */
fun ApiProviderConfigEntity.toDomain(): ApiProviderConfig = ApiProviderConfigMapper.toDomain(this)

/** Extension function to convert domain model to entity. */
fun ApiProviderConfig.toEntity(): ApiProviderConfigEntity = ApiProviderConfigMapper.toEntity(this)
