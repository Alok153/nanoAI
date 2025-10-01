package com.vjaykrsna.nanoai.feature.chat.domain

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.data.repository.PersonaRepository
import com.vjaykrsna.nanoai.core.data.repository.PersonaSwitchLogRepository
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.domain.model.PersonaSwitchLog
import com.vjaykrsna.nanoai.core.model.MessageSource
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import com.vjaykrsna.nanoai.core.model.Role
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import org.junit.Before
import org.junit.Test
import java.util.UUID

class SendPromptAndPersonaUseCaseTest {
    private lateinit var useCase: SendPromptAndPersonaUseCase
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var personaRepository: PersonaRepository
    private lateinit var inferenceOrchestrator: InferenceOrchestrator
    private lateinit var personaSwitchLogRepository: PersonaSwitchLogRepository

    @Before
    fun setUp() {
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
    fun sendPrompt_returnsFailureWhenOfflineAndNoLocalModel() =
        runTest {
            val threadId = UUID.randomUUID()
            val personaId = UUID.randomUUID()

            coEvery { inferenceOrchestrator.isOnline() } returns false
            coEvery { inferenceOrchestrator.hasLocalModelAvailable() } returns false

            val result = useCase.sendPrompt(threadId, "hi", personaId)

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(OfflineNoModelException::class.java)
            coVerify(exactly = 0) {
                inferenceOrchestrator.generateResponse(
                    prompt = "hi",
                    personaId = personaId,
                    preferLocal = false,
                    options = match { true },
                )
            }
        }

    @Test
    fun sendPrompt_prefersLocalModelWhenAvailable() =
        runTest {
            val threadId = UUID.randomUUID()
            val personaId = UUID.randomUUID()
            val latency = 1200L

            coEvery { inferenceOrchestrator.isOnline() } returns true
            coEvery { inferenceOrchestrator.hasLocalModelAvailable() } returns true
            coEvery { inferenceOrchestrator.generateResponse(any(), any(), any(), any()) } returns
                InferenceResult.Success(
                    text = "response",
                    source = MessageSource.LOCAL_MODEL,
                    latencyMs = latency,
                )

            val result = useCase.sendPrompt(threadId, "hello", personaId)

            assertThat(result.isSuccess).isTrue()
            coVerify {
                inferenceOrchestrator.generateResponse(
                    prompt = "hello",
                    personaId = personaId,
                    preferLocal = true,
                    options = any(),
                )
            }
            coVerify {
                conversationRepository.saveMessage(
                    match { message ->
                        message.threadId == threadId &&
                            message.role == Role.ASSISTANT &&
                            message.source == MessageSource.LOCAL_MODEL &&
                            message.latencyMs == latency
                    },
                )
            }
            assertThat(result.isFailure).isFalse()
        }

    @Test
    fun sendPrompt_recordsErrorWhenInferenceFails() =
        runTest {
            val threadId = UUID.randomUUID()
            val personaId = UUID.randomUUID()

            coEvery { inferenceOrchestrator.isOnline() } returns true
            coEvery { inferenceOrchestrator.hasLocalModelAvailable() } returns false
            coEvery { inferenceOrchestrator.generateResponse(any(), any(), any(), any()) } returns
                InferenceResult.Error(
                    errorCode = "RATE_LIMIT",
                    message = "Too many requests",
                )

            val result = useCase.sendPrompt(threadId, "question", personaId)

            assertThat(result.isFailure).isTrue()
            coVerify {
                conversationRepository.saveMessage(
                    match { message ->
                        message.threadId == threadId &&
                            message.errorCode == "RATE_LIMIT" &&
                            message.text == null
                    },
                )
            }
        }

    @Test
    fun sendPrompt_appliesPersonaTuning() =
        runTest {
            val personaId = UUID.randomUUID()
            val threadId = UUID.randomUUID()
            val now = Clock.System.now()
            val persona =
                PersonaProfile(
                    personaId = personaId,
                    name = "Creative",
                    description = "Creative assistant",
                    systemPrompt = "Stay creative",
                    defaultModelPreference = null,
                    temperature = 0.8f,
                    topP = 0.9f,
                    defaultVoice = null,
                    defaultImageStyle = null,
                    createdAt = now.minus(1.days),
                    updatedAt = now,
                )

            coEvery { inferenceOrchestrator.isOnline() } returns true
            coEvery { inferenceOrchestrator.hasLocalModelAvailable() } returns true
            coEvery { personaRepository.getPersonaById(personaId) } returns flowOf(persona)
            coEvery { inferenceOrchestrator.generateResponse(any(), any(), any(), any()) } returns
                InferenceResult.Success(
                    text = "ok",
                    source = MessageSource.LOCAL_MODEL,
                    latencyMs = 1000,
                )

            useCase.sendPrompt(threadId, "prompt", personaId)

            coVerify {
                inferenceOrchestrator.generateResponse(
                    prompt = "prompt",
                    personaId = personaId,
                    preferLocal = true,
                    options =
                        match { options ->
                            options.temperature == persona.temperature &&
                                options.topP == persona.topP &&
                                options.systemPrompt == persona.systemPrompt
                        },
                )
            }
        }

    @Test
    fun switchPersona_updatesExistingThread() =
        runTest {
            val threadId = UUID.randomUUID()
            val oldPersona = UUID.randomUUID()
            val newPersona = UUID.randomUUID()

            coEvery { conversationRepository.getCurrentPersonaForThread(threadId) } returns oldPersona

            useCase.switchPersona(threadId, newPersona, PersonaSwitchAction.CONTINUE_THREAD)

            coVerify { conversationRepository.updateThreadPersona(threadId, newPersona) }
            coVerify {
                personaSwitchLogRepository.logSwitch(
                    match { log ->
                        log.threadId == threadId &&
                            log.previousPersonaId == oldPersona &&
                            log.newPersonaId == newPersona &&
                            log.actionTaken == PersonaSwitchAction.CONTINUE_THREAD
                    },
                )
            }
        }

    @Test
    fun switchPersona_createsNewThreadWhenRequested() =
        runTest {
            val originalThread = UUID.randomUUID()
            val newPersona = UUID.randomUUID()
            val newThreadId = UUID.randomUUID()

            coEvery { conversationRepository.getCurrentPersonaForThread(originalThread) } returns null
            coEvery { conversationRepository.createNewThread(newPersona) } returns newThreadId

            val result =
                useCase.switchPersona(originalThread, newPersona, PersonaSwitchAction.START_NEW_THREAD)

            assertThat(result).isEqualTo(newThreadId)
            coVerify { conversationRepository.createNewThread(newPersona) }
            coVerify {
                personaSwitchLogRepository.logSwitch(
                    match { log ->
                        log.threadId == newThreadId &&
                            log.newPersonaId == newPersona &&
                            log.actionTaken == PersonaSwitchAction.START_NEW_THREAD
                    },
                )
            }
        }

    @Test
    fun getPersonaSwitchHistory_delegatesToRepository() =
        runTest {
            val threadId = UUID.randomUUID()
            val logs =
                listOf(
                    PersonaSwitchLog(
                        logId = UUID.randomUUID(),
                        threadId = threadId,
                        previousPersonaId = null,
                        newPersonaId = UUID.randomUUID(),
                        actionTaken = PersonaSwitchAction.CONTINUE_THREAD,
                        createdAt = Clock.System.now(),
                    ),
                )

            coEvery { personaSwitchLogRepository.getLogsByThreadId(threadId) } returns flowOf(logs)

            val history = useCase.getPersonaSwitchHistory(threadId)

            assertThat(history.first()).isEqualTo(logs)
        }
}
