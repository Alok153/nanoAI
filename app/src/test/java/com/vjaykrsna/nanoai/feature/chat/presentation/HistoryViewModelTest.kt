package com.vjaykrsna.nanoai.feature.chat.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.chat.ConversationUseCase
import com.vjaykrsna.nanoai.feature.chat.presentation.state.HistoryUiState
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHostTestHarness
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import com.vjaykrsna.nanoai.testing.FakeConversationRepository
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class HistoryViewModelTest {

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension()

  private lateinit var conversationRepository: FakeConversationRepository
  private lateinit var conversationUseCase: ConversationUseCase
  private lateinit var viewModel: HistoryViewModel
  private lateinit var harness: ViewModelStateHostTestHarness<HistoryUiState, HistoryUiEvent>

  @BeforeEach
  fun setup() {
    conversationRepository = FakeConversationRepository()
    conversationUseCase = ConversationUseCase(conversationRepository)
    viewModel = HistoryViewModel(conversationUseCase, mainDispatcherExtension.dispatcher)
    harness = ViewModelStateHostTestHarness(viewModel)
  }

  @Test
  fun `init loads threads automatically`() = runTest {
    val thread =
      DomainTestBuilders.buildChatThread(threadId = UUID.randomUUID(), title = "Test Thread")
    conversationRepository.addThread(thread)

    advanceUntilIdle()

    val state = harness.awaitState(predicate = { it.threads.isNotEmpty() })
    assertThat(state.threads).hasSize(1)
    assertThat(state.threads.first().title).isEqualTo("Test Thread")
  }

  @Test
  fun `loadThreads toggles loading state`() = runTest {
    val loadingState = async { harness.awaitState(predicate = { it.isLoading }) }
    viewModel.loadThreads()

    assertThat(loadingState.await().isLoading).isTrue()
    advanceUntilIdle()
    assertThat(harness.currentState.isLoading).isFalse()
  }

  @Test
  fun `loadThreads surfaces errors with envelope`() = runTest {
    conversationRepository.shouldFailOnGetAllThreads = true

    var emittedMessage: String? = null
    harness.testEvents {
      viewModel.loadThreads()
      val event = awaitItem()
      val envelope = (event as HistoryUiEvent.ErrorRaised).error
      emittedMessage = envelope.userMessage
      assertThat(envelope.userMessage).contains("Unable to load chat history")
      assertThat(envelope.context["operation"]).isEqualTo("loadThreads")
    }

    assertThat(emittedMessage).isNotNull()
    assertThat(harness.currentState.lastErrorMessage).isEqualTo(emittedMessage)
  }

  @Test
  fun `archiveThread marks thread archived`() = runTest {
    val threadId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId, isArchived = false)
    conversationRepository.addThread(thread)

    viewModel.archiveThread(threadId)
    advanceUntilIdle()

    harness.awaitState(predicate = { state -> state.threads.none { it.threadId == threadId } })
  }

  @Test
  fun `archiveThread surfaces ArchiveFailed envelope with context`() = runTest {
    conversationRepository.shouldFailOnArchiveThread = true
    val threadId = UUID.randomUUID()

    var emittedMessage: String? = null
    harness.testEvents {
      viewModel.archiveThread(threadId)
      val event = awaitItem()
      val envelope = (event as HistoryUiEvent.ErrorRaised).error
      emittedMessage = envelope.userMessage
      assertThat(envelope.userMessage).contains("Failed to archive thread")
      assertThat(envelope.context["threadId"]).isEqualTo(threadId.toString())
    }

    assertThat(emittedMessage).isNotNull()
    assertThat(harness.currentState.lastErrorMessage).isEqualTo(emittedMessage)
  }

  @Test
  fun `deleteThread removes thread`() = runTest {
    val threadId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId)
    conversationRepository.addThread(thread)

    viewModel.deleteThread(threadId)
    advanceUntilIdle()

    assertThat(harness.currentState.threads).isEmpty()
  }

  @Test
  fun `deleteThread surfaces DeleteFailed envelope with context`() = runTest {
    conversationRepository.shouldFailOnDeleteThread = true
    val threadId = UUID.randomUUID()

    var emittedMessage: String? = null
    harness.testEvents {
      viewModel.deleteThread(threadId)
      val event = awaitItem()
      val envelope = (event as HistoryUiEvent.ErrorRaised).error
      emittedMessage = envelope.userMessage
      assertThat(envelope.userMessage).contains("Failed to delete thread")
      assertThat(envelope.context["threadId"]).isEqualTo(threadId.toString())
    }

    assertThat(emittedMessage).isNotNull()
    assertThat(harness.currentState.lastErrorMessage).isEqualTo(emittedMessage)
  }
}
