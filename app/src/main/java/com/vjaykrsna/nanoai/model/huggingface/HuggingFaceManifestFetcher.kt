package com.vjaykrsna.nanoai.model.huggingface

import com.vjaykrsna.nanoai.model.catalog.DownloadManifest
import com.vjaykrsna.nanoai.model.huggingface.network.HuggingFaceService
import com.vjaykrsna.nanoai.model.huggingface.network.dto.HuggingFaceLfsDto
import com.vjaykrsna.nanoai.model.huggingface.network.dto.HuggingFaceModelDto
import com.vjaykrsna.nanoai.model.huggingface.network.dto.HuggingFacePathInfoDto
import com.vjaykrsna.nanoai.model.huggingface.network.dto.HuggingFaceSiblingDto
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Clock

private const val BASE_DOWNLOAD_URL = "https://huggingface.co"

/** Request payload describing which Hugging Face artifact to resolve. */
data class HuggingFaceManifestRequest(
  val modelId: String,
  val repository: String,
  val revision: String,
  val artifactPath: String,
  val version: String,
)

/** Fetches manifest metadata for Hugging Face hosted artifacts. */
@Singleton
class HuggingFaceManifestFetcher
@Inject
constructor(
  private val service: HuggingFaceService,
  private val clock: Clock = Clock.System,
) {

  suspend fun fetchManifest(request: HuggingFaceManifestRequest): DownloadManifest {
    val normalizedPath = request.artifactPath.removePrefix("/")

    val primary =
      runCatching { service.getPathsInfo(request.repository, request.revision, normalizedPath) }
        .getOrNull()

    val fromPathsInfo = primary?.firstOrNull()
    val checksumFromPaths = fromPathsInfo?.bestSha256()
    val sizeFromPaths = fromPathsInfo?.bestSize()

    val modelSummary: HuggingFaceModelDto? =
      if (checksumFromPaths != null && sizeFromPaths != null) null
      else runCatching { service.getModelSummary(request.repository) }.getOrNull()

    val sibling =
      modelSummary?.siblings?.firstOrNull { sibling ->
        val candidates =
          setOf(
            sibling.relativeFilename,
            sibling.relativeFilename.removePrefix("/"),
          )
        candidates.contains(normalizedPath)
      }

    val checksum =
      checksumFromPaths
        ?: sibling?.bestSha256()
        ?: modelSummary?.revisionSha?.takeIf { it.isSha256() }
        ?: fromPathsInfo?.gitOid?.takeIf { it.isSha256() }
    val size = sizeFromPaths ?: sibling?.bestSize()

    require(!checksum.isNullOrBlank()) {
      "Unable to resolve SHA-256 checksum for ${request.repository}/${request.artifactPath}"
    }
    require(size != null && size > 0) {
      "Unable to resolve size for ${request.repository}/${request.artifactPath}"
    }

    val downloadUrl = buildString {
      append(BASE_DOWNLOAD_URL)
      append('/')
      append(request.repository)
      append("/resolve/")
      append(request.revision)
      append('/')
      append(normalizedPath)
      append("?download=1")
    }

    return DownloadManifest(
      modelId = request.modelId,
      version = request.version,
      checksumSha256 = checksum,
      sizeBytes = size,
      downloadUrl = downloadUrl,
      signature = null,
      publicKeyUrl = null,
      expiresAt = null,
      fetchedAt = clock.now(),
    )
  }
}

private fun HuggingFacePathInfoDto.bestSha256(): String? {
  return sha256 ?: lfs?.bestSha256() ?: gitOid?.takeIf { it.isSha256() }
}

private fun HuggingFacePathInfoDto.bestSize(): Long? = lfs?.sizeBytes ?: sizeBytes

private fun HuggingFaceSiblingDto.bestSha256(): String? {
  return sha256 ?: lfs?.bestSha256() ?: gitOid?.takeIf { it.isSha256() }
}

private fun HuggingFaceSiblingDto.bestSize(): Long? = lfs?.sizeBytes ?: sizeBytes

private fun HuggingFaceLfsDto.bestSha256(): String? {
  return when {
    !sha256.isNullOrBlank() -> sha256
    oid.startsWith("sha256:", ignoreCase = true) -> oid.substringAfter(':').lowercase(Locale.US)
    oid.isSha256() -> oid
    else -> null
  }
}

private fun String.isSha256(): Boolean = length == 64 && all { it.isHexDigit() }

private fun Char.isHexDigit(): Boolean =
  (this in '0'..'9') || (this in 'a'..'f') || (this in 'A'..'F')
