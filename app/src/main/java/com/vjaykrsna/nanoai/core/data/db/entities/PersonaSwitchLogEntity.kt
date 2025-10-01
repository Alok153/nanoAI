package com.vjaykrsna.nanoai.core.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import kotlinx.datetime.Instant

/**
 * Room entity tracking persona switch history for chat threads.
 * 
 * Logs when users switch between personas, enabling analytics and
 * context awareness for the UI (e.g., showing switch timeline).
 *
 * @property logId Unique identifier (UUID string)
 * @property threadId Parent thread where switch occurred
 * @property previousPersonaId Previous persona ID (null if first persona in thread)
 * @property newPersonaId New persona ID being switched to
 * @property actionTaken Whether thread was continued or started anew
 * @property createdAt Timestamp when switch occurred
 */
@Entity(
    tableName = "persona_switch_logs",
    foreignKeys = [
        ForeignKey(
            entity = ChatThreadEntity::class,
            parentColumns = ["thread_id"],
            childColumns = ["thread_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["thread_id"]),
        Index(value = ["new_persona_id"])
    ]
)
data class PersonaSwitchLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "log_id")
    val logId: String,

    @ColumnInfo(name = "thread_id")
    val threadId: String,

    @ColumnInfo(name = "previous_persona_id")
    val previousPersonaId: String? = null,

    @ColumnInfo(name = "new_persona_id")
    val newPersonaId: String,

    @ColumnInfo(name = "action_taken")
    val actionTaken: PersonaSwitchAction,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant
)
