package com.vjaykrsna.nanoai.feature.chat.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.chat.ConversationUseCase
import com.vjaykrsna.nanoai.core.domain.chat.SendPromptUseCase
import com.vjaykrsna.nanoai.core.domain.chat.SwitchPersonaUseCase
import com.vjaykrsna.nanoai.core.domain.library.Model
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.GetDefaultPersonaUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.ObservePersonasUseCase
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import com.vjaykrsna.nanoai.feature.chat.presentation.state.ChatUiState
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHostTestHarness
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import com.vjaykrsna.nanoai.testing.FakeConversationRepository
import com.vjaykrsna.nanoai.testing.FakePersonaRepository
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ChatViewModelTest {

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension()

  private lateinit var conversationUseCase: ConversationUseCase
  private lateinit var conversationRepository: FakeConversationRepository
  private lateinit var personaRepository: FakePersonaRepository
  private lateinit var observePersonasUseCase: ObservePersonasUseCase
  private lateinit var getDefaultPersonaUseCase: GetDefaultPersonaUseCase
  private lateinit var modelCatalogUseCase: ModelCatalogUseCase
  private lateinit var sendPromptUseCase: SendPromptUseCase
  private lateinit var switchPersonaUseCase: SwitchPersonaUseCase
  private lateinit var viewModel: ChatViewModel
  private lateinit var harness: ViewModelStateHostTestHarness<ChatUiState, ChatUiEvent>

  @BeforeEach
  fun setup() {
    conversationRepository = FakeConversationRepository()
    conversationUseCase = ConversationUseCase(conversationRepository)
    personaRepository = FakePersonaRepository()
    observePersonasUseCase = ObservePersonasUseCase(personaRepository)
    getDefaultPersonaUseCase = GetDefaultPersonaUseCase(personaRepository)
    modelCatalogUseCase = mockk(relaxed = true)
    sendPromptUseCase = mockk(relaxed = true)
    switchPersonaUseCase = mockk(relaxed = true)

    coEvery { sendPromptUseCase(any(), any(), any(), any(), any()) } returns
      NanoAIResult.success(Unit)
    coEvery { switchPersonaUseCase(any(), any(), any()) } returns
      NanoAIResult.success(UUID.randomUUID())

    viewModel =
      ChatViewModel(
        sendPromptUseCase,
        switchPersonaUseCase,
        conversationUseCase,
        observePersonasUseCase,
        getDefaultPersonaUseCase,
        modelCatalogUseCase,
        mainDispatcher = mainDispatcherExtension.dispatcher,
      )
    harness = ViewModelStateHostTestHarness(viewModel)
  }

  @Test
  fun `selectThread updates active thread and messages`() = runTest {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId, personaId = personaId)
    val message1 = DomainTestBuilders.buildUserMessage(threadId = threadId, text = "Hello")
    val message2 = DomainTestBuilders.buildAssistantMessage(threadId = threadId, text = "Hi there")
    conversationRepository.addThread(thread)
    conversationRepository.addMessage(threadId, message1)
    conversationRepository.addMessage(threadId, message2)

    viewModel.selectThread(threadId)
    advanceUntilIdle()

    assertThat(harness.currentState.activeThreadId).isEqualTo(threadId)
    val stateWithMessages = harness.awaitState(predicate = { it.messages.size == 2 })
    assertThat(stateWithMessages.messages.map { it.text }).containsExactly("Hello", "Hi there")
  }

  @Test
  fun `sendMessage with no active thread emits error`() = runTest {
    val personaId = UUID.randomUUID()

    harness.testEvents {
      viewModel.sendMessage("Test message", personaId)
      advanceUntilIdle()

      val event = awaitItem()
      assertThat(event).isInstanceOf(ChatUiEvent.ErrorRaised::class.java)
      val error = (event as ChatUiEvent.ErrorRaised).error
      assertThat(error).isInstanceOf(ChatError.ThreadCreationFailed::class.java)
      cancelAndIgnoreRemainingEvents()
    }

    coVerify(exactly = 0) { sendPromptUseCase(any(), any(), any(), any(), any()) }
    assertThat(harness.currentState.isSendingMessage).isFalse()
  }

  @Test
  fun `sendMessage saves user message and calls use case`() = runTest {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId, personaId = personaId)
    conversationRepository.addThread(thread)

    viewModel.selectThread(threadId)
    advanceUntilIdle()

    viewModel.sendMessage("Test prompt", personaId)
    advanceUntilIdle()

    val messages = conversationRepository.getMessages(threadId)
    assertThat(messages).hasSize(1)
    assertThat(messages.first().text).isEqualTo("Test prompt")
    coVerify { sendPromptUseCase(threadId, "Test prompt", personaId, any(), any()) }
  }

  @Test
  fun `sendMessage toggles loading indicator`() = runTest {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId, personaId = personaId)
    conversationRepository.addThread(thread)

    viewModel.selectThread(threadId)
    advanceUntilIdle()

    viewModel.sendMessage("Hello", personaId)
    advanceUntilIdle()

    assertThat(harness.currentState.isSendingMessage).isFalse()
  }

  @Test
  fun `sendMessage emits error when inference fails`() = runTest {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId, personaId = personaId)
    conversationRepository.addThread(thread)
    coEvery { sendPromptUseCase(any(), any(), any(), any(), any()) } returns
      NanoAIResult.recoverable(message = "Failed")

    viewModel.selectThread(threadId)
    advanceUntilIdle()

    harness.testEvents {
      viewModel.sendMessage("Test", personaId)
      advanceUntilIdle()

      val event = awaitItem()
      assertThat(event).isInstanceOf(ChatUiEvent.ErrorRaised::class.java)
      val error = (event as ChatUiEvent.ErrorRaised).error
      assertThat(error).isInstanceOf(ChatError.InferenceFailed::class.java)
    }
  }

  @Test
  fun `switchPersona updates active thread on START_NEW_THREAD`() = runTest {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    val newThreadId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId, personaId = personaId)
    conversationRepository.addThread(thread)
    coEvery { switchPersonaUseCase(any(), any(), any()) } returns NanoAIResult.success(newThreadId)

    viewModel.selectThread(threadId)
    advanceUntilIdle()

    viewModel.switchPersona(UUID.randomUUID(), PersonaSwitchAction.START_NEW_THREAD)
    advanceUntilIdle()

    assertThat(harness.currentState.activeThreadId).isEqualTo(newThreadId)
  }

  @Test
  fun `switchPersona emits error on failure`() = runTest {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId, personaId = personaId)
    conversationRepository.addThread(thread)
    coEvery { switchPersonaUseCase(any(), any(), any()) } returns
      NanoAIResult.recoverable(message = "Switch failed")

    viewModel.selectThread(threadId)
    advanceUntilIdle()

    harness.testEvents {
      viewModel.switchPersona(UUID.randomUUID(), PersonaSwitchAction.START_NEW_THREAD)
      advanceUntilIdle()

      val event = awaitItem()
      val error = (event as ChatUiEvent.ErrorRaised).error
      assertThat(error).isInstanceOf(ChatError.PersonaSwitchFailed::class.java)
    }
  }

  @Test
  fun `createNewThread selects created thread`() = runTest {
    val persona = DomainTestBuilders.buildPersona()
    personaRepository.setPersonas(listOf(persona))

    viewModel.createNewThread(persona.personaId, "Test Thread")
    advanceUntilIdle()

    assertThat(harness.currentState.activeThreadId).isNotNull()
    assertThat(conversationRepository.getAllThreads()).hasSize(1)
  }

  @Test
  fun `archiveThread clears active thread and surfaces error`() = runTest {
    val threadId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId)
    conversationRepository.addThread(thread)
    conversationRepository.shouldFailOnArchiveThread = true

    harness.testEvents {
      viewModel.archiveThread(threadId)
      advanceUntilIdle()

      val event = awaitItem()
      val error = (event as ChatUiEvent.ErrorRaised).error
      assertThat(error).isInstanceOf(ChatError.ThreadArchiveFailed::class.java)
    }
  }

  @Test
  fun `deleteThread removes thread and clears selection`() = runTest {
    val threadId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId)
    conversationRepository.addThread(thread)

    viewModel.selectThread(threadId)
    advanceUntilIdle()

    viewModel.deleteThread(threadId)
    advanceUntilIdle()

    assertThat(conversationRepository.getAllThreads()).isEmpty()
    assertThat(harness.currentState.activeThreadId).isNull()
  }

  @Test
  fun `selectModel emits snackbar event`() = runTest {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId, personaId = personaId)
    conversationRepository.addThread(thread)
    viewModel.selectThread(threadId)
    advanceUntilIdle()

    val model = Model(modelId = "model-1", displayName = "Local", size = 1L, parameter = "test")

    harness.testEvents {
      viewModel.selectModel(model)
      val event = awaitItem()
      assertThat(event).isInstanceOf(ChatUiEvent.ModelSelected::class.java)
    }
  }
}
