package com.vjaykrsna.nanoai.core.domain.model

import com.vjaykrsna.nanoai.core.model.MessageRole
import com.vjaykrsna.nanoai.core.model.MessageSource
import java.util.UUID
import kotlinx.datetime.Instant

/**
 * Domain model for a chat message.
 *
 * Clean architecture: Separate from database entities. Used by repositories, use cases, ViewModels,
 * and UI. Mapping to/from entities is handled by
 * [com.vjaykrsna.nanoai.core.data.db.mappers.MessageMapper].
 */
data class Message(
  val messageId: UUID,
  val threadId: UUID,
  val role: MessageRole,
  val text: String?,
  val audioUri: String? = null,
  val imageUri: String? = null,
  val source: MessageSource,
  val latencyMs: Long? = null,
  val createdAt: Instant,
  val errorCode: String? = null,
)
