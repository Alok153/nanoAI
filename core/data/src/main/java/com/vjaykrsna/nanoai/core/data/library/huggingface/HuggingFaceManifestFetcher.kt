package com.vjaykrsna.nanoai.core.data.library.huggingface

import com.vjaykrsna.nanoai.core.data.library.huggingface.network.HuggingFaceService
import com.vjaykrsna.nanoai.core.data.library.huggingface.network.dto.HuggingFaceModelDto
import com.vjaykrsna.nanoai.core.data.library.huggingface.network.dto.HuggingFacePathInfoDto
import com.vjaykrsna.nanoai.core.data.library.huggingface.network.dto.HuggingFaceSiblingDto
import com.vjaykrsna.nanoai.core.data.library.huggingface.network.dto.HuggingFaceTreeEntryDto
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadManifest
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
constructor(private val service: HuggingFaceService, private val clock: Clock = Clock.System) {

  suspend fun fetchManifest(request: HuggingFaceManifestRequest): DownloadManifest {
    val normalizedPath = request.artifactPath.removePrefix("/")
    val (checksum, size, downloadUrl) = resolveManifestData(request, normalizedPath)
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

  private suspend fun resolveManifestData(
    request: HuggingFaceManifestRequest,
    normalizedPath: String,
  ): Triple<String, Long, String> {
    val pathsInfo = fetchPathsInfo(request, normalizedPath)
    val checksumFromPaths = pathsInfo?.bestSha256()
    val sizeFromPaths = pathsInfo?.bestSize()

    val summary = fetchModelSummaryIfNeeded(request.repository, checksumFromPaths, sizeFromPaths)
    val sibling = findSibling(normalizedPath, summary)
    val siblingChecksum = sibling?.bestSha256()
    val siblingSize = sibling?.bestSize()
    val summaryChecksum = summary?.revisionSha?.takeIf { it.isSha256() }
    val gitOidChecksum = pathsInfo?.gitOid?.takeIf { it.isSha256() }

    val hasChecksumWithoutTree =
      sequenceOf(checksumFromPaths, siblingChecksum, summaryChecksum, gitOidChecksum)
        .filterNotNull()
        .firstOrNull() != null
    val hasSizeWithoutTree =
      sequenceOf(sizeFromPaths, siblingSize).filterNotNull().firstOrNull() != null

    val treeEntry =
      resolveTreeEntryIfNeeded(
        request = request,
        normalizedPath = normalizedPath,
        hasChecksum = hasChecksumWithoutTree,
        hasSize = hasSizeWithoutTree,
      )

    val checksum =
      sequenceOf(
          checksumFromPaths,
          siblingChecksum,
          treeEntry?.bestSha256(),
          summaryChecksum,
          gitOidChecksum,
        )
        .filterNotNull()
        .firstOrNull()

    val size =
      sequenceOf(sizeFromPaths, siblingSize, treeEntry?.bestSize()).filterNotNull().firstOrNull()

    require(!checksum.isNullOrBlank()) {
      "Unable to resolve SHA-256 checksum for ${request.repository}/${request.artifactPath}"
    }
    require(size != null && size > 0) {
      "Unable to resolve size for ${request.repository}/${request.artifactPath}"
    }

    val downloadUrl = buildDownloadUrl(request, normalizedPath)

    return Triple(checksum, size, downloadUrl)
  }

  private suspend fun fetchPathsInfo(
    request: HuggingFaceManifestRequest,
    normalizedPath: String,
  ): HuggingFacePathInfoDto? {
    return runCatching {
        service.getPathsInfo(request.repository, request.revision, normalizedPath)
      }
      .getOrNull()
      ?.firstOrNull()
  }

  private suspend fun fetchModelSummaryIfNeeded(
    repository: String,
    checksumFromPaths: String?,
    sizeFromPaths: Long?,
  ): HuggingFaceModelDto? {
    if (checksumFromPaths != null && sizeFromPaths != null) return null
    return runCatching { service.getModelSummary(repository) }.getOrNull()
  }

  private fun findSibling(
    normalizedPath: String,
    summary: HuggingFaceModelDto?,
  ): HuggingFaceSiblingDto? {
    val siblings = summary?.siblings ?: return null
    return siblings.firstOrNull { sibling ->
      val candidates = setOf(sibling.relativeFilename, sibling.relativeFilename.removePrefix("/"))
      normalizedPath in candidates
    }
  }

  private suspend fun resolveTreeEntryIfNeeded(
    request: HuggingFaceManifestRequest,
    normalizedPath: String,
    hasChecksum: Boolean,
    hasSize: Boolean,
  ): HuggingFaceTreeEntryDto? {
    if (hasChecksum && hasSize) return null
    val revisions =
      sequenceOf(request.revision, DEFAULT_REVISION)
        .filterNotNull()
        .map { it.takeIf(String::isNotBlank) }
        .filterNotNull()
        .distinct()
    return revisions.firstNotNullOfOrNull { revision ->
      runCatching { service.getTree(request.repository, revision, normalizedPath, false) }
        .getOrNull()
        ?.firstOrNull { it.path == normalizedPath }
    }
  }

  private fun buildDownloadUrl(
    request: HuggingFaceManifestRequest,
    normalizedPath: String,
  ): String {
    return buildString {
      append(BASE_DOWNLOAD_URL)
      append('/')
      append(request.repository)
      append("/resolve/")
      append(request.revision)
      append('/')
      append(normalizedPath)
      append("?download=1")
    }
  }

  companion object {
    private const val DEFAULT_REVISION = "main"
  }
}
