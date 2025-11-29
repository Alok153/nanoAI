package com.vjaykrsna.nanoai.core.data.db.mappers

import com.vjaykrsna.nanoai.core.data.db.entities.MessageEntity
import com.vjaykrsna.nanoai.core.domain.model.Message
import java.util.UUID

/** Maps chat messages between Room entities and domain models. */
internal object MessageMapper {
  /** Converts a [MessageEntity] database entity to a [Message] domain model. */
  fun toDomain(entity: MessageEntity): Message =
    Message(
      messageId = UUID.fromString(entity.messageId),
      threadId = UUID.fromString(entity.threadId),
      role = entity.role,
      text = entity.text,
      audioUri = entity.audioUri,
      imageUri = entity.imageUri,
      source = entity.source,
      latencyMs = entity.latencyMs,
      createdAt = entity.createdAt,
      errorCode = entity.errorCode,
    )

  /** Converts a [Message] domain model to a [MessageEntity] database entity. */
  fun toEntity(domain: Message): MessageEntity =
    MessageEntity(
      messageId = domain.messageId.toString(),
      threadId = domain.threadId.toString(),
      role = domain.role,
      text = domain.text,
      audioUri = domain.audioUri,
      imageUri = domain.imageUri,
      source = domain.source,
      latencyMs = domain.latencyMs,
      createdAt = domain.createdAt,
      errorCode = domain.errorCode,
    )
}

/** Extension function to convert entity to domain model. */
fun MessageEntity.toDomain(): Message = MessageMapper.toDomain(this)

/** Extension function to convert domain model to entity. */
fun Message.toEntity(): MessageEntity = MessageMapper.toEntity(this)
