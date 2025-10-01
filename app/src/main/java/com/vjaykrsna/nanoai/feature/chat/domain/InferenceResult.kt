package com.vjaykrsna.nanoai.feature.chat.domain

import com.vjaykrsna.nanoai.core.model.MessageSource

/**
 * Result wrapper returned by [InferenceOrchestrator.generateResponse].
 */
sealed class InferenceResult {
    data class Success(
        val text: String,
        val source: MessageSource,
        val latencyMs: Long,
        val metadata: Map<String, Any?> = emptyMap()
    ) : InferenceResult()

    data class Error(
        val errorCode: String,
        val message: String? = null,
        val cause: Throwable? = null,
        val metadata: Map<String, Any?> = emptyMap()
    ) : InferenceResult()
}
