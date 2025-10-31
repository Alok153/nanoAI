package com.vjaykrsna.nanoai.feature.chat.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.model.MessageSource

/** Success data returned by [InferenceOrchestrator.generateResponse]. */
data class InferenceSuccessData(
  val text: String,
  val source: MessageSource,
  val latencyMs: Long,
  val metadata: Map<String, Any?> = emptyMap(),
)

/** Result wrapper returned by [InferenceOrchestrator.generateResponse]. */
typealias InferenceResult = NanoAIResult<InferenceSuccessData>
