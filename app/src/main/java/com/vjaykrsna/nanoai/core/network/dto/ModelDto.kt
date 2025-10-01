package com.vjaykrsna.nanoai.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs for the GET /v1/models endpoint.
 */
@Serializable
data class ModelListResponseDto(
    val data: List<RemoteModelDto>
)

@Serializable
data class RemoteModelDto(
    val id: String,
    val provider: String,
    val capabilities: List<String>? = null,
    @SerialName("input_formats") val inputFormats: List<String>? = null,
    @SerialName("output_formats") val outputFormats: List<String>? = null,
    @SerialName("context_window") val contextWindow: Int? = null
)
