@file:Suppress(
  "ReturnCount",
  "LongMethod",
) // Multiple validation paths in use case, complex business logic

package com.vjaykrsna.nanoai.feature.chat.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.data.repository.InferencePreferenceRepository
import com.vjaykrsna.nanoai.core.data.repository.PersonaRepository
import com.vjaykrsna.nanoai.core.data.repository.PersonaSwitchLogRepository
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.domain.model.PersonaSwitchLog
import com.vjaykrsna.nanoai.core.model.InferenceMode
import com.vjaykrsna.nanoai.core.model.MessageSource
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import com.vjaykrsna.nanoai.core.model.Role
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

@Singleton
class SendPromptAndPersonaUseCase
@Inject
constructor(
  private val conversationRepository: ConversationRepository,
  private val personaRepository: PersonaRepository,
  private val inferenceOrchestrator: InferenceOrchestrator,
  private val personaSwitchLogRepository: PersonaSwitchLogRepository,
  private val inferencePreferenceRepository: InferencePreferenceRepository,
) {
  suspend fun sendPrompt(threadId: UUID, prompt: String, personaId: UUID): NanoAIResult<Unit> {
    val isOnline = inferenceOrchestrator.isOnline()
    val hasLocalModel = inferenceOrchestrator.hasLocalModelAvailable()

    if (!isOnline && !hasLocalModel) {
      return NanoAIResult.recoverable(
        message = "Device offline with no local model available",
        context = mapOf("threadId" to threadId.toString(), "personaId" to personaId.toString()),
      )
    }

    val persona = runCatching { personaRepository.getPersonaById(personaId).first() }.getOrNull()
    val generationOptions =
      persona?.let {
        GenerationOptions(
          systemPrompt = it.systemPrompt,
          temperature = it.temperature,
          topP = it.topP,
          localModelPreference = it.defaultModelPreference,
        )
      } ?: GenerationOptions()

    val userPreference = inferencePreferenceRepository.observeInferencePreference().first()
    val preferLocal =
      shouldPreferLocal(hasLocalModel, isOnline, userPreference.mode == InferenceMode.LOCAL_FIRST)

    val inferenceResult =
      inferenceOrchestrator.generateResponse(
        prompt = prompt,
        personaId = personaId,
        options = generationOptions,
      )

    return when (inferenceResult) {
      is InferenceResult.Success -> {
        val message =
          Message(
            messageId = UUID.randomUUID(),
            threadId = threadId,
            role = Role.ASSISTANT,
            text = inferenceResult.text,
            source = inferenceResult.source,
            latencyMs = inferenceResult.latencyMs,
            createdAt = Clock.System.now(),
          )
        conversationRepository.saveMessage(message)
        NanoAIResult.success(Unit)
      }
      is InferenceResult.Error -> {
        val message =
          Message(
            messageId = UUID.randomUUID(),
            threadId = threadId,
            role = Role.ASSISTANT,
            text = null,
            source = if (preferLocal) MessageSource.LOCAL_MODEL else MessageSource.CLOUD_API,
            latencyMs = null,
            createdAt = Clock.System.now(),
            errorCode = inferenceResult.errorCode,
          )
        conversationRepository.saveMessage(message)
        NanoAIResult.recoverable(
          message =
            inferenceResult.message ?: "Inference failed with code ${inferenceResult.errorCode}",
          telemetryId = inferenceResult.errorCode,
          context = mapOf("threadId" to threadId.toString(), "personaId" to personaId.toString()),
        )
      }
    }
  }

  suspend fun switchPersona(
    threadId: UUID,
    newPersonaId: UUID,
    action: PersonaSwitchAction,
  ): NanoAIResult<UUID> {
    return try {
      val previousPersonaId = conversationRepository.getCurrentPersonaForThread(threadId)
      val targetThreadId =
        when (action) {
          PersonaSwitchAction.CONTINUE_THREAD -> {
            conversationRepository.updateThreadPersona(threadId, newPersonaId)
            threadId
          }
          PersonaSwitchAction.START_NEW_THREAD -> {
            conversationRepository.createNewThread(newPersonaId)
          }
        }

      val log =
        PersonaSwitchLog(
          logId = UUID.randomUUID(),
          threadId = targetThreadId,
          previousPersonaId = previousPersonaId,
          newPersonaId = newPersonaId,
          actionTaken = action,
          createdAt = Clock.System.now(),
        )
      personaSwitchLogRepository.logSwitch(log)
      NanoAIResult.success(targetThreadId)
    } catch (error: Throwable) {
      NanoAIResult.recoverable(
        message = "Failed to switch persona: ${error.message}",
        cause = error,
      )
    }
  }

  suspend fun getPersonaSwitchHistory(threadId: UUID): Flow<List<PersonaSwitchLog>> =
    personaSwitchLogRepository.getLogsByThreadId(threadId)
}

private fun shouldPreferLocal(
  hasLocalModel: Boolean,
  isOnline: Boolean,
  userPrefersLocal: Boolean,
): Boolean {
  if (!hasLocalModel) return false
  if (!isOnline) return true
  return userPrefersLocal
}
