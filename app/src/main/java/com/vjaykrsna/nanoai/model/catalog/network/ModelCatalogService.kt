package com.vjaykrsna.nanoai.model.catalog.network

import com.vjaykrsna.nanoai.model.catalog.network.dto.ManifestVerificationRequestDto
import com.vjaykrsna.nanoai.model.catalog.network.dto.ManifestVerificationResponseDto
import com.vjaykrsna.nanoai.model.catalog.network.dto.ModelManifestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit definition for model manifest catalog operations. */
interface ModelCatalogService {
  @GET("catalog/models/{modelId}/manifest")
  suspend fun getModelManifest(
    @Path("modelId") modelId: String,
    @Query("version") version: String,
  ): ModelManifestDto

  @POST("catalog/models/{modelId}/verify")
  suspend fun verifyModelPackage(
    @Path("modelId") modelId: String,
    @Body request: ManifestVerificationRequestDto,
  ): ManifestVerificationResponseDto
}
