package com.vjaykrsna.nanoai.core.domain.repository

import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Repository for persona profile management.
 *
 * Provides clean API for persona CRUD operations, abstracting database access through domain
 * models.
 */
interface PersonaRepository {
  /** Get all personas. */
  suspend fun getAllPersonas(): List<PersonaProfile>

  /** Get a specific persona by ID. */
  suspend fun getPersona(personaId: UUID): PersonaProfile?

  /** Observe a specific persona by ID. */
  suspend fun getPersonaById(personaId: UUID): kotlinx.coroutines.flow.Flow<PersonaProfile?>

  /** Create a new persona. */
  suspend fun createPersona(persona: PersonaProfile)

  /** Update an existing persona. */
  suspend fun updatePersona(persona: PersonaProfile)

  /** Delete a persona. */
  suspend fun deletePersona(personaId: UUID)

  /** Get the default persona (for new threads). Returns first persona if no default set. */
  suspend fun getDefaultPersona(): PersonaProfile?

  /** Observe all personas (reactive updates). */
  fun observeAllPersonas(): Flow<List<PersonaProfile>>
}
