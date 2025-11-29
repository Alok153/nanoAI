package com.vjaykrsna.nanoai.core.data.db.mappers

import com.vjaykrsna.nanoai.core.data.db.entities.ChatThreadEntity
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import java.util.UUID

/** Maps chat threads between Room entities and domain models. */
internal object ChatThreadMapper {
  /** Converts a [ChatThreadEntity] database entity to a [ChatThread] domain model. */
  fun toDomain(entity: ChatThreadEntity): ChatThread =
    ChatThread(
      threadId = UUID.fromString(entity.threadId),
      title = entity.title,
      personaId = entity.personaId?.let(UUID::fromString),
      activeModelId = entity.activeModelId,
      createdAt = entity.createdAt,
      updatedAt = entity.updatedAt,
      isArchived = entity.isArchived,
    )

  /** Converts a [ChatThread] domain model to a [ChatThreadEntity] database entity. */
  fun toEntity(domain: ChatThread): ChatThreadEntity =
    ChatThreadEntity(
      threadId = domain.threadId.toString(),
      title = domain.title,
      personaId = domain.personaId?.toString(),
      activeModelId = domain.activeModelId,
      createdAt = domain.createdAt,
      updatedAt = domain.updatedAt,
      isArchived = domain.isArchived,
    )
}

/** Extension function to convert entity to domain model. */
fun ChatThreadEntity.toDomain(): ChatThread = ChatThreadMapper.toDomain(this)

/** Extension function to convert domain model to entity. */
fun ChatThread.toEntity(): ChatThreadEntity = ChatThreadMapper.toEntity(this)
