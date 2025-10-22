package com.vjaykrsna.nanoai.feature.chat.domain

import com.vjaykrsna.nanoai.core.data.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.data.repository.InferencePreferenceRepository
import com.vjaykrsna.nanoai.core.data.repository.PersonaRepository
import com.vjaykrsna.nanoai.testing.assertRecoverableErrorWithMessage
import io.mockk.coEvery
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SendPromptUseCaseTest {
  private lateinit var useCase: SendPromptUseCase
  private lateinit var conversationRepository: ConversationRepository
  private lateinit var personaRepository: PersonaRepository
  private lateinit var inferenceOrchestrator: InferenceOrchestrator
  private lateinit var inferencePreferenceRepository: InferencePreferenceRepository

  private val threadId = UUID.randomUUID()
  private val personaId = UUID.randomUUID()
  private val prompt = "Test prompt"

  @Before
  fun setup() {
    conversationRepository = mockk(relaxed = true)
    personaRepository = mockk(relaxed = true)
    inferenceOrchestrator = mockk<InferenceOrchestrator>(relaxed = true)
    inferencePreferenceRepository = mockk(relaxed = true)

    useCase =
      SendPromptUseCase(
        conversationRepository = conversationRepository,
        personaRepository = personaRepository,
        inferenceOrchestrator = inferenceOrchestrator,
        inferencePreferenceRepository = inferencePreferenceRepository,
      )
  }

  @Test
  fun `returns recoverable error when offline with no local model`() = runTest {
    coEvery { inferenceOrchestrator.isOnline() } returns false
    coEvery { inferenceOrchestrator.hasLocalModelAvailable() } returns false

    val result = useCase(threadId, prompt, personaId)

    result.assertRecoverableErrorWithMessage("Device offline with no local model available")
  }
}
