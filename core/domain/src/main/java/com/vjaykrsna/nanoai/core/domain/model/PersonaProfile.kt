package com.vjaykrsna.nanoai.core.domain.model

import java.util.UUID
import kotlinx.datetime.Instant

/**
 * Domain model for a persona profile.
 *
 * Clean architecture: Separate from database entities. Used by repositories, use cases, ViewModels,
 * and UI. Mapping to/from entities is handled by
 * [com.vjaykrsna.nanoai.core.data.db.mappers.PersonaProfileMapper].
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
