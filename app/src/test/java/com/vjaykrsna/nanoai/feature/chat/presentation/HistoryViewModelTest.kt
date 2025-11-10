package com.vjaykrsna.nanoai.feature.chat.presentation

import com.google.common.truth.Truth.assertThat
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
  private lateinit var viewModel: HistoryViewModel
  private lateinit var harness: ViewModelStateHostTestHarness<HistoryUiState, HistoryUiEvent>

  @BeforeEach
  fun setup() {
    conversationRepository = FakeConversationRepository()
    viewModel = HistoryViewModel(conversationRepository, mainDispatcherExtension.dispatcher)
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
  fun `loadThreads surfaces errors`() = runTest {
    conversationRepository.shouldFailOnGetAllThreads = true

    harness.testEvents {
      viewModel.loadThreads()
      val event = awaitItem()
      val error = (event as HistoryUiEvent.ErrorRaised).error
      assertThat(error).isInstanceOf(HistoryError.LoadFailed::class.java)
    }
  }

  @Test
  fun `archiveThread marks thread archived`() = runTest {
    val threadId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(threadId = threadId, isArchived = false)
    conversationRepository.addThread(thread)

    viewModel.archiveThread(threadId)
    advanceUntilIdle()

    val state =
      harness.awaitState(
        predicate = { state ->
          state.threads.firstOrNull { it.threadId == threadId }?.isArchived == true
        }
      )
    assertThat(state.threads.first { it.threadId == threadId }.isArchived).isTrue()
  }

  @Test
  fun `archiveThread surfaces ArchiveFailed`() = runTest {
    conversationRepository.shouldFailOnArchiveThread = true

    harness.testEvents {
      viewModel.archiveThread(UUID.randomUUID())
      val event = awaitItem()
      val error = (event as HistoryUiEvent.ErrorRaised).error
      assertThat(error).isInstanceOf(HistoryError.ArchiveFailed::class.java)
    }
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
  fun `deleteThread surfaces DeleteFailed`() = runTest {
    conversationRepository.shouldFailOnDeleteThread = true

    harness.testEvents {
      viewModel.deleteThread(UUID.randomUUID())
      val event = awaitItem()
      val error = (event as HistoryUiEvent.ErrorRaised).error
      assertThat(error).isInstanceOf(HistoryError.DeleteFailed::class.java)
    }
  }
}
