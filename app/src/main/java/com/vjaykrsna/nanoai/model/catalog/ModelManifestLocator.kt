package com.vjaykrsna.nanoai.model.catalog

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** Describes how to resolve a model manifest or direct artifact. */
sealed class ModelManifestLocator {
  /** Manifest served by the NanoAI catalog backend (default behaviour). */
  data class Remote(val manifestUrl: String) : ModelManifestLocator()

  /** Manifest derived from Hugging Face repository metadata. */
  data class HuggingFace(
    val repository: String,
    val artifactPath: String,
    val revision: String?,
  ) : ModelManifestLocator()

  companion object {
    private const val HUGGING_FACE_SCHEME = "hf"

    fun parse(raw: String): ModelManifestLocator {
      val uri = toUriOrNull(raw)
      if (uri == null || !uri.isHuggingFaceScheme()) return Remote(raw)

      val queryParams = uri.rawQuery?.let(::parseQueryParams).orEmpty()
      val repository = extractRepository(uri)
      val artifactPath = extractArtifactPath(queryParams, uri)

      return if (repository.isNotBlank() && artifactPath != null) {
        HuggingFace(
          repository = repository,
          artifactPath = artifactPath,
          revision = queryParams["revision"] ?: queryParams["ref"],
        )
      } else {
        Remote(raw)
      }
    }

    private fun parseQueryParams(query: String): Map<String, String> {
      return query
        .split('&')
        .mapNotNull { pair ->
          if (pair.isBlank()) return@mapNotNull null
          val keyValue = pair.split('=', limit = 2)
          val key = keyValue.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
          val value = keyValue.getOrNull(1)?.let(::decode) ?: ""
          decode(key) to value
        }
        .toMap()
    }

    private fun decode(value: String): String =
      URLDecoder.decode(value, StandardCharsets.UTF_8.name())

    private fun toUriOrNull(raw: String): URI? = runCatching { URI(raw) }.getOrNull()

    private fun URI.isHuggingFaceScheme(): Boolean =
      scheme?.equals(HUGGING_FACE_SCHEME, ignoreCase = true) == true

    private fun extractRepository(uri: URI): String {
      return buildList {
          uri.host?.takeIf { it.isNotBlank() }?.let(::add)
          uri.path?.trim('/')?.takeIf { it.isNotBlank() }?.let(::add)
        }
        .joinToString(separator = "/")
    }

    private fun extractArtifactPath(
      queryParams: Map<String, String>,
      uri: URI,
    ): String? {
      return queryParams["artifact"] ?: queryParams["file"] ?: queryParams["path"] ?: uri.fragment
    }
  }
}
