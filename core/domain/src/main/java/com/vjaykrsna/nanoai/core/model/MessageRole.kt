package com.vjaykrsna.nanoai.core.model

/**
 * Role of a message participant in a chat conversation. Maps to OpenAI chat completion API roles.
 */
enum class MessageRole {
  /** Message from the user/human. */
  USER,

  /** Message from the AI assistant. */
  ASSISTANT,

  /** System instruction/prompt (typically hidden from UI). */
  SYSTEM,
}
