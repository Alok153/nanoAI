package com.vjaykrsna.nanoai.core.domain.model

import com.vjaykrsna.nanoai.feature.library.data.entities.ModelPackageEntity
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import java.util.UUID
import kotlinx.datetime.Instant

/**
 * Domain model for a model package in the catalog.
 *
 * Clean architecture: Separate from database entities. Used by repositories, use cases, ViewModels,
 * and UI.
 */
data class ModelPackage(
  val modelId: String,
  val displayName: String,
  val version: String,
  val providerType: ProviderType,
  val sizeBytes: Long,
  val capabilities: Set<String>,
  val installState: InstallState,
  val downloadTaskId: UUID? = null,
  val checksum: String? = null,
  val updatedAt: Instant,
)

/** Extension function to convert entity to domain model. */
fun ModelPackageEntity.toDomain(): ModelPackage =
  ModelPackage(
    modelId = modelId,
    displayName = displayName,
    version = version,
    providerType = providerType,
    sizeBytes = sizeBytes,
    capabilities = capabilities,
    installState = installState,
    downloadTaskId = downloadTaskId?.let(UUID::fromString),
    checksum = checksum,
    updatedAt = updatedAt,
  )

/** Extension function to convert domain model to entity. */
fun ModelPackage.toEntity(): ModelPackageEntity =
  ModelPackageEntity(
    modelId = modelId,
    displayName = displayName,
    version = version,
    providerType = providerType,
    sizeBytes = sizeBytes,
    capabilities = capabilities,
    installState = installState,
    downloadTaskId = downloadTaskId?.toString(),
    checksum = checksum,
    updatedAt = updatedAt,
  )
