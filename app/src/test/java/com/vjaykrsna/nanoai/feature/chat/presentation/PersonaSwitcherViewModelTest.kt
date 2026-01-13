package com.vjaykrsna.nanoai.feature.chat.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.chat.ConversationUseCase
import com.vjaykrsna.nanoai.core.domain.chat.SendPromptUseCase
import com.vjaykrsna.nanoai.core.domain.chat.SwitchPersonaUseCase
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.model.PersonaSwitchLog
import com.vjaykrsna.nanoai.core.domain.repository.PersonaSwitchLogRepository
import com.vjaykrsna.nanoai.core.domain.usecase.GetDefaultPersonaUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.ObservePersonasUseCase
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import com.vjaykrsna.nanoai.feature.chat.domain.ChatFeatureCoordinator
import com.vjaykrsna.nanoai.feature.chat.domain.DefaultChatFeatureCoordinator
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHostTestHarness
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import com.vjaykrsna.nanoai.testing.FakeConversationRepository
import com.vjaykrsna.nanoai.testing.FakePersonaRepository
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class PersonaSwitcherViewModelTest {

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension()

  private lateinit var conversationRepository: FakeConversationRepository
  private lateinit var personaRepository: FakePersonaRepository
  private lateinit var logRepository: RecordingPersonaSwitchLogRepository
  private lateinit var switchPersonaUseCase: SwitchPersonaUseCase
  private lateinit var chatFeatureCoordinator: ChatFeatureCoordinator
  private lateinit var viewModel: PersonaSwitcherViewModel
  private lateinit var harness:
    ViewModelStateHostTestHarness<PersonaSwitcherUiState, PersonaSwitcherEvent>

  @BeforeEach
  fun setup() {
    conversationRepository = FakeConversationRepository()
    personaRepository = FakePersonaRepository()
    logRepository = RecordingPersonaSwitchLogRepository()
    switchPersonaUseCase = SwitchPersonaUseCase(conversationRepository, logRepository)

    val observePersonasUseCase = ObservePersonasUseCase(personaRepository)
    val getDefaultPersonaUseCase = GetDefaultPersonaUseCase(personaRepository)
    val conversationUseCase = ConversationUseCase(conversationRepository)
    val sendPromptUseCase = mockk<SendPromptUseCase>(relaxed = true)
    val modelCatalogUseCase = mockk<ModelCatalogUseCase>(relaxed = true)
    every { modelCatalogUseCase.observeInstalledModels() } returns flowOf(emptyList())

    chatFeatureCoordinator =
      DefaultChatFeatureCoordinator(
        sendPromptUseCase,
        switchPersonaUseCase,
        conversationUseCase,
        observePersonasUseCase,
        getDefaultPersonaUseCase,
        modelCatalogUseCase,
      )

    viewModel =
      PersonaSwitcherViewModel(
        chatFeatureCoordinator,
        dispatcher = mainDispatcherExtension.dispatcher,
      )
    harness = ViewModelStateHostTestHarness(viewModel)
  }

  @Test
  fun switchPersona_continueThread_updatesSelectionAndEmitsEvent() = runTest {
    val personaA = DomainTestBuilders.buildPersona(name = "Analyst")
    val personaB = DomainTestBuilders.buildPersona(name = "Creator")
    personaRepository.setPersonas(listOf(personaA, personaB))
    val threadId = UUID.randomUUID()
    conversationRepository.addThread(
      DomainTestBuilders.buildChatThread(threadId = threadId, personaId = personaA.personaId)
    )

    viewModel.setActiveThread(threadId, personaA.personaId)
    advanceUntilIdle()

    harness.testEvents {
      viewModel.switchPersona(personaB.personaId, PersonaSwitchAction.CONTINUE_THREAD)
      advanceUntilIdle()

      val event = awaitItem() as PersonaSwitcherEvent.SwitchCompleted
      assertThat(event.targetThreadId).isEqualTo(threadId)
      val updatedThread = conversationRepository.getThread(threadId)
      assertThat(updatedThread?.personaId).isEqualTo(personaB.personaId)
      assertThat(harness.currentState.isSwitching).isFalse()
      assertThat(harness.currentState.selectedPersonaId).isEqualTo(personaB.personaId)
    }
  }

  @Test
  fun switchPersona_startNewThread_createsThreadAndEmitsEvent() = runTest {
    val personaA = DomainTestBuilders.buildPersona(name = "Analyst")
    val personaB = DomainTestBuilders.buildPersona(name = "Creator")
    personaRepository.setPersonas(listOf(personaA, personaB))
    val threadId = UUID.randomUUID()
    conversationRepository.addThread(
      DomainTestBuilders.buildChatThread(threadId = threadId, personaId = personaA.personaId)
    )

    viewModel.setActiveThread(threadId, personaA.personaId)
    advanceUntilIdle()

    harness.testEvents {
      viewModel.switchPersona(personaB.personaId, PersonaSwitchAction.START_NEW_THREAD)
      advanceUntilIdle()

      val event = awaitItem() as PersonaSwitcherEvent.SwitchCompleted
      assertThat(event.targetThreadId).isNotEqualTo(threadId)
      val newThread = conversationRepository.getThread(event.targetThreadId)
      assertThat(newThread?.personaId).isEqualTo(personaB.personaId)
      assertThat(logRepository.logged).hasSize(1)
      assertThat(logRepository.logged.first().actionTaken)
        .isEqualTo(PersonaSwitchAction.START_NEW_THREAD)
    }
  }

  private class RecordingPersonaSwitchLogRepository : PersonaSwitchLogRepository {
    val logged = mutableListOf<PersonaSwitchLog>()

    override suspend fun logSwitch(log: PersonaSwitchLog) {
      logged += log
    }

    override suspend fun getSwitchHistory(threadId: UUID) =
      logged.filter { it.threadId == threadId }

    override suspend fun getLatestSwitch(threadId: UUID): PersonaSwitchLog? =
      logged.filter { it.threadId == threadId }.lastOrNull()

    override suspend fun getLogsByThreadId(
      threadId: UUID
    ): kotlinx.coroutines.flow.Flow<List<PersonaSwitchLog>> =
      kotlinx.coroutines.flow.flowOf(logged.filter { it.threadId == threadId })
  }
}
