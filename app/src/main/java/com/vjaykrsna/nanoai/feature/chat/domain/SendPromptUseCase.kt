package com.vjaykrsna.nanoai.feature.chat.domain

import android.graphics.Bitmap
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.data.repository.InferencePreferenceRepository
import com.vjaykrsna.nanoai.core.data.repository.PersonaRepository
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.model.InferenceMode
import com.vjaykrsna.nanoai.core.model.MessageSource
import com.vjaykrsna.nanoai.core.model.Role
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

private data class GenerationOptionsData(val options: GenerationOptions, val preferLocal: Boolean)

/** Use case for sending prompts and generating AI responses. */
@Singleton
class SendPromptUseCase
@Inject
constructor(
  private val conversationRepository: ConversationRepository,
  private val personaRepository: PersonaRepository,
  private val inferenceOrchestrator: InferenceOrchestrator,
  private val inferencePreferenceRepository: InferencePreferenceRepository,
) {
  suspend operator fun invoke(
    threadId: UUID,
    prompt: String,
    personaId: UUID,
    image: Bitmap? = null,
    audio: ByteArray? = null
  ): NanoAIResult<Unit> {
    val availability = checkInferenceAvailability(threadId, personaId)
    if (availability is NanoAIResult.RecoverableError) return availability

    val options = prepareGenerationOptions(personaId)
    val inferenceResult = performInference(prompt, personaId, options, image, audio)

    return handleInferenceResult(inferenceResult, threadId, personaId, options.preferLocal)
  }

  private suspend fun checkInferenceAvailability(
    threadId: UUID,
    personaId: UUID,
  ): NanoAIResult<Unit> {
    val isOnline = inferenceOrchestrator.isOnline()
    val hasLocalModel = inferenceOrchestrator.hasLocalModelAvailable()

    return if (!isOnline && !hasLocalModel) {
      NanoAIResult.recoverable(
        message = "Device offline with no local model available",
        context = mapOf("threadId" to threadId.toString(), "personaId" to personaId.toString()),
      )
    } else {
      NanoAIResult.success(Unit)
    }
  }

  private suspend fun prepareGenerationOptions(personaId: UUID): GenerationOptionsData {
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
    val isOnline = inferenceOrchestrator.isOnline()
    val hasLocalModel = inferenceOrchestrator.hasLocalModelAvailable()
    val preferLocal =
      shouldPreferLocal(hasLocalModel, isOnline, userPreference.mode == InferenceMode.LOCAL_FIRST)

    return GenerationOptionsData(generationOptions, preferLocal)
  }

  private suspend fun performInference(
    prompt: String,
    personaId: UUID,
    options: GenerationOptionsData,
    image: Bitmap? = null,
    audio: ByteArray? = null
  ): InferenceResult =
    inferenceOrchestrator.generateResponse(
      prompt = prompt,
      personaId = personaId,
      options = options.options,
      image = image,
      audio = audio,
    )

  private suspend fun handleInferenceResult(
    result: InferenceResult,
    threadId: UUID,
    personaId: UUID,
    preferLocal: Boolean,
  ): NanoAIResult<Unit> =
    when (result) {
      is InferenceResult.Success -> saveSuccessMessage(result, threadId)
      is InferenceResult.Error -> saveErrorMessage(result, threadId, personaId, preferLocal)
    }

  private suspend fun saveSuccessMessage(
    result: InferenceResult.Success,
    threadId: UUID,
  ): NanoAIResult<Unit> {
    val message =
      Message(
        messageId = UUID.randomUUID(),
        threadId = threadId,
        role = Role.ASSISTANT,
        text = result.text,
        source = result.source,
        latencyMs = result.latencyMs,
        createdAt = Clock.System.now(),
      )
    conversationRepository.saveMessage(message)
    return NanoAIResult.success(Unit)
  }

  private suspend fun saveErrorMessage(
    result: InferenceResult.Error,
    threadId: UUID,
    personaId: UUID,
    preferLocal: Boolean,
  ): NanoAIResult<Unit> {
    val message =
      Message(
        messageId = UUID.randomUUID(),
        threadId = threadId,
        role = Role.ASSISTANT,
        text = null,
        source = if (preferLocal) MessageSource.LOCAL_MODEL else MessageSource.CLOUD_API,
        latencyMs = null,
        createdAt = Clock.System.now(),
        errorCode = result.errorCode,
      )
    conversationRepository.saveMessage(message)
    return NanoAIResult.recoverable(
      message = result.message ?: "Inference failed with code ${result.errorCode}",
      telemetryId = result.errorCode,
      context = mapOf("threadId" to threadId.toString(), "personaId" to personaId.toString()),
    )
  }

  private fun shouldPreferLocal(
    hasLocalModel: Boolean,
    isOnline: Boolean,
    userPrefersLocal: Boolean,
  ): Boolean =
    when {
      !hasLocalModel -> false
      !isOnline -> true
      else -> userPrefersLocal
    }
}
