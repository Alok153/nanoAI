package com.vjaykrsna.nanoai.feature.chat.domain

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

/**
 * Domain unit test for SendPromptAndPersonaUseCase.
 * Verifies persona switch logging and local vs cloud routing logic.
 *
 * TDD: This test is written BEFORE the use case is implemented.
 * Expected to FAIL with compilation errors until:
 * - SendPromptAndPersonaUseCase is created
 * - ConversationRepository is defined
 * - PersonaRepository is defined
 * - InferenceOrchestrator is defined
 * - PersonaSwitchLogRepository is defined
 */
class SendPromptAndPersonaUseCaseTest {
    private lateinit var useCase: SendPromptAndPersonaUseCase
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var personaRepository: PersonaRepository
    private lateinit var inferenceOrchestrator: InferenceOrchestrator
    private lateinit var personaSwitchLogRepository: PersonaSwitchLogRepository

    @Before
    fun setup() {
        conversationRepository = mockk(relaxed = true)
        personaRepository = mockk(relaxed = true)
        inferenceOrchestrator = mockk(relaxed = true)
        personaSwitchLogRepository = mockk(relaxed = true)

        useCase =
            SendPromptAndPersonaUseCase(
                conversationRepository = conversationRepository,
                personaRepository = personaRepository,
                inferenceOrchestrator = inferenceOrchestrator,
                personaSwitchLogRepository = personaSwitchLogRepository,
            )
    }

    @Test
    fun `sendPrompt should use local model when offline and model available`() =
        runTest {
            // Arrange
            val threadId = UUID.randomUUID()
            val prompt = "Tell me a joke"
            val personaId = UUID.randomUUID()

            coEvery { inferenceOrchestrator.isOnline() } returns false
            coEvery { inferenceOrchestrator.hasLocalModelAvailable() } returns true
            coEvery {
                inferenceOrchestrator.generateResponse(any(), any(), any())
            } returns
                InferenceResult.Success(
                    text = "Why did the AI cross the road?",
                    source = MessageSource.LOCAL_MODEL,
                    latencyMs = 1200,
                )

            // Act
            val result = useCase.sendPrompt(threadId, prompt, personaId)

            // Assert
            assertThat(result.isSuccess).isTrue()
            coVerify {
                inferenceOrchestrator.generateResponse(
                    prompt = prompt,
                    personaId = personaId,
                    preferLocal = true,
                )
            }
        }

    @Test
    fun `sendPrompt should fail gracefully when offline and no local model`() =
        runTest {
            // Arrange
            val threadId = UUID.randomUUID()
            val prompt = "Tell me a story"
            val personaId = UUID.randomUUID()

            coEvery { inferenceOrchestrator.isOnline() } returns false
            coEvery { inferenceOrchestrator.hasLocalModelAvailable() } returns false

            // Act
            val result = useCase.sendPrompt(threadId, prompt, personaId)

            // Assert
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(OfflineNoModelException::class.java)
            coVerify(exactly = 0) {
                inferenceOrchestrator.generateResponse(any(), any(), any())
            }
        }

    @Test
    fun `sendPrompt should use cloud API when online and local model unavailable`() =
        runTest {
            // Arrange
            val threadId = UUID.randomUUID()
            val prompt = "Write a poem"
            val personaId = UUID.randomUUID()

            coEvery { inferenceOrchestrator.isOnline() } returns true
            coEvery { inferenceOrchestrator.hasLocalModelAvailable() } returns false
            coEvery {
                inferenceOrchestrator.generateResponse(any(), any(), any())
            } returns
                InferenceResult.Success(
                    text = "Roses are red...",
                    source = MessageSource.CLOUD_API,
                    latencyMs = 3500,
                )

            // Act
            val result = useCase.sendPrompt(threadId, prompt, personaId)

            // Assert
            assertThat(result.isSuccess).isTrue()
            coVerify {
                inferenceOrchestrator.generateResponse(
                    prompt = prompt,
                    personaId = personaId,
                    preferLocal = false,
                )
            }
        }

    @Test
    fun `sendPrompt should prefer local model when both local and cloud available`() =
        runTest {
            // Arrange
            val threadId = UUID.randomUUID()
            val prompt = "Explain quantum computing"
            val personaId = UUID.randomUUID()

            coEvery { inferenceOrchestrator.isOnline() } returns true
            coEvery { inferenceOrchestrator.hasLocalModelAvailable() } returns true
            coEvery {
                inferenceOrchestrator.generateResponse(any(), any(), any())
            } returns
                InferenceResult.Success(
                    text = "Quantum computing uses qubits...",
                    source = MessageSource.LOCAL_MODEL,
                    latencyMs = 1800,
                )

            // Act
            val result = useCase.sendPrompt(threadId, prompt, personaId)

            // Assert
            assertThat(result.isSuccess).isTrue()
            coVerify {
                inferenceOrchestrator.generateResponse(
                    prompt = prompt,
                    personaId = personaId,
                    preferLocal = true,
                )
            }
        }

    @Test
    fun `sendPrompt should save message to repository with latency metrics`() =
        runTest {
            // Arrange
            val threadId = UUID.randomUUID()
            val prompt = "Hello AI"
            val personaId = UUID.randomUUID()
            val expectedLatency = 1500L

            coEvery { inferenceOrchestrator.isOnline() } returns true
            coEvery { inferenceOrchestrator.hasLocalModelAvailable() } returns true
            coEvery {
                inferenceOrchestrator.generateResponse(any(), any(), any())
            } returns
                InferenceResult.Success(
                    text = "Hello! How can I help?",
                    source = MessageSource.LOCAL_MODEL,
                    latencyMs = expectedLatency,
                )

            // Act
            useCase.sendPrompt(threadId, prompt, personaId)

            // Assert
            coVerify {
                conversationRepository.saveMessage(
                    match { message ->
                        message.threadId == threadId &&
                            message.role == MessageRole.ASSISTANT &&
                            message.latencyMs == expectedLatency &&
                            message.source == MessageSource.LOCAL_MODEL
                    },
                )
            }
        }

    @Test
    fun `switchPersona should log persona change when continuing thread`() =
        runTest {
            // Arrange
            val threadId = UUID.randomUUID()
            val previousPersonaId = UUID.randomUUID()
            val newPersonaId = UUID.randomUUID()

            coEvery {
                conversationRepository.getCurrentPersonaForThread(threadId)
            } returns previousPersonaId

            // Act
            useCase.switchPersona(
                threadId = threadId,
                newPersonaId = newPersonaId,
                action = PersonaSwitchAction.CONTINUE_THREAD,
            )

            // Assert
            coVerify {
                personaSwitchLogRepository.logSwitch(
                    match { log ->
                        log.threadId == threadId &&
                            log.previousPersonaId == previousPersonaId &&
                            log.newPersonaId == newPersonaId &&
                            log.actionTaken == PersonaSwitchAction.CONTINUE_THREAD
                    },
                )
            }
        }

    @Test
    fun `switchPersona should create new thread when action is START_NEW_THREAD`() =
        runTest {
            // Arrange
            val oldThreadId = UUID.randomUUID()
            val newThreadId = UUID.randomUUID()
            val previousPersonaId = UUID.randomUUID()
            val newPersonaId = UUID.randomUUID()

            coEvery {
                conversationRepository.getCurrentPersonaForThread(oldThreadId)
            } returns previousPersonaId

            coEvery {
                conversationRepository.createNewThread(newPersonaId)
            } returns newThreadId

            // Act
            val result =
                useCase.switchPersona(
                    threadId = oldThreadId,
                    newPersonaId = newPersonaId,
                    action = PersonaSwitchAction.START_NEW_THREAD,
                )

            // Assert
            assertThat(result).isEqualTo(newThreadId)
            coVerify { conversationRepository.createNewThread(newPersonaId) }
            coVerify {
                personaSwitchLogRepository.logSwitch(
                    match { log ->
                        log.threadId == newThreadId &&
                            log.previousPersonaId == previousPersonaId &&
                            log.newPersonaId == newPersonaId &&
                            log.actionTaken == PersonaSwitchAction.START_NEW_THREAD
                    },
                )
            }
        }

    @Test
    fun `switchPersona should handle first persona assignment with null previous`() =
        runTest {
            // Arrange
            val threadId = UUID.randomUUID()
            val newPersonaId = UUID.randomUUID()

            coEvery {
                conversationRepository.getCurrentPersonaForThread(threadId)
            } returns null

            // Act
            useCase.switchPersona(
                threadId = threadId,
                newPersonaId = newPersonaId,
                action = PersonaSwitchAction.CONTINUE_THREAD,
            )

            // Assert
            coVerify {
                personaSwitchLogRepository.logSwitch(
                    match { log ->
                        log.threadId == threadId &&
                            log.previousPersonaId == null &&
                            log.newPersonaId == newPersonaId
                    },
                )
            }
        }

    @Test
    fun `sendPrompt should apply persona temperature settings`() =
        runTest {
            // Arrange
            val threadId = UUID.randomUUID()
            val prompt = "Test prompt"
            val personaId = UUID.randomUUID()
            val expectedTemperature = 0.9f

            val persona =
                PersonaProfile(
                    personaId = personaId,
                    name = "Creative Writer",
                    description = "Creative writing assistant",
                    systemPrompt = "You are a creative writer",
                    defaultModelPreference = null,
                    temperature = expectedTemperature,
                    topP = 0.95f,
                    defaultVoice = null,
                    defaultImageStyle = null,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )

            coEvery { personaRepository.getPersonaById(personaId) } returns flowOf(persona)
            coEvery { inferenceOrchestrator.isOnline() } returns true
            coEvery { inferenceOrchestrator.hasLocalModelAvailable() } returns true
            coEvery {
                inferenceOrchestrator.generateResponse(any(), any(), any())
            } returns
                InferenceResult.Success(
                    text = "Creative response",
                    source = MessageSource.LOCAL_MODEL,
                    latencyMs = 1000,
                )

            // Act
            useCase.sendPrompt(threadId, prompt, personaId)

            // Assert: Verify temperature was passed to orchestrator
            coVerify {
                inferenceOrchestrator.generateResponse(
                    prompt = prompt,
                    personaId = personaId,
                    preferLocal = true,
                )
            }
        }

    @Test
    fun `sendPrompt should store error code when inference fails`() =
        runTest {
            // Arrange
            val threadId = UUID.randomUUID()
            val prompt = "Test prompt"
            val personaId = UUID.randomUUID()

            coEvery { inferenceOrchestrator.isOnline() } returns true
            coEvery { inferenceOrchestrator.hasLocalModelAvailable() } returns false
            coEvery {
                inferenceOrchestrator.generateResponse(any(), any(), any())
            } returns
                InferenceResult.Error(
                    errorCode = "RATE_LIMIT_EXCEEDED",
                    message = "API rate limit exceeded",
                )

            // Act
            val result = useCase.sendPrompt(threadId, prompt, personaId)

            // Assert
            assertThat(result.isFailure).isTrue()
            coVerify {
                conversationRepository.saveMessage(
                    match { message ->
                        message.threadId == threadId &&
                            message.errorCode == "RATE_LIMIT_EXCEEDED" &&
                            message.text == null
                    },
                )
            }
        }

    @Test
    fun `getPersonaSwitchHistory should return logs for thread`() =
        runTest {
            // Arrange
            val threadId = UUID.randomUUID()
            val logs =
                listOf(
                    PersonaSwitchLog(
                        logId = UUID.randomUUID(),
                        threadId = threadId,
                        previousPersonaId = null,
                        newPersonaId = UUID.randomUUID(),
                        actionTaken = PersonaSwitchAction.CONTINUE_THREAD,
                        createdAt = Instant.now(),
                    ),
                    PersonaSwitchLog(
                        logId = UUID.randomUUID(),
                        threadId = threadId,
                        previousPersonaId = UUID.randomUUID(),
                        newPersonaId = UUID.randomUUID(),
                        actionTaken = PersonaSwitchAction.START_NEW_THREAD,
                        createdAt = Instant.now(),
                    ),
                )

            coEvery {
                personaSwitchLogRepository.getLogsByThreadId(threadId)
            } returns flowOf(logs)

            // Act & Assert
            useCase.getPersonaSwitchHistory(threadId).test {
                val result = awaitItem()
                assertThat(result).hasSize(2)
                assertThat(result[0].threadId).isEqualTo(threadId)
                awaitComplete()
            }
        }
}
