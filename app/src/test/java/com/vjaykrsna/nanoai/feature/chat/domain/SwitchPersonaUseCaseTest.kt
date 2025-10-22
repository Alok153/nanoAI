package com.vjaykrsna.nanoai.feature.chat.domain

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.data.repository.PersonaSwitchLogRepository
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import com.vjaykrsna.nanoai.testing.assertRecoverableError
import com.vjaykrsna.nanoai.testing.assertSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SwitchPersonaUseCaseTest {
  private lateinit var useCase: SwitchPersonaUseCase
  private lateinit var conversationRepository: ConversationRepository
  private lateinit var personaSwitchLogRepository: PersonaSwitchLogRepository

  private val threadId = UUID.randomUUID()
  private val newPersonaId = UUID.randomUUID()
  private val previousPersonaId = UUID.randomUUID()

  @Before
  fun setup() {
    conversationRepository = mockk<ConversationRepository>(relaxed = true)
    personaSwitchLogRepository = mockk<PersonaSwitchLogRepository>(relaxed = true)

    useCase =
      SwitchPersonaUseCase(
        conversationRepository = conversationRepository,
        personaSwitchLogRepository = personaSwitchLogRepository,
      )
  }

  @Test
  fun `invoke continues thread and returns original threadId when CONTINUE_THREAD action`() =
    runTest {
      // Given
      coEvery { conversationRepository.getCurrentPersonaForThread(threadId) } returns
        previousPersonaId

      // When
      val result = useCase(threadId, newPersonaId, PersonaSwitchAction.CONTINUE_THREAD)

      // Then
      val returnedThreadId = result.assertSuccess()
      assertThat(returnedThreadId).isEqualTo(threadId)
      coVerify { conversationRepository.updateThreadPersona(threadId, newPersonaId) }
      coVerify { personaSwitchLogRepository.logSwitch(any()) }
    }

  @Test
  fun `invoke creates new thread and returns new threadId when START_NEW_THREAD action`() =
    runTest {
      // Given
      val newThreadId = UUID.randomUUID()
      coEvery { conversationRepository.getCurrentPersonaForThread(threadId) } returns
        previousPersonaId
      coEvery { conversationRepository.createNewThread(newPersonaId) } returns newThreadId

      // When
      val result = useCase(threadId, newPersonaId, PersonaSwitchAction.START_NEW_THREAD)

      // Then
      val returnedThreadId = result.assertSuccess()
      assertThat(returnedThreadId).isEqualTo(newThreadId)
      coVerify { conversationRepository.createNewThread(newPersonaId) }
      coVerify(exactly = 0) { conversationRepository.updateThreadPersona(any(), any()) }
      coVerify { personaSwitchLogRepository.logSwitch(any()) }
    }

  @Test
  fun `invoke returns recoverable error when repository throws exception`() = runTest {
    // Given
    val exception = RuntimeException("Database error")
    coEvery { conversationRepository.getCurrentPersonaForThread(threadId) } throws exception

    // When
    val result = useCase(threadId, newPersonaId, PersonaSwitchAction.CONTINUE_THREAD)

    // Then
    val error = result.assertRecoverableError()
    assertThat(error.message).isEqualTo("Failed to switch persona: Database error")
    assertThat(error.cause).isEqualTo(exception)
    coVerify(exactly = 0) { personaSwitchLogRepository.logSwitch(any()) }
  }

  @Test
  fun `invoke logs switch with correct details for continue thread action`() = runTest {
    // Given
    coEvery { conversationRepository.getCurrentPersonaForThread(threadId) } returns
      previousPersonaId

    // When
    useCase(threadId, newPersonaId, PersonaSwitchAction.CONTINUE_THREAD)

    // Then
    coVerify {
      personaSwitchLogRepository.logSwitch(
        withArg {
          assertThat(it.threadId).isEqualTo(threadId)
          assertThat(it.previousPersonaId).isEqualTo(previousPersonaId)
          assertThat(it.newPersonaId).isEqualTo(newPersonaId)
          assertThat(it.actionTaken).isEqualTo(PersonaSwitchAction.CONTINUE_THREAD)
        }
      )
    }
  }

  @Test
  fun `invoke logs switch with correct details for new thread action`() = runTest {
    // Given
    val newThreadId = UUID.randomUUID()
    coEvery { conversationRepository.getCurrentPersonaForThread(threadId) } returns
      previousPersonaId
    coEvery { conversationRepository.createNewThread(newPersonaId) } returns newThreadId

    // When
    useCase(threadId, newPersonaId, PersonaSwitchAction.START_NEW_THREAD)

    // Then
    coVerify {
      personaSwitchLogRepository.logSwitch(
        withArg {
          assertThat(it.threadId).isEqualTo(newThreadId)
          assertThat(it.previousPersonaId).isEqualTo(previousPersonaId)
          assertThat(it.newPersonaId).isEqualTo(newPersonaId)
          assertThat(it.actionTaken).isEqualTo(PersonaSwitchAction.START_NEW_THREAD)
        }
      )
    }
  }
}
