package com.vjaykrsna.nanoai.feature.settings.domain

import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Persona-specific repository contract for the Settings feature. */
interface PersonaRepository {
  fun observePersonas(): Flow<List<PersonaProfile>>

  suspend fun getDefaultPersona(): NanoAIResult<PersonaProfile?>

  suspend fun switchPersona(
    currentThreadId: UUID?,
    personaId: UUID,
    action: PersonaSwitchAction,
  ): NanoAIResult<UUID>

  suspend fun savePersona(persona: PersonaProfile): NanoAIResult<Unit>

  suspend fun deletePersona(personaId: UUID): NanoAIResult<Unit>

  suspend fun recordPersonaSwitch(
    threadId: UUID,
    previousPersonaId: UUID?,
    newPersonaId: UUID,
    action: PersonaSwitchAction,
  ): NanoAIResult<Unit>
}

/** Use case wrapper for persona operations. */
class PersonaUseCase @Inject constructor(private val repository: PersonaRepository) {

  fun observePersonas(): Flow<List<PersonaProfile>> = repository.observePersonas()

  suspend fun getDefaultPersona(): NanoAIResult<PersonaProfile?> = repository.getDefaultPersona()

  suspend fun switchPersona(
    currentThreadId: UUID?,
    personaId: UUID,
    action: PersonaSwitchAction,
  ): NanoAIResult<UUID> = repository.switchPersona(currentThreadId, personaId, action)

  suspend fun savePersona(persona: PersonaProfile): NanoAIResult<Unit> =
    repository.savePersona(persona)

  suspend fun deletePersona(personaId: UUID): NanoAIResult<Unit> =
    repository.deletePersona(personaId)

  suspend fun recordPersonaSwitch(
    threadId: UUID,
    previousPersonaId: UUID?,
    newPersonaId: UUID,
    action: PersonaSwitchAction,
  ): NanoAIResult<Unit> =
    repository.recordPersonaSwitch(threadId, previousPersonaId, newPersonaId, action)
}
