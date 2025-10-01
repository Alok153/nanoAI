package com.vjaykrsna.nanoai.core.domain.model

import com.vjaykrsna.nanoai.core.data.db.entities.PersonaSwitchLogEntity
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import kotlinx.datetime.Instant
import java.util.UUID

/**
 * Domain model for persona switch log entry.
 *
 * Clean architecture: Separate from database entities.
 * Used by repositories, use cases, ViewModels, and UI.
 */
data class PersonaSwitchLog(
    val logId: UUID,
    val threadId: UUID,
    val previousPersonaId: UUID?,
    val newPersonaId: UUID,
    val actionTaken: PersonaSwitchAction,
    val createdAt: Instant,
)

/**
 * Extension function to convert entity to domain model.
 */
fun PersonaSwitchLogEntity.toDomain(): PersonaSwitchLog =
    PersonaSwitchLog(
        logId = UUID.fromString(logId),
        threadId = UUID.fromString(threadId),
        previousPersonaId = previousPersonaId?.let(UUID::fromString),
        newPersonaId = UUID.fromString(newPersonaId),
        actionTaken = actionTaken,
        createdAt = createdAt,
    )

/**
 * Extension function to convert domain model to entity.
 */
fun PersonaSwitchLog.toEntity(): PersonaSwitchLogEntity =
    PersonaSwitchLogEntity(
        logId = logId.toString(),
        threadId = threadId.toString(),
        previousPersonaId = previousPersonaId?.toString(),
        newPersonaId = newPersonaId.toString(),
        actionTaken = actionTaken,
        createdAt = createdAt,
    )
