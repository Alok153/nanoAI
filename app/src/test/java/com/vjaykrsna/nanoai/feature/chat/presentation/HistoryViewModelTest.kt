@file:Suppress("LargeClass")

package com.vjaykrsna.nanoai.feature.chat.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.usecase.GetConversationHistoryUseCase
import com.vjaykrsna.nanoai.feature.chat.domain.ConversationUseCase
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Unit tests for [HistoryViewModel].
 *
 * Covers loading states, thread operations, and error handling.
 */
class HistoryViewModelTest {

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension()

  private lateinit var getConversationHistoryUseCase: GetConversationHistoryUseCase
  private lateinit var conversationUseCase: ConversationUseCase
  private lateinit var viewModel: HistoryViewModel

  @BeforeEach
  fun setup() {
    getConversationHistoryUseCase = mockk(relaxed = true)
    conversationUseCase = mockk(relaxed = true)
    viewModel =
      HistoryViewModel(
        getConversationHistoryUseCase,
        conversationUseCase,
        mainDispatcherExtension.dispatcher,
      )
  }

  @Test
  fun `init loads threads automatically`() = runTest {
    val thread =
      DomainTestBuilders.buildChatThread(threadId = UUID.randomUUID(), title = "Test Thread")
    coEvery { getConversationHistoryUseCase() } returns NanoAIResult.success(listOf(thread))

    // Wait for init to complete
    viewModel.threads.test {
      val threads = awaitItem()
      assertThat(threads).hasSize(1)
      assertThat(threads.first().title).isEqualTo("Test Thread")
    }
  }

  @Test
  fun `loadThreads sets loading state during operation`() = runTest {
    viewModel.isLoading.test {
      assertThat(awaitItem()).isFalse() // Initial state

      viewModel.loadThreads()
      assertThat(awaitItem()).isTrue() // Loading starts
      assertThat(awaitItem()).isFalse() // Loading ends
    }
  }

  @Test
  fun `loadThreads updates threads on success`() = runTest {
    val thread =
      DomainTestBuilders.buildChatThread(threadId = UUID.randomUUID(), title = "Loaded Thread")
    coEvery { getConversationHistoryUseCase() } returns NanoAIResult.success(listOf(thread))

    viewModel.loadThreads()

    viewModel.threads.test {
      val threads = awaitItem()
      assertThat(threads).hasSize(1)
      assertThat(threads.first().title).isEqualTo("Loaded Thread")
    }
  }

  @Test
  fun `loadThreads emits LoadFailed error on failure`() = runTest {
    coEvery { getConversationHistoryUseCase() } returns
      NanoAIResult.recoverable(message = "Failed to load")

    viewModel.errors.test {
      viewModel.loadThreads()

      val error = awaitItem()
      assertThat(error).isInstanceOf(HistoryError.LoadFailed::class.java)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `archiveThread reloads threads on success`() = runTest {
    val threadId = UUID.randomUUID()
    val archivedThread =
      DomainTestBuilders.buildChatThread(
        threadId = threadId,
        title = "To Archive",
        isArchived = true,
      )
    coEvery { getConversationHistoryUseCase() } returns NanoAIResult.success(listOf(archivedThread))
    coEvery { conversationUseCase.archiveThread(threadId) } returns NanoAIResult.success(Unit)

    viewModel.archiveThread(threadId)

    // Should reload and show archived threads or not
    // Assuming getAllThreads returns all, including archived
    viewModel.threads.test {
      val threads = awaitItem()
      assertThat(threads).hasSize(1)
      assertThat(threads.first().isArchived).isTrue()
    }
  }

  @Test
  fun `archiveThread emits ArchiveFailed error on failure`() = runTest {
    val threadId = UUID.randomUUID()
    coEvery { conversationUseCase.archiveThread(threadId) } returns
      NanoAIResult.recoverable(message = "Failed")

    viewModel.errors.test {
      viewModel.archiveThread(threadId)

      val error = awaitItem()
      assertThat(error).isInstanceOf(HistoryError.ArchiveFailed::class.java)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `deleteThread reloads threads on success`() = runTest {
    val threadId = UUID.randomUUID()
    coEvery { getConversationHistoryUseCase() } returns NanoAIResult.success(emptyList())
    coEvery { conversationUseCase.deleteThread(threadId) } returns NanoAIResult.success(Unit)

    viewModel.deleteThread(threadId)

    viewModel.threads.test {
      val threads = awaitItem()
      assertThat(threads).isEmpty()
    }
  }

  @Test
  fun `deleteThread emits DeleteFailed error on failure`() = runTest {
    val threadId = UUID.randomUUID()
    coEvery { conversationUseCase.deleteThread(threadId) } returns
      NanoAIResult.recoverable(message = "Failed")

    viewModel.errors.test {
      viewModel.deleteThread(threadId)

      val error = awaitItem()
      assertThat(error).isInstanceOf(HistoryError.DeleteFailed::class.java)
      cancelAndIgnoreRemainingEvents()
    }
  }
}
