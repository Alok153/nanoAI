package com.vjaykrsna.nanoai.core.data.db.mappers

import com.vjaykrsna.nanoai.core.data.db.entities.PersonaProfileEntity
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import java.util.UUID

/** Maps persona profiles between Room entities and domain models. */
internal object PersonaProfileMapper {
  /** Converts a [PersonaProfileEntity] database entity to a [PersonaProfile] domain model. */
  fun toDomain(entity: PersonaProfileEntity): PersonaProfile =
    PersonaProfile(
      personaId = UUID.fromString(entity.personaId),
      name = entity.name,
      description = entity.description,
      systemPrompt = entity.systemPrompt,
      defaultModelPreference = entity.defaultModelPreference,
      temperature = entity.temperature,
      topP = entity.topP,
      defaultVoice = entity.defaultVoice,
      defaultImageStyle = entity.defaultImageStyle,
      createdAt = entity.createdAt,
      updatedAt = entity.updatedAt,
    )

  /** Converts a [PersonaProfile] domain model to a [PersonaProfileEntity] database entity. */
  fun toEntity(domain: PersonaProfile): PersonaProfileEntity =
    PersonaProfileEntity(
      personaId = domain.personaId.toString(),
      name = domain.name,
      description = domain.description,
      systemPrompt = domain.systemPrompt,
      defaultModelPreference = domain.defaultModelPreference,
      temperature = domain.temperature,
      topP = domain.topP,
      defaultVoice = domain.defaultVoice,
      defaultImageStyle = domain.defaultImageStyle,
      createdAt = domain.createdAt,
      updatedAt = domain.updatedAt,
    )
}

/** Extension function to convert entity to domain model. */
fun PersonaProfileEntity.toDomain(): PersonaProfile = PersonaProfileMapper.toDomain(this)

/** Extension function to convert domain model to entity. */
fun PersonaProfile.toEntity(): PersonaProfileEntity = PersonaProfileMapper.toEntity(this)
