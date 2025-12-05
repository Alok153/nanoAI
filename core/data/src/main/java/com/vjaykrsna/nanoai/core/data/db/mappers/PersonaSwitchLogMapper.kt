package com.vjaykrsna.nanoai.core.data.db.mappers

import com.vjaykrsna.nanoai.core.data.db.entities.PersonaSwitchLogEntity
import com.vjaykrsna.nanoai.core.domain.model.PersonaSwitchLog
import java.util.UUID

/** Maps persona switch logs between Room entities and domain models. */
internal object PersonaSwitchLogMapper {
  /** Converts a [PersonaSwitchLogEntity] database entity to a [PersonaSwitchLog] domain model. */
  fun toDomain(entity: PersonaSwitchLogEntity): PersonaSwitchLog =
    PersonaSwitchLog(
      logId = UUID.fromString(entity.logId),
      threadId = UUID.fromString(entity.threadId),
      previousPersonaId = entity.previousPersonaId?.let(UUID::fromString),
      newPersonaId = UUID.fromString(entity.newPersonaId),
      actionTaken = entity.actionTaken,
      createdAt = entity.createdAt,
    )

  /** Converts a [PersonaSwitchLog] domain model to a [PersonaSwitchLogEntity] database entity. */
  fun toEntity(domain: PersonaSwitchLog): PersonaSwitchLogEntity =
    PersonaSwitchLogEntity(
      logId = domain.logId.toString(),
      threadId = domain.threadId.toString(),
      previousPersonaId = domain.previousPersonaId?.toString(),
      newPersonaId = domain.newPersonaId.toString(),
      actionTaken = domain.actionTaken,
      createdAt = domain.createdAt,
    )
}

/** Extension function to convert entity to domain model. */
fun PersonaSwitchLogEntity.toDomain(): PersonaSwitchLog = PersonaSwitchLogMapper.toDomain(this)

/** Extension function to convert domain model to entity. */
fun PersonaSwitchLog.toEntity(): PersonaSwitchLogEntity = PersonaSwitchLogMapper.toEntity(this)
