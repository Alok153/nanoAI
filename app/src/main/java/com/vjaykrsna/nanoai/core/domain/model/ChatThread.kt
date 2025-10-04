package com.vjaykrsna.nanoai.core.domain.model

import com.vjaykrsna.nanoai.core.data.db.entities.ChatThreadEntity
import java.util.UUID
import kotlinx.datetime.Instant

/**
 * Domain model for a chat conversation thread.
 *
 * Clean architecture: Separate from database entities. Used by repositories, use cases, ViewModels,
 * and UI.
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

/** Extension function to convert entity to domain model. */
fun ChatThreadEntity.toDomain(): ChatThread =
  ChatThread(
    threadId = UUID.fromString(threadId),
    title = title,
    personaId = personaId?.let(UUID::fromString),
    activeModelId = activeModelId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isArchived = isArchived,
  )

/** Extension function to convert domain model to entity. */
fun ChatThread.toEntity(): ChatThreadEntity =
  ChatThreadEntity(
    threadId = threadId.toString(),
    title = title,
    personaId = personaId?.toString(),
    activeModelId = activeModelId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isArchived = isArchived,
  )
