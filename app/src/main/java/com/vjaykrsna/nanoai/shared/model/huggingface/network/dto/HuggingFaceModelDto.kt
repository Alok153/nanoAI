package com.vjaykrsna.nanoai.shared.model.huggingface.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Summary metadata for a Hugging Face model repository. */
@Serializable
data class HuggingFaceModelDto(
  @SerialName("modelId") val modelId: String,
  @SerialName("sha") val revisionSha: String? = null,
  @SerialName("siblings") val siblings: List<HuggingFaceSiblingDto> = emptyList(),
)

/** File entry returned alongside the model summary response. */
@Serializable
data class HuggingFaceSiblingDto(
  @SerialName("rfilename") val relativeFilename: String,
  @SerialName("size") val sizeBytes: Long? = null,
  @SerialName("lfs") val lfs: HuggingFaceLfsDto? = null,
  @SerialName("oid") val gitOid: String? = null,
  @SerialName("sha256") val sha256: String? = null,
)

/** Large File Storage (LFS) metadata associated with a sibling. */
@Serializable
data class HuggingFaceLfsDto(
  @SerialName("oid") val oid: String,
  @SerialName("size") val sizeBytes: Long? = null,
  @SerialName("sha256") val sha256: String? = null,
  @SerialName("sha512") val sha512: String? = null,
)

/** Detailed metadata for a single path resolved under a revision. */
@Serializable
data class HuggingFacePathInfoDto(
  @SerialName("path") val path: String,
  @SerialName("size") val sizeBytes: Long? = null,
  @SerialName("oid") val gitOid: String? = null,
  @SerialName("sha256") val sha256: String? = null,
  @SerialName("lfs") val lfs: HuggingFaceLfsDto? = null,
)

/** Entry returned by the repository tree endpoint. */
@Serializable
data class HuggingFaceTreeEntryDto(
  @SerialName("type") val type: String,
  @SerialName("path") val path: String,
  @SerialName("oid") val gitOid: String? = null,
  @SerialName("size") val sizeBytes: Long? = null,
  @SerialName("lfs") val lfs: HuggingFaceTreeLfsDto? = null,
)

/** LFS metadata nested under tree entries. */
@Serializable
data class HuggingFaceTreeLfsDto(
  @SerialName("oid") val oid: String,
  @SerialName("size") val sizeBytes: Long? = null,
)
