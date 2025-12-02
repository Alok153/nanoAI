package com.vjaykrsna.nanoai.core.domain.model

import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import java.util.UUID
import kotlinx.datetime.Instant

/**
 * Domain model for a model package in the catalog.
 *
 * Clean architecture: Separate from database entities. Used by repositories, use cases, ViewModels,
 * and UI. Mapping to/from entities is handled by
 * [com.vjaykrsna.nanoai.core.data.db.mappers.ModelPackageMapper].
 */
data class ModelPackage(
  val modelId: String,
  val displayName: String,
  val version: String,
  val providerType: ProviderType,
  val deliveryType: DeliveryType,
  val minAppVersion: Int,
  val sizeBytes: Long,
  val capabilities: Set<String>,
  val installState: InstallState,
  val downloadTaskId: UUID? = null,
  val manifestUrl: String,
  val checksumSha256: String? = null,
  val signature: String? = null,
  val createdAt: Instant,
  val updatedAt: Instant,
  // Enhanced metadata for consistency with HuggingFace models
  val author: String? = null,
  val license: String? = null,
  val languages: List<String> = emptyList(),
  val baseModel: String? = null,
  val architectures: List<String> = emptyList(),
  val modelType: String? = null,
  val summary: String? = null,
  val description: String? = null,
)
