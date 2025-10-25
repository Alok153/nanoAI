package com.vjaykrsna.nanoai.feature.library.data.leap

import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.data.catalog.DeliveryType
import com.vjaykrsna.nanoai.feature.library.domain.InstallState
import com.vjaykrsna.nanoai.feature.library.domain.ProviderType
import kotlinx.datetime.Instant

/**
 * Represents a Leap model, which is a specialized type of [ModelPackage]. This class adds
 * Leap-specific properties to the base model.
 */
data class LeapModel(
  val modelId: String,
  val displayName: String,
  val version: String,
  val sizeBytes: Long,
  val downloadUrl: String,
  val checksumSha256: String,
  val createdAt: Instant,
  val updatedAt: Instant,
  val summary: String,
  val description: String,
  val author: String,
  val license: String,
  val languages: List<String>,
  val tags: List<String>,
)

/** Converts a [LeapModel] to a [ModelPackage] for use in the domain layer. */
fun LeapModel.toModelPackage(): ModelPackage =
  ModelPackage(
    modelId = modelId,
    displayName = displayName,
    version = version,
    providerType = ProviderType.LEAP,
    deliveryType = DeliveryType.CLOUD_FALLBACK,
    minAppVersion = 1,
    sizeBytes = sizeBytes,
    capabilities = setOf("text-generation", "constrained-generation", "function-calling"),
    installState = InstallState.NOT_INSTALLED,
    downloadTaskId = null,
    manifestUrl = downloadUrl,
    checksumSha256 = checksumSha256,
    signature = null,
    createdAt = createdAt,
    updatedAt = updatedAt,
    author = author,
    license = license,
    languages = languages,
    baseModel = null,
    architectures = emptyList(),
    modelType = "leap",
    summary = summary,
    description = description,
  )
