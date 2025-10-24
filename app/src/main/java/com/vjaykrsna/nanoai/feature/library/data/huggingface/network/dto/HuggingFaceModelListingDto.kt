package com.vjaykrsna.nanoai.feature.library.data.huggingface.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

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
  @SerialName("gated") val gated: JsonElement? = null,
  @SerialName("disabled") val disabled: Boolean? = null,
  @SerialName("cardData") val cardData: ModelCardDataDto? = null,
  @SerialName("config") val config: ModelConfigDto? = null,
  @SerialName("baseModels") val baseModels: BaseModelRelationDto? = null,
  @SerialName("siblings") val siblings: List<ModelSiblingDto>? = null,
  @SerialName("trendingScore") val trendingScore: Long? = null,
  @SerialName("createdAt") val createdAt: String? = null,
  @SerialName("lastModified") val lastModified: String? = null,
  @SerialName("private") val isPrivate: Boolean? = null,
) {
  /** Returns true if the model has gated access (either boolean true or string "manual"). */
  val isGated: Boolean
    get() =
      when (gated) {
        is JsonPrimitive -> {
          gated.jsonPrimitive.booleanOrNull ?: (gated.jsonPrimitive.content == "manual")
        }
        else -> false
      }
}

@Serializable
data class ModelCardDataDto(
  @SerialName("license") val license: String? = null,
  @SerialName("language") val languages: JsonElement? = null,
  @SerialName("base_model") val baseModel: JsonElement? = null,
  @SerialName("datasets") val datasets: JsonElement? = null,
  @SerialName("pipeline_tag") val pipelineTag: String? = null,
  @SerialName("summary") val summary: String? = null,
  @SerialName("description") val description: String? = null,
)

@Serializable
data class ModelConfigDto(
  @SerialName("architectures") val architectures: List<String>? = null,
  @SerialName("model_type") val modelType: String? = null,
)

@Serializable
data class BaseModelRelationDto(
  @SerialName("relation") val relation: String? = null,
  @SerialName("models") val models: List<BaseModelRefDto>? = null,
)

@Serializable data class BaseModelRefDto(@SerialName("id") val id: String? = null)

@Serializable
data class ModelSiblingDto(
  @SerialName("rfilename") val filename: String? = null,
  @SerialName("size") val sizeBytes: Long? = null,
)
