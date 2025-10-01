package com.vjaykrsna.nanoai.core.domain.model

import com.vjaykrsna.nanoai.core.data.db.entities.MessageEntity
import com.vjaykrsna.nanoai.core.model.MessageSource
import com.vjaykrsna.nanoai.core.model.Role
import kotlinx.datetime.Instant
import java.util.UUID

/**
 * Domain model for a chat message.
 *
 * Clean architecture: Separate from database entities.
 * Used by repositories, use cases, ViewModels, and UI.
 */
data class Message(
    val messageId: UUID,
    val threadId: UUID,
    val role: Role,
    val text: String?,
    val audioUri: String? = null,
    val imageUri: String? = null,
    val source: MessageSource,
    val latencyMs: Long? = null,
    val createdAt: Instant,
    val errorCode: String? = null,
)

/**
 * Extension function to convert entity to domain model.
 */
fun MessageEntity.toDomain(): Message =
    Message(
        messageId = UUID.fromString(messageId),
        threadId = UUID.fromString(threadId),
        role = role,
        text = text,
        audioUri = audioUri,
        imageUri = imageUri,
        source = source,
        latencyMs = latencyMs,
        createdAt = createdAt,
        errorCode = errorCode,
    )

/**
 * Extension function to convert domain model to entity.
 */
fun Message.toEntity(): MessageEntity =
    MessageEntity(
        messageId = messageId.toString(),
        threadId = threadId.toString(),
        role = role,
        text = text,
        audioUri = audioUri,
        imageUri = imageUri,
        source = source,
        latencyMs = latencyMs,
        createdAt = createdAt,
        errorCode = errorCode,
    )
