package com.vjaykrsna.nanoai.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.repository.ConversationRepository
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetConversationHistoryUseCaseTest {

  private val conversationRepository: ConversationRepository = mockk()
  private lateinit var useCase: GetConversationHistoryUseCase

  @BeforeEach
  fun setUp() {
    useCase = GetConversationHistoryUseCase(conversationRepository)
  }

  @Test
  fun `returns success when repository provides threads`() = runTest {
    val threads =
      listOf(
        DomainTestBuilders.buildChatThread(threadId = UUID.randomUUID(), title = "Thread 1"),
        DomainTestBuilders.buildChatThread(threadId = UUID.randomUUID(), title = "Thread 2"),
      )
    coEvery { conversationRepository.getAllThreads() } returns threads

    val result = useCase.invoke()

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    val success = result as NanoAIResult.Success
    assertThat(success.value).containsExactlyElementsIn(threads)
  }

  @Test
  fun `returns recoverable error when repository throws io exception`() = runTest {
    val exception = IOException("database unavailable")
    coEvery { conversationRepository.getAllThreads() } throws exception

    val result = useCase.invoke()

    assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
    val error = result as NanoAIResult.RecoverableError
    assertThat(error.cause).isEqualTo(exception)
    assertThat(error.message).contains("Failed to load conversation history")
  }
}
