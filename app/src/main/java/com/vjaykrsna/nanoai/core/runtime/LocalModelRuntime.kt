package com.vjaykrsna.nanoai.core.runtime

import com.vjaykrsna.nanoai.core.domain.model.ModelPackage

/** Abstraction for local on-device model execution. */
interface LocalModelRuntime {
  /** Returns true if the given model is ready for inference (downloaded + loadable). */
  suspend fun isModelReady(modelId: String): Boolean

  /** Returns true if any of the provided models can be executed locally. */
  suspend fun hasReadyModel(models: List<ModelPackage>): Boolean

  /** Execute a generation request using the specified model. */
  suspend fun generate(request: LocalGenerationRequest): Result<LocalGenerationResult>
}

/** Request payload for running local inference. */
data class LocalGenerationRequest(
  val modelId: String,
  val prompt: String,
  val systemPrompt: String? = null,
  val temperature: Float? = null,
  val topP: Float? = null,
  val maxOutputTokens: Int? = null,
)

/** Result payload from local inference. */
data class LocalGenerationResult(
  val text: String,
  val latencyMs: Long,
  val metadata: Map<String, Any?> = emptyMap(),
)
