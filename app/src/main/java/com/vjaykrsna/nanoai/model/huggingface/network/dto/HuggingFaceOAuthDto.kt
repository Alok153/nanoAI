package com.vjaykrsna.nanoai.model.huggingface.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HuggingFaceDeviceCodeResponse(
  @SerialName("device_code") val deviceCode: String,
  @SerialName("user_code") val userCode: String,
  @SerialName("verification_uri") val verificationUri: String,
  @SerialName("verification_uri_complete") val verificationUriComplete: String? = null,
  @SerialName("expires_in") val expiresIn: Int,
  @SerialName("interval") val interval: Int? = null,
)

@Serializable
data class HuggingFaceTokenResponse(
  @SerialName("access_token") val accessToken: String,
  @SerialName("refresh_token") val refreshToken: String? = null,
  @SerialName("token_type") val tokenType: String = "Bearer",
  @SerialName("expires_in") val expiresIn: Long? = null,
  @SerialName("scope") val scope: String? = null,
)

@Serializable
data class HuggingFaceOAuthErrorResponse(
  @SerialName("error") val error: String,
  @SerialName("error_description") val errorDescription: String? = null,
)
