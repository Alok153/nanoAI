package com.vjaykrsna.nanoai.shared.testing

import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.domain.repository.PersonaRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [PersonaRepository] for instrumentation testing. Maintains in-memory state
 * for personas.
 */
class FakePersonaRepository : PersonaRepository {
  private val _personas = MutableStateFlow<List<PersonaProfile>>(emptyList())

  var shouldFailOnCreate = false
  var shouldFailOnUpdate = false
  var shouldFailOnDelete = false

  fun setPersonas(personas: List<PersonaProfile>) {
    _personas.value = personas
  }

  fun clearAll() {
    _personas.value = emptyList()
    shouldFailOnCreate = false
    shouldFailOnUpdate = false
    shouldFailOnDelete = false
  }

  override suspend fun getAllPersonas(): List<PersonaProfile> = _personas.value

  override suspend fun getPersona(personaId: UUID): PersonaProfile? =
    _personas.value.firstOrNull { it.personaId == personaId }

  override suspend fun getPersonaById(personaId: UUID): Flow<PersonaProfile?> {
    val persona = _personas.value.firstOrNull { it.personaId == personaId }
    return MutableStateFlow(persona)
  }

  override suspend fun createPersona(persona: PersonaProfile) {
    if (shouldFailOnCreate) {
      error("Failed to create persona")
    }
    _personas.value += persona
  }

  override suspend fun updatePersona(persona: PersonaProfile) {
    if (shouldFailOnUpdate) {
      error("Failed to update persona")
    }
    _personas.value = _personas.value.map { if (it.personaId == persona.personaId) persona else it }
  }

  override suspend fun deletePersona(personaId: UUID) {
    if (shouldFailOnDelete) {
      error("Failed to delete persona")
    }
    _personas.value = _personas.value.filterNot { it.personaId == personaId }
  }

  override suspend fun getDefaultPersona(): PersonaProfile? = _personas.value.firstOrNull()

  override fun observeAllPersonas(): Flow<List<PersonaProfile>> = _personas
}
