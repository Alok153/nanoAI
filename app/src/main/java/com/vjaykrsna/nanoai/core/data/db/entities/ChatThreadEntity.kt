package com.vjaykrsna.nanoai.core.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/**
 * Room entity representing a chat conversation thread.
 *
 * A thread contains multiple messages and maintains context for a conversation with a specific
 * persona and model configuration.
 *
 * @property threadId Unique identifier (UUID string)
 * @property title Optional user-assigned title for the thread
 * @property personaId UUID of the default persona for this thread (nullable)
 * @property activeModelId Identifier of the model used for latest response (local or cloud)
 * @property createdAt Timestamp when thread was created
 * @property updatedAt Timestamp when thread was last modified
 * @property isArchived Whether the thread is archived (hidden from main list)
 */
@Entity(tableName = "chat_threads")
data class ChatThreadEntity(
  @PrimaryKey @ColumnInfo(name = "thread_id") val threadId: String,
  @ColumnInfo(name = "title") val title: String? = null,
  @ColumnInfo(name = "persona_id") val personaId: String? = null,
  @ColumnInfo(name = "active_model_id") val activeModelId: String,
  @ColumnInfo(name = "created_at") val createdAt: Instant,
  @ColumnInfo(name = "updated_at") val updatedAt: Instant,
  @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
)
