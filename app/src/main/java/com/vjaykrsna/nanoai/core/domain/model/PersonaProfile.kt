package com.vjaykrsna.nanoai.core.domain.model

import com.vjaykrsna.nanoai.core.data.db.entities.PersonaProfileEntity
import kotlinx.datetime.Instant
import java.util.UUID

/**
 * Domain model for a persona profile.
 *
 * Clean architecture: Separate from database entities.
 * Used by repositories, use cases, ViewModels, and UI.
 */
data class PersonaProfile(
    val personaId: UUID,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val defaultModelPreference: String? = null,
    val temperature: Float = 1.0f,
    val topP: Float = 1.0f,
    val defaultVoice: String? = null,
    val defaultImageStyle: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * Extension function to convert entity to domain model.
 */
fun PersonaProfileEntity.toDomain(): PersonaProfile =
    PersonaProfile(
        personaId = UUID.fromString(personaId),
        name = name,
        description = description,
        systemPrompt = systemPrompt,
        defaultModelPreference = defaultModelPreference,
        temperature = temperature,
        topP = topP,
        defaultVoice = defaultVoice,
        defaultImageStyle = defaultImageStyle,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

/**
 * Extension function to convert domain model to entity.
 */
fun PersonaProfile.toEntity(): PersonaProfileEntity =
    PersonaProfileEntity(
        personaId = personaId.toString(),
        name = name,
        description = description,
        systemPrompt = systemPrompt,
        defaultModelPreference = defaultModelPreference,
        temperature = temperature,
        topP = topP,
        defaultVoice = defaultVoice,
        defaultImageStyle = defaultImageStyle,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
