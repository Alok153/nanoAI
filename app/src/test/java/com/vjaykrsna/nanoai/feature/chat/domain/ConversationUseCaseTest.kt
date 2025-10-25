package com.vjaykrsna.nanoai.feature.chat.domain

import com.vjaykrsna.nanoai.core.data.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.model.MessageRole
import com.vjaykrsna.nanoai.core.model.MessageSource
import com.vjaykrsna.nanoai.testing.assertRecoverableError
import com.vjaykrsna.nanoai.testing.assertSuccess
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test

class ConversationUseCaseTest {
  private lateinit var useCase: ConversationUseCase
  private lateinit var conversationRepository: ConversationRepository

  @Before
  fun setup() {
    conversationRepository = mockk(relaxed = true)

    useCase = ConversationUseCase(conversationRepository)
  }

  @Test
  fun `getAllThreadsFlow returns flow from repository`() = runTest {
    val threads =
      listOf(
        ChatThread(
          threadId = UUID.randomUUID(),
          title = "Test Thread",
          personaId = UUID.randomUUID(),
          activeModelId = "gpt-3.5-turbo",
          createdAt = Instant.parse("2024-01-01T12:00:00Z"),
          updatedAt = Instant.parse("2024-01-01T12:05:00Z"),
          isArchived = false,
        )
      )
    val flow = flowOf(threads)
    every { conversationRepository.getAllThreadsFlow() } returns flow

    val result = useCase.getAllThreadsFlow()

    assert(result == flow)
  }

  @Test
  fun `getMessagesFlow returns flow from repository`() = runTest {
    val threadId = UUID.randomUUID()
    val messages =
      listOf(
        Message(
          messageId = UUID.randomUUID(),
          threadId = threadId,
          role = MessageRole.USER,
          text = "Hello",
          source = MessageSource.CLOUD_API,
          createdAt = Instant.parse("2024-01-01T12:00:00Z"),
        )
      )
    val flow = flowOf(messages)
    every { conversationRepository.getMessagesFlow(threadId) } returns flow

    val result = useCase.getMessagesFlow(threadId)

    assert(result == flow)
  }

  @Test
  fun `createNewThread returns success with thread ID when repository succeeds`() = runTest {
    val personaId = UUID.randomUUID()
    val title = "New Thread"
    val threadId = UUID.randomUUID()
    coEvery { conversationRepository.createNewThread(personaId, title) } returns threadId

    val result = useCase.createNewThread(personaId, title)

    val returnedThreadId = result.assertSuccess()
    assert(returnedThreadId == threadId)
  }

  @Test
  fun `createNewThread returns recoverable error when repository fails`() = runTest {
    val personaId = UUID.randomUUID()
    val title = "New Thread"
    val exception = RuntimeException("Database error")
    coEvery { conversationRepository.createNewThread(personaId, title) } throws exception

    val result = useCase.createNewThread(personaId, title)

    result.assertRecoverableError()
  }

  @Test
  fun `archiveThread returns success when repository succeeds`() = runTest {
    val threadId = UUID.randomUUID()
    coEvery { conversationRepository.archiveThread(threadId) } returns Unit

    val result = useCase.archiveThread(threadId)

    result.assertSuccess()
  }

  @Test
  fun `archiveThread returns recoverable error when repository fails`() = runTest {
    val threadId = UUID.randomUUID()
    val exception = RuntimeException("Database error")
    coEvery { conversationRepository.archiveThread(threadId) } throws exception

    val result = useCase.archiveThread(threadId)

    result.assertRecoverableError()
  }

  @Test
  fun `deleteThread returns success when repository succeeds`() = runTest {
    val threadId = UUID.randomUUID()
    coEvery { conversationRepository.deleteThread(threadId) } returns Unit

    val result = useCase.deleteThread(threadId)

    result.assertSuccess()
  }

  @Test
  fun `deleteThread returns recoverable error when repository fails`() = runTest {
    val threadId = UUID.randomUUID()
    val exception = RuntimeException("Database error")
    coEvery { conversationRepository.deleteThread(threadId) } throws exception

    val result = useCase.deleteThread(threadId)

    result.assertRecoverableError()
  }

  @Test
  fun `saveMessage returns success when repository succeeds`() = runTest {
    val message =
      Message(
        messageId = UUID.randomUUID(),
        threadId = UUID.randomUUID(),
        role = MessageRole.ASSISTANT,
        text = "Hello, how can I help you?",
        source = MessageSource.LOCAL_MODEL,
        latencyMs = 1500,
        createdAt = Instant.parse("2024-01-01T12:00:00Z"),
      )
    coEvery { conversationRepository.saveMessage(message) } returns Unit

    val result = useCase.saveMessage(message)

    result.assertSuccess()
  }

  @Test
  fun `saveMessage returns recoverable error when repository fails`() = runTest {
    val message =
      Message(
        messageId = UUID.randomUUID(),
        threadId = UUID.randomUUID(),
        role = MessageRole.ASSISTANT,
        text = "Hello, how can I help you?",
        source = MessageSource.LOCAL_MODEL,
        createdAt = Instant.parse("2024-01-01T12:00:00Z"),
      )
    val exception = RuntimeException("Database error")
    coEvery { conversationRepository.saveMessage(message) } throws exception

    val result = useCase.saveMessage(message)

    result.assertRecoverableError()
  }

  @Test
  fun `getCurrentPersonaForThread returns success with persona ID when found`() = runTest {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    coEvery { conversationRepository.getCurrentPersonaForThread(threadId) } returns personaId

    val result = useCase.getCurrentPersonaForThread(threadId)

    val returnedPersonaId = result.assertSuccess()
    assert(returnedPersonaId == personaId)
  }

  @Test
  fun `getCurrentPersonaForThread returns success with null when not found`() = runTest {
    val threadId = UUID.randomUUID()
    coEvery { conversationRepository.getCurrentPersonaForThread(threadId) } returns null

    val result = useCase.getCurrentPersonaForThread(threadId)

    val returnedPersonaId = result.assertSuccess()
    assert(returnedPersonaId == null)
  }

  @Test
  fun `getCurrentPersonaForThread returns recoverable error when repository fails`() = runTest {
    val threadId = UUID.randomUUID()
    val exception = RuntimeException("Database error")
    coEvery { conversationRepository.getCurrentPersonaForThread(threadId) } throws exception

    val result = useCase.getCurrentPersonaForThread(threadId)

    result.assertRecoverableError()
  }

  @Test
  fun `updateThreadPersona returns success when repository succeeds`() = runTest {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    coEvery { conversationRepository.updateThreadPersona(threadId, personaId) } returns Unit

    val result = useCase.updateThreadPersona(threadId, personaId)

    result.assertSuccess()
  }

  @Test
  fun `updateThreadPersona returns success when setting persona to null`() = runTest {
    val threadId = UUID.randomUUID()
    coEvery { conversationRepository.updateThreadPersona(threadId, null) } returns Unit

    val result = useCase.updateThreadPersona(threadId, null)

    result.assertSuccess()
  }

  @Test
  fun `updateThreadPersona returns recoverable error when repository fails`() = runTest {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    val exception = RuntimeException("Database error")
    coEvery { conversationRepository.updateThreadPersona(threadId, personaId) } throws exception

    val result = useCase.updateThreadPersona(threadId, personaId)

    result.assertRecoverableError()
  }
}
