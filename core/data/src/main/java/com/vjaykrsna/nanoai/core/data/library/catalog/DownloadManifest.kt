package com.vjaykrsna.nanoai.core.data.library.catalog

import com.vjaykrsna.nanoai.core.data.db.entities.DownloadManifestEntity
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadManifest
import kotlinx.datetime.Instant

/** Converts a network DTO into a domain manifest representation. */
fun com.vjaykrsna.nanoai.core.data.library.catalog.network.dto.ModelManifestDto.toDomain(
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
