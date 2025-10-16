package com.vjaykrsna.nanoai.feature.library.domain.model

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
  val trendingScore: Long?,
  val createdAt: Instant?,
  val lastModified: Instant?,
  val isPrivate: Boolean,
)
