package com.vjaykrsna.nanoai.shared.model.huggingface.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Authenticated user profile metadata returned from Hugging Face whoami endpoint. */
@Serializable
data class HuggingFaceUserDto(
  @SerialName("name") val name: String,
  @SerialName("displayName") val displayName: String? = null,
  @SerialName("email") val email: String? = null,
  @SerialName("type") val accountType: String? = null,
  @SerialName("avatarUrl") val avatarUrl: String? = null,
)
