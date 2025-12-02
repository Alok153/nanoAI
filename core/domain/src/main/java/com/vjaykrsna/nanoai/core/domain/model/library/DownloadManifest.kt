package com.vjaykrsna.nanoai.core.domain.model.library

import kotlinx.datetime.Instant

/** Domain representation of a model manifest fetched from any source. */
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
