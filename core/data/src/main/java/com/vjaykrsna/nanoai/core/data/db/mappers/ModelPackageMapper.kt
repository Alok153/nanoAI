package com.vjaykrsna.nanoai.core.data.db.mappers

import com.vjaykrsna.nanoai.core.data.db.entities.ModelPackageEntity
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import java.util.UUID

/** Maps model packages between Room entities and domain models. */
internal object ModelPackageMapper {
  /** Converts a [ModelPackageEntity] database entity to a [ModelPackage] domain model. */
  fun toDomain(entity: ModelPackageEntity): ModelPackage =
    ModelPackage(
      modelId = entity.modelId,
      displayName = entity.displayName,
      version = entity.version,
      providerType = entity.providerType,
      deliveryType = entity.deliveryType,
      minAppVersion = entity.minAppVersion,
      sizeBytes = entity.sizeBytes,
      capabilities = entity.capabilities,
      installState = entity.installState,
      downloadTaskId = entity.downloadTaskId?.let(UUID::fromString),
      manifestUrl = entity.manifestUrl,
      checksumSha256 = entity.checksumSha256,
      signature = entity.signature,
      createdAt = entity.createdAt,
      updatedAt = entity.updatedAt,
    )

  /** Converts a [ModelPackage] domain model to a [ModelPackageEntity] database entity. */
  fun toEntity(domain: ModelPackage): ModelPackageEntity =
    ModelPackageEntity(
      modelId = domain.modelId,
      displayName = domain.displayName,
      version = domain.version,
      providerType = domain.providerType,
      deliveryType = domain.deliveryType,
      minAppVersion = domain.minAppVersion,
      sizeBytes = domain.sizeBytes,
      capabilities = domain.capabilities,
      installState = domain.installState,
      downloadTaskId = domain.downloadTaskId?.toString(),
      manifestUrl = domain.manifestUrl,
      checksumSha256 = domain.checksumSha256,
      signature = domain.signature,
      createdAt = domain.createdAt,
      updatedAt = domain.updatedAt,
    )
}

/** Extension function to convert entity to domain model. */
fun ModelPackageEntity.toDomain(): ModelPackage = ModelPackageMapper.toDomain(this)

/** Extension function to convert domain model to entity. */
fun ModelPackage.toEntity(): ModelPackageEntity = ModelPackageMapper.toEntity(this)
