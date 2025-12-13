package com.vjaykrsna.nanoai.feature.chat.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.chat.ConversationUseCase
import com.vjaykrsna.nanoai.core.domain.chat.PromptAttachments
import com.vjaykrsna.nanoai.core.domain.chat.SendPromptUseCase
import com.vjaykrsna.nanoai.core.domain.chat.SwitchPersonaUseCase
import com.vjaykrsna.nanoai.core.domain.library.Model
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityBannerState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.repository.ConnectivityRepository
import com.vjaykrsna.nanoai.core.domain.uiux.ConnectivityOperationsUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.GetDefaultPersonaUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.ObservePersonasUseCase
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import com.vjaykrsna.nanoai.feature.chat.domain.ChatFeatureCoordinator
import com.vjaykrsna.nanoai.feature.chat.domain.DefaultChatFeatureCoordinator
import com.vjaykrsna.nanoai.feature.chat.domain.LocalInferenceUseCase
import com.vjaykrsna.nanoai.feature.chat.domain.LocalModelCandidate
import com.vjaykrsna.nanoai.feature.chat.domain.LocalModelReadiness
import com.vjaykrsna.nanoai.feature.chat.model.LocalInferenceUiStatus
import com.vjaykrsna.nanoai.feature.chat.presentation.state.ChatUiState
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHostTestHarness
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import com.vjaykrsna.nanoai.testing.FakeConversationRepository
import com.vjaykrsna.nanoai.testing.FakePersonaRepository
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
  private lateinit var chatFeatureCoordinator: ChatFeatureCoordinator
  private lateinit var connectivityRepository: FakeConnectivityRepository
  private lateinit var connectivityOperationsUseCase: ConnectivityOperationsUseCase
  private lateinit var localInferenceUseCase: LocalInferenceUseCase
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
    every { modelCatalogUseCase.observeInstalledModels() } returns flowOf(emptyList())
    sendPromptUseCase = mockk(relaxed = true)
    switchPersonaUseCase = mockk(relaxed = true)
    connectivityRepository = FakeConnectivityRepository(mainDispatcherExtension.dispatcher)
    connectivityOperationsUseCase =
      ConnectivityOperationsUseCase(connectivityRepository, mainDispatcherExtension.dispatcher)
    localInferenceUseCase = mockk(relaxed = true)

    coEvery { sendPromptUseCase(any(), any(), any(), any()) } returns NanoAIResult.success(Unit)
    coEvery { switchPersonaUseCase(any(), any(), any()) } returns
      NanoAIResult.success(UUID.randomUUID())

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
      ChatViewModel(
        chatFeatureCoordinator,
        connectivityOperationsUseCase,
        localInferenceUseCase,
        mainDispatcher = mainDispatcherExtension.dispatcher,
        ioDispatcher = mainDispatcherExtension.dispatcher,
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
      viewModel.onComposerTextChanged("Test message")
      viewModel.onSendMessage()
      advanceUntilIdle()

      val event = awaitItem()
      assertThat(event).isInstanceOf(ChatUiEvent.ErrorRaised::class.java)
      val error = (event as ChatUiEvent.ErrorRaised).error
      assertThat(error).isInstanceOf(ChatError.ThreadCreationFailed::class.java)
      assertEnvelopeMirrorsPendingState(event)
      cancelAndIgnoreRemainingEvents()
    }

    coVerify(exactly = 0) { sendPromptUseCase(any(), any(), any(), any()) }
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

    viewModel.onComposerTextChanged("Test prompt")
    viewModel.onSendMessage()
    advanceUntilIdle()

    val messages = conversationRepository.getMessages(threadId)
    assertThat(messages).hasSize(1)
    assertThat(messages.first().text).isEqualTo("Test prompt")
    coVerify { sendPromptUseCase(threadId, "Test prompt", personaId, any()) }
  }

  @Test
  fun `sendMessage toggles loading indicator`() = runTest {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId, personaId = personaId)
    conversationRepository.addThread(thread)

    viewModel.selectThread(threadId)
    advanceUntilIdle()

    viewModel.onComposerTextChanged("Hello")
    viewModel.onSendMessage()
    advanceUntilIdle()

    assertThat(harness.currentState.isSendingMessage).isFalse()
    assertThat(harness.currentState.composerText).isEmpty()
  }

  @Test
  fun `sendMessage emits error when inference fails`() = runTest {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId, personaId = personaId)
    conversationRepository.addThread(thread)
    coEvery { sendPromptUseCase(any(), any(), any(), any()) } returns
      NanoAIResult.recoverable(message = "Failed")

    viewModel.selectThread(threadId)
    advanceUntilIdle()

    harness.testEvents {
      viewModel.onComposerTextChanged("Test")
      viewModel.onSendMessage()
      advanceUntilIdle()

      val event = awaitItem()
      assertThat(event).isInstanceOf(ChatUiEvent.ErrorRaised::class.java)
      val error = (event as ChatUiEvent.ErrorRaised).error
      assertThat(error).isInstanceOf(ChatError.InferenceFailed::class.java)
      assertEnvelopeMirrorsPendingState(event)
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

      val event = awaitItem() as ChatUiEvent.ErrorRaised
      val error = event.error
      assertThat(error).isInstanceOf(ChatError.PersonaSwitchFailed::class.java)
      assertEnvelopeMirrorsPendingState(event)
    }
  }

  @Test
  fun `sendMessage emits error when persona missing`() = runTest {
    val threadId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId, personaId = null)
    conversationRepository.addThread(thread)

    viewModel.selectThread(threadId)
    advanceUntilIdle()

    harness.testEvents {
      viewModel.onComposerTextChanged("Hello")
      viewModel.onSendMessage()
      advanceUntilIdle()

      val event = awaitItem() as ChatUiEvent.ErrorRaised
      val error = event.error
      assertThat(error).isInstanceOf(ChatError.PersonaSelectionFailed::class.java)
      assertEnvelopeMirrorsPendingState(event)
    }
  }

  @Test
  fun `showModelPicker toggles visibility`() = runTest {
    assertThat(harness.currentState.isModelPickerVisible).isFalse()

    viewModel.showModelPicker()

    assertThat(harness.currentState.isModelPickerVisible).isTrue()

    viewModel.dismissModelPicker()

    assertThat(harness.currentState.isModelPickerVisible).isFalse()
  }

  @Test
  fun `attachments update when media provided`() = runTest {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId, personaId = personaId)
    conversationRepository.addThread(thread)
    viewModel.selectThread(threadId)
    advanceUntilIdle()

    val bitmap = mockk<android.graphics.Bitmap>(relaxed = true)
    val audioData = byteArrayOf(1, 2, 3)

    viewModel.onImageSelected(bitmap)
    viewModel.onAudioRecorded(audioData, "audio/wav")

    val attachments = harness.currentState.attachments
    assertThat(attachments.image).isNotNull()
    assertThat(attachments.image?.bitmap).isEqualTo(bitmap)
    assertThat(attachments.audio).isNotNull()
    assertThat(attachments.audio?.data).isEqualTo(audioData)
    assertThat(attachments.audio?.mimeType).isEqualTo("audio/wav")
  }

  @Test
  fun `successful send clears attachments`() = runTest {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId, personaId = personaId)
    conversationRepository.addThread(thread)
    viewModel.selectThread(threadId)
    advanceUntilIdle()

    val bitmap = mockk<android.graphics.Bitmap>(relaxed = true)
    val audioData = byteArrayOf(5, 6, 7)

    viewModel.onImageSelected(bitmap)
    viewModel.onAudioRecorded(audioData, "audio/wav")
    viewModel.onComposerTextChanged("Prompt")
    viewModel.onSendMessage()
    advanceUntilIdle()

    val attachments = harness.currentState.attachments
    assertThat(attachments.image).isNull()
    assertThat(attachments.audio).isNull()
  }

  @Test
  fun `sendMessage converts attachments before sending`() = runTest {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId, personaId = personaId)
    conversationRepository.addThread(thread)
    viewModel.selectThread(threadId)
    advanceUntilIdle()

    val attachmentsSlot = slot<PromptAttachments>()
    coEvery { sendPromptUseCase(any(), any(), any(), capture(attachmentsSlot)) } returns
      NanoAIResult.success(Unit)

    val bitmap = mockk<android.graphics.Bitmap>(relaxed = true)
    every { bitmap.width } returns 4
    every { bitmap.height } returns 4
    every { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, any(), any()) } answers
      {
        val stream = thirdArg<java.io.OutputStream>()
        stream.write(byteArrayOf(1, 2, 3, 4))
        true
      }

    val audioData = byteArrayOf(9, 9, 9)
    viewModel.onImageSelected(bitmap)
    viewModel.onAudioRecorded(audioData, "audio/wav")
    viewModel.onComposerTextChanged("Prompt with media")

    viewModel.onSendMessage()
    advanceUntilIdle()

    val captured = attachmentsSlot.captured
    assertThat(captured.image).isNotNull()
    assertThat(captured.image?.bytes).isNotEmpty()
    assertThat(captured.image?.mimeType).isEqualTo("image/png")
    assertThat(captured.audio?.bytes).isEqualTo(audioData)
    assertThat(captured.audio?.mimeType).isEqualTo("audio/wav")
  }

  @Test
  fun `clearPendingError removes pending message`() = runTest {
    viewModel.onComposerTextChanged("Hello")
    viewModel.onSendMessage()
    advanceUntilIdle()

    assertThat(harness.currentState.pendingErrorMessage).isNotNull()

    viewModel.clearPendingError()

    assertThat(harness.currentState.pendingErrorMessage).isNull()
  }

  @Test
  fun `sendMessage surfaces error when saving message fails`() = runTest {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId, personaId = personaId)
    conversationRepository.addThread(thread)
    conversationRepository.shouldFailOnSaveMessage = true

    viewModel.selectThread(threadId)
    advanceUntilIdle()

    harness.testEvents {
      viewModel.onComposerTextChanged("Prompt")
      viewModel.onSendMessage()
      advanceUntilIdle()

      val event = awaitItem() as ChatUiEvent.ErrorRaised
      val error = event.error
      assertThat(error).isInstanceOf(ChatError.UnexpectedError::class.java)
      assertEnvelopeMirrorsPendingState(event)
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(harness.currentState.isSendingMessage).isFalse()
    assertThat(harness.currentState.pendingErrorMessage).isNotNull()
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

      val event = awaitItem() as ChatUiEvent.ErrorRaised
      val error = event.error
      assertThat(error).isInstanceOf(ChatError.ThreadArchiveFailed::class.java)
      assertEnvelopeMirrorsPendingState(event)
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

  @Test
  fun `offline connectivity selects ready local model`() = runTest {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    val thread =
      DomainTestBuilders.buildChatThread(
        threadId = threadId,
        personaId = personaId,
        activeModelId = "cloud-only",
      )
    val persona = DomainTestBuilders.buildPersona(personaId = personaId, name = "Analyst")
    conversationRepository.addThread(thread)
    personaRepository.setPersonas(listOf(persona))

    val readiness =
      LocalModelReadiness.Ready(
        candidate =
          LocalModelCandidate(
            modelId = "local-ready",
            displayName = "Phoenix",
            providerType = ProviderType.MEDIA_PIPE,
            installState = InstallState.INSTALLED,
            sizeBytes = 0,
          ),
        autoSelected = true,
      )
    coEvery { localInferenceUseCase.prepareForOffline(any(), any()) } returns readiness

    viewModel.selectThread(threadId)
    advanceUntilIdle()

    connectivityRepository.emit(ConnectivityStatus.OFFLINE)
    advanceUntilIdle()

    val updatedThread = conversationRepository.getAllThreads().first()
    assertThat(updatedThread.activeModelId).isEqualTo("local-ready")
    val localState = harness.currentState.localInferenceUi
    assertThat(localState.status).isInstanceOf(LocalInferenceUiStatus.OfflineReady::class.java)
    assertThat(harness.currentState.connectivityBanner?.status)
      .isEqualTo(ConnectivityStatus.OFFLINE)
  }

  @Test
  fun `offline connectivity without local model surfaces missing state`() = runTest {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId, personaId = personaId)
    val persona = DomainTestBuilders.buildPersona(personaId = personaId, name = "Creator")
    conversationRepository.addThread(thread)
    personaRepository.setPersonas(listOf(persona))

    coEvery { localInferenceUseCase.prepareForOffline(any(), any()) } returns
      LocalModelReadiness.Missing(LocalModelReadiness.MissingReason.NO_LOCAL_MODELS)

    viewModel.selectThread(threadId)
    advanceUntilIdle()

    connectivityRepository.emit(ConnectivityStatus.OFFLINE)
    advanceUntilIdle()

    val localState = harness.currentState.localInferenceUi
    assertThat(localState.status).isInstanceOf(LocalInferenceUiStatus.OfflineMissing::class.java)
    assertThat(conversationRepository.getAllThreads().first().activeModelId)
      .isEqualTo(thread.activeModelId)
  }

  private fun assertEnvelopeMirrorsPendingState(event: ChatUiEvent.ErrorRaised) {
    assertThat(event.envelope.userMessage).isEqualTo(harness.currentState.pendingErrorMessage)
  }
}

private class FakeConnectivityRepository(
  override val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
) : ConnectivityRepository {
  private val bannerState =
    MutableStateFlow(ConnectivityBannerState(status = ConnectivityStatus.ONLINE))

  override val connectivityBannerState: Flow<ConnectivityBannerState> = bannerState

  override suspend fun updateConnectivity(status: ConnectivityStatus) {
    bannerState.value = ConnectivityBannerState(status = status)
  }

  fun emit(status: ConnectivityStatus) {
    bannerState.value = ConnectivityBannerState(status = status)
  }
}
