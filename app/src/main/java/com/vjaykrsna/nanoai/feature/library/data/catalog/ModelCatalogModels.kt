package com.vjaykrsna.nanoai.feature.library.data.catalog

import kotlinx.datetime.Instant

/** Delivery mechanisms for model packages. */
enum class DeliveryType {
  LOCAL_ARCHIVE,
  PLAY_ASSET,
  CLOUD_FALLBACK,
}

/** Representation of a model manifest fetched from the server. */
data class DownloadManifest(
  val modelId: String,
  val version: String,
  val checksumSha256: String,
  val sizeBytes: Long,
  val downloadUrl: String,
  val signature: String?,
  val publicKeyUrl: String?,
  val expiresAt: Instant?,
  val fetchedAt: Instant,
)

/** Converts a network DTO into a domain manifest representation. */
fun com.vjaykrsna.nanoai.feature.library.data.catalog.network.dto.ModelManifestDto.toDomain(
  fetchedAt: Instant,
  expiresParser: (String) -> Instant? = { runCatching { Instant.parse(it) }.getOrNull() },
): DownloadManifest =
  DownloadManifest(
    modelId = modelId,
    version = version,
    checksumSha256 = checksumSha256,
    sizeBytes = sizeBytes,
    downloadUrl = downloadUrl,
    signature = signature,
    publicKeyUrl = publicKeyUrl,
    expiresAt = expiresAt?.let(expiresParser),
    fetchedAt = fetchedAt,
  )

/** Persistable representation of a manifest for Room caching. */
fun DownloadManifest.toEntity(): DownloadManifestEntity =
  DownloadManifestEntity(
    modelId = modelId,
    version = version,
    checksumSha256 = checksumSha256,
    sizeBytes = sizeBytes,
    downloadUrl = downloadUrl,
    signature = signature,
    publicKeyUrl = publicKeyUrl,
    expiresAt = expiresAt,
    fetchedAt = fetchedAt,
    releaseNotes = null,
  )
