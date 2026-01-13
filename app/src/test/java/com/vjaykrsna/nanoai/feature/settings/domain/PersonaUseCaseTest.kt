package com.vjaykrsna.nanoai.feature.settings.domain

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.PersonaSwitchLog
import com.vjaykrsna.nanoai.core.domain.repository.PersonaSwitchLogRepository
import com.vjaykrsna.nanoai.core.model.NanoAISuccess
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import com.vjaykrsna.nanoai.feature.settings.data.CorePersonaDataSource
import com.vjaykrsna.nanoai.feature.settings.data.DefaultPersonaRepository
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import com.vjaykrsna.nanoai.testing.FakeConversationRepository
import com.vjaykrsna.nanoai.testing.FakePersonaRepository
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PersonaUseCaseTest {

  private lateinit var conversationRepository: FakeConversationRepository
  private lateinit var personaRepository: FakePersonaRepository
  private lateinit var logRepository: RecordingPersonaSwitchLogRepository
  private lateinit var personaUseCase: PersonaUseCase

  @BeforeEach
  fun setup() {
    conversationRepository = FakeConversationRepository()
    personaRepository = FakePersonaRepository()
    logRepository = RecordingPersonaSwitchLogRepository()
    val dataSource =
      CorePersonaDataSource(conversationRepository, personaRepository, logRepository, Clock.System)
    val repository = DefaultPersonaRepository(dataSource)
    personaUseCase = PersonaUseCase(repository)
  }

  @Test
  fun switchPersona_continuesThread_andLogsPreviousPersona() = runTest {
    val originalPersona = DomainTestBuilders.buildPersona(name = "Analyst")
    val targetPersona = DomainTestBuilders.buildPersona(name = "Creator")
    personaRepository.setPersonas(listOf(originalPersona, targetPersona))
    val threadId = UUID.randomUUID()
    conversationRepository.addThread(
      DomainTestBuilders.buildChatThread(threadId = threadId, personaId = originalPersona.personaId)
    )

    val result =
      personaUseCase.switchPersona(
        currentThreadId = threadId,
        personaId = targetPersona.personaId,
        action = PersonaSwitchAction.CONTINUE_THREAD,
      )

    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
    val updatedThread = conversationRepository.getThread(threadId)
    assertThat(updatedThread?.personaId).isEqualTo(targetPersona.personaId)
    val logged = logRepository.logged.single()
    assertThat(logged.threadId).isEqualTo(threadId)
    assertThat(logged.previousPersonaId).isEqualTo(originalPersona.personaId)
    assertThat(logged.newPersonaId).isEqualTo(targetPersona.personaId)
    assertThat(logged.actionTaken).isEqualTo(PersonaSwitchAction.CONTINUE_THREAD)
  }

  @Test
  fun switchPersona_startsNewThread_andLogsWithPreviousPersona() = runTest {
    val originalPersona = DomainTestBuilders.buildPersona(name = "Analyst")
    val targetPersona = DomainTestBuilders.buildPersona(name = "Creator")
    personaRepository.setPersonas(listOf(originalPersona, targetPersona))
    val threadId = UUID.randomUUID()
    conversationRepository.addThread(
      DomainTestBuilders.buildChatThread(threadId = threadId, personaId = originalPersona.personaId)
    )

    val result =
      personaUseCase.switchPersona(
        currentThreadId = threadId,
        personaId = targetPersona.personaId,
        action = PersonaSwitchAction.START_NEW_THREAD,
      )

    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
    val targetThreadId = (result as NanoAISuccess<UUID>).value
    assertThat(targetThreadId).isNotEqualTo(threadId)
    val newThread = conversationRepository.getThread(targetThreadId)
    assertThat(newThread?.personaId).isEqualTo(targetPersona.personaId)

    val logged = logRepository.logged.single()
    assertThat(logged.threadId).isEqualTo(targetThreadId)
    assertThat(logged.previousPersonaId).isEqualTo(originalPersona.personaId)
    assertThat(logged.newPersonaId).isEqualTo(targetPersona.personaId)
    assertThat(logged.actionTaken).isEqualTo(PersonaSwitchAction.START_NEW_THREAD)
  }

  private class RecordingPersonaSwitchLogRepository : PersonaSwitchLogRepository {
    val logged = mutableListOf<PersonaSwitchLog>()

    override suspend fun logSwitch(log: PersonaSwitchLog) {
      logged += log
    }

    override suspend fun getSwitchHistory(threadId: UUID): List<PersonaSwitchLog> =
      logged.filter { it.threadId == threadId }

    override suspend fun getLatestSwitch(threadId: UUID): PersonaSwitchLog? =
      logged.filter { it.threadId == threadId }.lastOrNull()

    override suspend fun getLogsByThreadId(
      threadId: UUID
    ): kotlinx.coroutines.flow.Flow<List<PersonaSwitchLog>> =
      kotlinx.coroutines.flow.flowOf(logged.filter { it.threadId == threadId })
  }
}
