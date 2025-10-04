package com.vjaykrsna.nanoai.model.catalog

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
  val expiresAt: Instant?,
  val fetchedAt: Instant,
)
