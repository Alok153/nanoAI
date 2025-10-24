package com.vjaykrsna.nanoai.feature.chat.domain

import kotlinx.serialization.json.JsonObject

/** Configuration parameters for AI model inference and text generation. */
data class InferenceConfiguration(
  val systemPrompt: String? = null,
  val temperature: Float? = null,
  val topP: Float? = null,
  val maxOutputTokens: Int? = null,
  val localModelPreference: String? = null,
  val cloudModel: String? = null,
  val metadata: JsonObject? = null,
)
