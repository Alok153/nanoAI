package com.vjaykrsna.nanoai.feature.library.data.catalog

import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.domain.InstallState
import com.vjaykrsna.nanoai.feature.library.domain.ProviderType
import com.vjaykrsna.nanoai.shared.model.catalog.DeliveryType
import java.util.Locale
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Serializable configuration describing the curated model catalog bundled with the app. */
@Serializable
internal data class ModelCatalogConfig(
  val version: Int = 1,
  val models: List<ModelConfig> = emptyList(),
)

/** Minimal representation of a model entry parsed from static configuration. */
@Serializable
internal data class ModelConfig(
  @SerialName("id") val id: String,
  @SerialName("display_name") val displayName: String,
  val version: String,
  @SerialName("provider") val providerType: String,
  @SerialName("delivery") val deliveryType: String,
  @SerialName("min_app_version") val minAppVersion: Int = 1,
  @SerialName("size_bytes") val sizeBytes: Long,
  val capabilities: List<String> = emptyList(),
  @SerialName("manifest_url") val manifestUrl: String,
  @SerialName("checksum_sha256") val checksumSha256: String? = null,
  val signature: String? = null,
  @SerialName("created_at") val createdAt: String? = null,
  @SerialName("updated_at") val updatedAt: String? = null,
  // Enhanced metadata for consistency with HuggingFace models
  val author: String? = null,
  val license: String? = null,
  val languages: List<String> = emptyList(),
  @SerialName("base_model") val baseModel: String? = null,
  val architectures: List<String> = emptyList(),
  @SerialName("model_type") val modelType: String? = null,
  val summary: String? = null,
  val description: String? = null,
) {
  fun toModelPackage(clock: Clock): ModelPackage {
    val provider =
      runCatching { ProviderType.valueOf(providerType.uppercase(Locale.US)) }
        .getOrElse { throw IllegalArgumentException("Unknown provider type: $providerType") }
    val delivery =
      runCatching { DeliveryType.valueOf(deliveryType.uppercase(Locale.US)) }
        .getOrElse { throw IllegalArgumentException("Unknown delivery type: $deliveryType") }
    val now = clock.now()
    return ModelPackage(
      modelId = id,
      displayName = displayName,
      version = version,
      providerType = provider,
      deliveryType = delivery,
      minAppVersion = minAppVersion,
      sizeBytes = sizeBytes,
      capabilities = capabilities.map { it.trim() }.filter { it.isNotBlank() }.toSet(),
      installState = InstallState.NOT_INSTALLED,
      downloadTaskId = null,
      manifestUrl = manifestUrl,
      checksumSha256 = checksumSha256,
      signature = signature,
      createdAt = createdAt?.let(::parseInstant) ?: now,
      updatedAt = updatedAt?.let(::parseInstant) ?: now,
      // Enhanced metadata for consistency with HuggingFace models
      author = author,
      license = license,
      languages = languages,
      baseModel = baseModel,
      architectures = architectures,
      modelType = modelType,
      summary = summary,
      description = description,
    )
  }

  private fun parseInstant(value: String): Instant =
    runCatching { Instant.parse(value) }
      .getOrElse { throw IllegalArgumentException("Invalid ISO timestamp: $value", it) }
}
