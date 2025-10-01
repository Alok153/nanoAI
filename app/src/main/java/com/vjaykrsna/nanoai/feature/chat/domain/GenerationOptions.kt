package com.vjaykrsna.nanoai.feature.chat.domain

import kotlinx.serialization.json.JsonObject

/**
 * Optional persona/model tuning parameters supplied to the orchestrator.
 */
data class GenerationOptions(
    val systemPrompt: String? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val maxOutputTokens: Int? = null,
    val localModelPreference: String? = null,
    val cloudModel: String? = null,
    val metadata: JsonObject? = null,
)
