package com.vjaykrsna.nanoai.core.network

import com.vjaykrsna.nanoai.core.network.dto.CompletionRequestDto
import com.vjaykrsna.nanoai.core.network.dto.CompletionResponseDto
import com.vjaykrsna.nanoai.core.network.dto.ModelListResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit definition for the nanoAI cloud gateway API.
 */
interface CloudGatewayService {

    @POST("v1/completions")
    suspend fun createCompletion(
        @Body request: CompletionRequestDto
    ): CompletionResponseDto

    @GET("v1/models")
    suspend fun listModels(): ModelListResponseDto
}
