package com.vjaykrsna.nanoai.core.domain.chat

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.InferencePreference
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.domain.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.domain.repository.InferencePreferenceRepository
import com.vjaykrsna.nanoai.core.domain.repository.PersonaRepository
import com.vjaykrsna.nanoai.testing.assertRecoverableErrorWithMessage
import io.mockk.*
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SendPromptUseCaseTest {
  private lateinit var useCase: SendPromptUseCase
  private lateinit var conversationRepository: ConversationRepository
  private lateinit var personaRepository: PersonaRepository
  private lateinit var inferenceGateway: PromptInferenceGateway
  private lateinit var inferencePreferenceRepository: InferencePreferenceRepository

  private val threadId = UUID.randomUUID()
  private val personaId = UUID.randomUUID()
  private val prompt = "Test prompt"

  @BeforeEach
  fun setup() {
    conversationRepository = mockk(relaxed = true)
    personaRepository = mockk(relaxed = true)
    inferenceGateway = mockk(relaxed = true)
    inferencePreferenceRepository = mockk(relaxed = true)

    coEvery { personaRepository.getPersonaById(any()) } returns flowOf(null)
    every { inferencePreferenceRepository.observeInferencePreference() } returns
      flowOf(InferencePreference())
    coEvery { inferenceGateway.isOnline() } returns true
    coEvery { inferenceGateway.hasLocalModelAvailable() } returns true

    useCase =
      SendPromptUseCase(
        conversationRepository = conversationRepository,
        personaRepository = personaRepository,
        promptInferenceGateway = inferenceGateway,
        inferencePreferenceRepository = inferencePreferenceRepository,
      )
  }

  @Test
  fun `returns recoverable error when offline with no local model`() = runTest {
    coEvery { inferenceGateway.isOnline() } returns false
    coEvery { inferenceGateway.hasLocalModelAvailable() } returns false

    val result = useCase(threadId, prompt, personaId)

    result.assertRecoverableErrorWithMessage("Device offline with no local model available")
  }

  @Test
  fun `includes persona metadata in recoverable context`() = runTest {
    val persona =
      PersonaProfile(
        personaId = personaId,
        name = "Researcher",
        description = "Helps with academic tasks",
        systemPrompt = "Assist with citations",
        defaultModelPreference = "nano.local",
        temperature = 0.7f,
        topP = 0.9f,
        defaultVoice = null,
        defaultImageStyle = null,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
      )
    coEvery { personaRepository.getPersonaById(personaId) } returns flowOf(persona)

    val orchestratorResult =
      NanoAIResult.recoverable(
        message = "Temporary issue",
        telemetryId = "TEMP",
        context = mapOf("existing" to "value"),
      )

    coEvery {
      inferenceGateway.generateResponse(
        prompt = prompt,
        personaId = personaId,
        configuration = any(),
        attachments = any(),
      )
    } returns orchestratorResult

    val result = useCase(threadId, prompt, personaId)

    val recoverable = assertIs<NanoAIResult.RecoverableError>(result)
    assertEquals("Temporary issue", recoverable.message)
    assertEquals("value", recoverable.context["existing"])
    assertEquals(threadId.toString(), recoverable.context["threadId"])
    assertEquals(personaId.toString(), recoverable.context["personaId"])
    assertEquals("Researcher", recoverable.context["personaName"])
    assertEquals("0.7", recoverable.context["personaTemperature"])
    assertEquals("0.9", recoverable.context["personaTopP"])
    assertEquals("nano.local", recoverable.context["personaModelPreference"])
  }
}
