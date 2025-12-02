package com.vjaykrsna.nanoai.core.domain.model

import java.util.UUID
import kotlinx.datetime.Instant

/**
 * Domain model for a chat conversation thread.
 *
 * Clean architecture: Separate from database entities. Used by repositories, use cases, ViewModels,
 * and UI. Mapping to/from entities is handled by
 * [com.vjaykrsna.nanoai.core.data.db.mappers.ChatThreadMapper].
 */
data class ChatThread(
  val threadId: UUID,
  val title: String?,
  val personaId: UUID?,
  val activeModelId: String,
  val createdAt: Instant,
  val updatedAt: Instant,
  val isArchived: Boolean = false,
)
