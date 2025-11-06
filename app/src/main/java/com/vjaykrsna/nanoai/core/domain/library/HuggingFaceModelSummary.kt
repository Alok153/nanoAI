package com.vjaykrsna.nanoai.core.domain.library

import kotlinx.datetime.Instant

/** Lightweight descriptor for a Hugging Face model listing. */
data class HuggingFaceModelSummary(
  val modelId: String,
  val displayName: String,
  val author: String?,
  val pipelineTag: String?,
  val libraryName: String?,
  val tags: List<String>,
  val likes: Long,
  val downloads: Long,
  val license: String? = null,
  val languages: List<String> = emptyList(),
  val baseModel: String? = null,
  val datasets: List<String> = emptyList(),
  val architectures: List<String> = emptyList(),
  val modelType: String? = null,
  val baseModelRelations: List<String> = emptyList(),
  val hasGatedAccess: Boolean = false,
  val isDisabled: Boolean = false,
  val totalSizeBytes: Long? = null,
  val summary: String? = null,
  val description: String? = null,
  val trendingScore: Long?,
  val createdAt: Instant?,
  val lastModified: Instant?,
  val isPrivate: Boolean,
  val sizeBucket: HuggingFaceSizeBucket? =
    totalSizeBytes?.let { bytes ->
      HuggingFaceSizeBucket.values().firstOrNull { bucket -> bytes.belongsTo(bucket) }
    },
)
