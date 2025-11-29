package com.vjaykrsna.nanoai.core.domain.model

import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import java.util.UUID
import kotlinx.datetime.Instant

/**
 * Domain model for persona switch log entry.
 *
 * Clean architecture: Separate from database entities. Used by repositories, use cases, ViewModels,
 * and UI. Mapping to/from entities is handled by
 * [com.vjaykrsna.nanoai.core.data.db.mappers.PersonaSwitchLogMapper].
 */
data class PersonaSwitchLog(
  val logId: UUID,
  val threadId: UUID,
  val previousPersonaId: UUID?,
  val newPersonaId: UUID,
  val actionTaken: PersonaSwitchAction,
  val createdAt: Instant,
)
