package com.vjaykrsna.nanoai.feature.settings.data

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import com.vjaykrsna.nanoai.core.model.NanoAISuccess
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import com.vjaykrsna.nanoai.feature.settings.domain.PersonaUseCase
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultPersonaRepositoryTest {
  private val fakeDataSource = FakePersonaDataSource()
  private val repository = DefaultPersonaRepository(fakeDataSource)
  private val useCase = PersonaUseCase(repository)

  @Test
  fun observePersonas_emitsFromDataSource() = runTest {
    val persona = samplePersona()
    fakeDataSource.personas.value = listOf(persona)

    val observed = useCase.observePersonas().first()

    assertThat(observed).containsExactly(persona)
  }

  @Test
  fun switchPersona_delegatesToDataSource() = runTest {
    val personaId = UUID.randomUUID()

    val result = repository.switchPersona(personaId, PersonaSwitchAction.CONTINUE_THREAD)

    assertThat(fakeDataSource.switchedIds).containsExactly(personaId)
    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
  }

  @Test
  fun recordPersonaSwitch_tracksAction() = runTest {
    val personaId = UUID.randomUUID()
    val threadId = UUID.randomUUID()

    val result =
      useCase.recordPersonaSwitch(
        threadId = threadId,
        previousPersonaId = null,
        newPersonaId = personaId,
        action = PersonaSwitchAction.START_NEW_THREAD,
      )

    assertThat(fakeDataSource.loggedSwitches).hasSize(1)
    val logged = fakeDataSource.loggedSwitches.single()
    assertThat(logged.threadId).isEqualTo(threadId)
    assertThat(logged.newPersonaId).isEqualTo(personaId)
    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
  }

  private fun samplePersona(): PersonaProfile =
    PersonaProfile(
      personaId = UUID.randomUUID(),
      name = "Test Persona",
      description = "A persona for testing",
      systemPrompt = "You are helpful",
      defaultModelPreference = null,
      temperature = 0.7f,
      topP = 0.9f,
      defaultVoice = null,
      defaultImageStyle = null,
      createdAt = Instant.DISTANT_PAST,
      updatedAt = Instant.DISTANT_FUTURE,
    )
}

private class FakePersonaDataSource : PersonaDataSource {
  val personas = MutableStateFlow<List<PersonaProfile>>(emptyList())
  val switchedIds = mutableListOf<UUID>()
  val loggedSwitches = mutableListOf<PersonaSwitchLog>()

  override fun observePersonas() = personas

  override suspend fun getDefaultPersona(): NanoAIResult<PersonaProfile?> =
    NanoAIResult.success(personas.value.firstOrNull())

  override suspend fun switchPersona(
    personaId: UUID,
    action: PersonaSwitchAction,
  ): NanoAIResult<UUID> {
    switchedIds += personaId
    return NanoAIResult.success(personaId)
  }

  override suspend fun savePersona(persona: PersonaProfile): NanoAIResult<Unit> {
    personas.value = personas.value + persona
    return NanoAIResult.success(Unit)
  }

  override suspend fun deletePersona(personaId: UUID): NanoAIResult<Unit> {
    personas.value = personas.value.filterNot { it.personaId == personaId }
    return NanoAIResult.success(Unit)
  }

  override suspend fun recordPersonaSwitch(
    threadId: UUID,
    previousPersonaId: UUID?,
    newPersonaId: UUID,
    action: PersonaSwitchAction,
  ): NanoAIResult<Unit> {
    loggedSwitches += PersonaSwitchLog(threadId, previousPersonaId, newPersonaId, action)
    return NanoAIResult.success(Unit)
  }
}

private data class PersonaSwitchLog(
  val threadId: UUID?,
  val previousPersonaId: UUID?,
  val newPersonaId: UUID,
  val action: PersonaSwitchAction,
)
