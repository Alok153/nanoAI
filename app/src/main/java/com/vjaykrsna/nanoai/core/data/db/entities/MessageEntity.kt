package com.vjaykrsna.nanoai.core.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vjaykrsna.nanoai.core.model.MessageSource
import com.vjaykrsna.nanoai.core.model.Role
import kotlinx.datetime.Instant

/**
 * Room entity representing a single message in a chat thread.
 * 
 * Messages are ordered by creation timestamp and belong to a specific thread.
 * Foreign key cascade ensures messages are deleted when their thread is deleted.
 *
 * @property messageId Unique identifier (UUID string)
 * @property threadId Parent thread identifier (foreign key)
 * @property role Role of message author (USER, ASSISTANT, SYSTEM)
 * @property text Main text content of the message
 * @property audioUri Optional URI to audio attachment (future feature)
 * @property imageUri Optional URI to image attachment (future feature)
 * @property source Source of assistant response (LOCAL_MODEL or CLOUD_API)
 * @property latencyMs Measured inference duration in milliseconds (nullable)
 * @property createdAt Timestamp when message was created
 * @property errorCode Optional error code if message generation failed
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatThreadEntity::class,
            parentColumns = ["thread_id"],
            childColumns = ["thread_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["thread_id", "created_at"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    val messageId: String,

    @ColumnInfo(name = "thread_id")
    val threadId: String,

    @ColumnInfo(name = "role")
    val role: Role,

    @ColumnInfo(name = "text")
    val text: String?,

    @ColumnInfo(name = "audio_uri")
    val audioUri: String? = null,

    @ColumnInfo(name = "image_uri")
    val imageUri: String? = null,

    @ColumnInfo(name = "source")
    val source: MessageSource,

    @ColumnInfo(name = "latency_ms")
    val latencyMs: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "error_code")
    val errorCode: String? = null
)
