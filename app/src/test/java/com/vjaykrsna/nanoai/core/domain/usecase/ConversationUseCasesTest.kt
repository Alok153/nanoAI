package com.vjaykrsna.nanoai.core.domain.usecase

import android.database.sqlite.SQLiteException
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.domain.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.domain.repository.PersonaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ConversationUseCasesTest {

  @MockK lateinit var conversationRepository: ConversationRepository
  @MockK lateinit var personaRepository: PersonaRepository

  private lateinit var getConversationHistoryUseCase: GetConversationHistoryUseCase
  private lateinit var getDefaultPersonaUseCase: GetDefaultPersonaUseCase
  private lateinit var observePersonasUseCase: ObservePersonasUseCase

  @BeforeEach
  fun setUp() {
    getConversationHistoryUseCase = GetConversationHistoryUseCase(conversationRepository)
    getDefaultPersonaUseCase = GetDefaultPersonaUseCase(personaRepository)
    observePersonasUseCase = ObservePersonasUseCase(personaRepository)
  }

  @Test
  fun `getConversationHistory returns success on repository data`() = runTest {
    val threads =
      listOf(
        ChatThread(
          threadId = UUID.randomUUID(),
          title = "First",
          personaId = UUID.randomUUID(),
          activeModelId = "nova",
          createdAt = Instant.fromEpochMilliseconds(0),
          updatedAt = Instant.fromEpochMilliseconds(10),
        )
      )
    coEvery { conversationRepository.getAllThreads() } returns threads

    val result = getConversationHistoryUseCase()

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    assertThat((result as NanoAIResult.Success).value).isEqualTo(threads)
  }

  @Test
  fun `getConversationHistory wraps data exceptions as recoverable`() = runTest {
    coEvery { conversationRepository.getAllThreads() } throws SQLiteException("boom")

    val sqliteResult = getConversationHistoryUseCase()

    assertThat(sqliteResult).isInstanceOf(NanoAIResult.RecoverableError::class.java)

    coEvery { conversationRepository.getAllThreads() } throws IOException("offline")

    val ioResult = getConversationHistoryUseCase()

    assertThat(ioResult).isInstanceOf(NanoAIResult.RecoverableError::class.java)
  }

  @Test
  fun `getDefaultPersona delegates to repository`() = runTest {
    val persona =
      PersonaProfile(
        personaId = UUID.randomUUID(),
        name = "Default",
        description = "desc",
        systemPrompt = "prompt",
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
      )
    coEvery { personaRepository.getDefaultPersona() } returns persona

    val result = getDefaultPersonaUseCase()

    assertThat(result).isEqualTo(persona)
    coVerify { personaRepository.getDefaultPersona() }
  }

  @Test
  fun `observePersonas exposes repository flow`() {
    val personas =
      listOf(
        PersonaProfile(
          personaId = UUID.randomUUID(),
          name = "One",
          description = "desc",
          systemPrompt = "prompt",
          createdAt = Instant.fromEpochMilliseconds(0),
          updatedAt = Instant.fromEpochMilliseconds(0),
        )
      )
    val flow = flowOf(personas)
    every { personaRepository.observeAllPersonas() } returns flow

    assertThat(observePersonasUseCase()).isEqualTo(flow)
  }
}
