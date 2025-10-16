package com.vjaykrsna.nanoai.model.huggingface.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Minimal representation of a model returned by the Hugging Face listing API. */
@Serializable
data class HuggingFaceModelListingDto(
  @SerialName("modelId") val modelId: String? = null,
  @SerialName("id") val id: String? = null,
  @SerialName("author") val author: String? = null,
  @SerialName("pipeline_tag") val pipelineTag: String? = null,
  @SerialName("library_name") val libraryName: String? = null,
  @SerialName("tags") val tags: List<String?> = emptyList(),
  @SerialName("likes") val likes: Long? = null,
  @SerialName("downloads") val downloads: Long? = null,
  @SerialName("trendingScore") val trendingScore: Long? = null,
  @SerialName("createdAt") val createdAt: String? = null,
  @SerialName("lastModified") val lastModified: String? = null,
  @SerialName("private") val isPrivate: Boolean? = null,
)
