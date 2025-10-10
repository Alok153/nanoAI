package com.vjaykrsna.nanoai.model.huggingface.network

import com.vjaykrsna.nanoai.model.huggingface.network.dto.HuggingFaceModelDto
import com.vjaykrsna.nanoai.model.huggingface.network.dto.HuggingFacePathInfoDto
import com.vjaykrsna.nanoai.model.huggingface.network.dto.HuggingFaceTreeEntryDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit definitions for Hugging Face model metadata APIs. */
interface HuggingFaceService {
  /** Retrieve repository summary including sibling file metadata. */
  @GET("api/models/{modelId}")
  suspend fun getModelSummary(
    @Path(value = "modelId", encoded = true) modelId: String,
  ): HuggingFaceModelDto

  /**
   * Resolve metadata for specific file paths under the provided revision.
   *
   * The `paths` query parameter should be provided without a leading slash.
   */
  @GET("api/models/{modelId}/paths-info/{revision}")
  suspend fun getPathsInfo(
    @Path(value = "modelId", encoded = true) modelId: String,
    @Path(value = "revision", encoded = true) revision: String,
    @Query("paths") paths: String,
  ): List<HuggingFacePathInfoDto>

  /** Lightweight tree listing to resolve file metadata when path info is unavailable. */
  @GET("api/models/{modelId}/tree/{revision}")
  suspend fun getTree(
    @Path(value = "modelId", encoded = true) modelId: String,
    @Path(value = "revision", encoded = true) revision: String,
    @Query("path") path: String,
    @Query("recursive") recursive: Boolean = false,
  ): List<HuggingFaceTreeEntryDto>
}
