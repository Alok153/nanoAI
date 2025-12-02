package com.vjaykrsna.nanoai.core.domain.chat

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.model.MessageSource

/** Success data returned by [PromptInferenceGateway.generateResponse]. */
data class InferenceSuccessData(
  val text: String,
  val source: MessageSource,
  val latencyMs: Long,
  val metadata: Map<String, Any?> = emptyMap(),
)

/** Result wrapper returned by [PromptInferenceGateway.generateResponse]. */
typealias InferenceResult = NanoAIResult<InferenceSuccessData>
