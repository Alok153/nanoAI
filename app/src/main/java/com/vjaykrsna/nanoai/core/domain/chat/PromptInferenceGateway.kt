package com.vjaykrsna.nanoai.core.domain.chat

import java.util.UUID

/**
 * Abstraction over inference execution so the domain layer does not depend on concrete runtime or
 * network clients.
 */
interface PromptInferenceGateway {
  /** True when the device has validated connectivity to online providers. */
  suspend fun isOnline(): Boolean

  /** True when at least one local model is fully installed and ready. */
  suspend fun hasLocalModelAvailable(): Boolean

  /**
   * Execute inference using the best available backend.
   *
   * @param prompt User prompt text.
   * @param personaId Optional persona context for telemetry.
   * @param configuration Tunable inference parameters.
   * @param attachments Optional multimodal payloads.
   */
  suspend fun generateResponse(
    prompt: String,
    personaId: UUID?,
    configuration: InferenceConfiguration,
    attachments: PromptAttachments,
  ): InferenceResult
}
