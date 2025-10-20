package com.vjaykrsna.nanoai.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

private const val MIN_TEMPERATURE = 0.0
private const val MAX_TEMPERATURE = 2.0
private const val MIN_TOP_P = 0.0
private const val MAX_TOP_P = 1.0
private const val MIN_COMPLETION_TOKENS = 1
private const val MAX_COMPLETION_TOKENS = 8_192

/** Data transfer objects for the cloud completion API based on contracts/llm-gateway.yaml. */
@Serializable
data class CompletionRequestDto(
  val model: String,
  val messages: List<CompletionMessageDto>,
  val temperature: Double? = null,
  @SerialName("top_p") val topP: Double? = null,
  @SerialName("max_tokens") val maxTokens: Int? = null,
  val stream: Boolean = false,
  val metadata: JsonObject? = null,
) {
  init {
    require(messages.isNotEmpty()) { "messages must not be empty" }
    require(temperature == null || (temperature in MIN_TEMPERATURE..MAX_TEMPERATURE)) {
      "temperature must be between $MIN_TEMPERATURE and $MAX_TEMPERATURE"
    }
    require(topP == null || (topP in MIN_TOP_P..MAX_TOP_P)) {
      "top_p must be between $MIN_TOP_P and $MAX_TOP_P"
    }
    require(maxTokens == null || maxTokens in MIN_COMPLETION_TOKENS..MAX_COMPLETION_TOKENS) {
      "max_tokens must be between $MIN_COMPLETION_TOKENS and $MAX_COMPLETION_TOKENS"
    }
  }
}

@Serializable data class CompletionMessageDto(val role: CompletionRole, val content: String)

/** Limited role enum for completion messages. */
@Serializable
enum class CompletionRole {
  @SerialName("system") SYSTEM,
  @SerialName("user") USER,
  @SerialName("assistant") ASSISTANT,
}

@Serializable
data class CompletionResponseDto(
  val id: String,
  val created: Long,
  val model: String? = null,
  val choices: List<CompletionChoiceDto>,
  val usage: CompletionUsageDto? = null,
)

@Serializable
data class CompletionChoiceDto(
  val index: Int,
  val message: CompletionChoiceMessageDto,
  @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class CompletionChoiceMessageDto(
  val role: CompletionResponseRole = CompletionResponseRole.ASSISTANT,
  val content: String,
)

/** Response message role (assistant only per contract). */
@Serializable
enum class CompletionResponseRole {
  @SerialName("assistant") ASSISTANT
}

@Serializable
data class CompletionUsageDto(
  @SerialName("prompt_tokens") val promptTokens: Int,
  @SerialName("completion_tokens") val completionTokens: Int,
  @SerialName("total_tokens") val totalTokens: Int,
)

/** Generic error payload returned by the gateway for structured errors. */
@Serializable
data class GatewayErrorDto(
  val error: JsonElement? = null,
  val message: String? = null,
  val code: String? = null,
)
