@file:Suppress("UnusedParameter")

package com.vjaykrsna.nanoai.core.domain.chat

import android.graphics.Bitmap
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.domain.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.domain.repository.InferencePreferenceRepository
import com.vjaykrsna.nanoai.core.domain.repository.PersonaRepository
import com.vjaykrsna.nanoai.core.model.InferenceMode
import com.vjaykrsna.nanoai.core.model.MessageRole
import com.vjaykrsna.nanoai.core.model.MessageSource
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

private data class InferenceConfigurationData(
  val options: InferenceConfiguration,
  val preferLocal: Boolean,
)

/** Use case for sending prompts and generating AI responses. */
@Singleton
class SendPromptUseCase
@Inject
constructor(
  private val conversationRepository: ConversationRepository,
  private val personaRepository: PersonaRepository,
  private val inferenceOrchestrator: InferenceOrchestrator,
  private val inferencePreferenceRepository: InferencePreferenceRepository,
) : SendPromptUseCaseInterface {
  override suspend operator fun invoke(
    threadId: UUID,
    prompt: String,
    @Suppress("UnusedParameter") personaId: UUID,
    image: Bitmap?,
    audio: ByteArray?,
  ): NanoAIResult<Unit> {
    val availability = checkInferenceAvailability(threadId, personaId)
    if (availability is NanoAIResult.RecoverableError) return availability

    val options = prepareInferenceConfiguration(personaId)
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

  private suspend fun prepareInferenceConfiguration(personaId: UUID): InferenceConfigurationData {
    val persona = runCatching { personaRepository.getPersonaById(personaId).first() }.getOrNull()
    val inferenceConfiguration =
      persona?.let {
        InferenceConfiguration(
          systemPrompt = it.systemPrompt,
          temperature = it.temperature,
          topP = it.topP,
          localModelPreference = it.defaultModelPreference,
        )
      } ?: InferenceConfiguration()

    val userPreference = inferencePreferenceRepository.observeInferencePreference().first()
    val isOnline = inferenceOrchestrator.isOnline()
    val hasLocalModel = inferenceOrchestrator.hasLocalModelAvailable()
    val preferLocal =
      shouldPreferLocal(hasLocalModel, isOnline, userPreference.mode == InferenceMode.LOCAL_FIRST)

    return InferenceConfigurationData(inferenceConfiguration, preferLocal)
  }

  private suspend fun performInference(
    prompt: String,
    personaId: UUID,
    options: InferenceConfigurationData,
    image: Bitmap? = null,
    audio: ByteArray? = null,
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
      is NanoAIResult.Success -> saveSuccessMessage(result.value, threadId)
      is NanoAIResult.RecoverableError -> saveErrorMessage(result, threadId, personaId, preferLocal)
      is NanoAIResult.FatalError -> saveErrorMessage(result, threadId, personaId, preferLocal)
    }

  private suspend fun saveSuccessMessage(
    result: InferenceSuccessData,
    threadId: UUID,
  ): NanoAIResult<Unit> {
    val message =
      Message(
        messageId = UUID.randomUUID(),
        threadId = threadId,
        role = MessageRole.ASSISTANT,
        text = result.text,
        source = result.source,
        latencyMs = result.latencyMs,
        createdAt = Clock.System.now(),
      )
    conversationRepository.saveMessage(message)
    return NanoAIResult.success(Unit)
  }

  private suspend fun saveErrorMessage(
    result: NanoAIResult.RecoverableError,
    threadId: UUID,
    personaId: UUID,
    preferLocal: Boolean,
  ): NanoAIResult<Unit> {
    val message =
      Message(
        messageId = UUID.randomUUID(),
        threadId = threadId,
        role = MessageRole.ASSISTANT,
        text = null,
        source = if (preferLocal) MessageSource.LOCAL_MODEL else MessageSource.CLOUD_API,
        latencyMs = null,
        createdAt = Clock.System.now(),
        errorCode = result.telemetryId,
      )
    conversationRepository.saveMessage(message)
    return NanoAIResult.recoverable(
      message = result.message,
      telemetryId = result.telemetryId,
      context =
        mapOf("threadId" to threadId.toString(), "personaId" to personaId.toString()) +
          result.context,
    )
  }

  private suspend fun saveErrorMessage(
    result: NanoAIResult.FatalError,
    threadId: UUID,
    personaId: UUID,
    preferLocal: Boolean,
  ): NanoAIResult<Unit> {
    val message =
      Message(
        messageId = UUID.randomUUID(),
        threadId = threadId,
        role = MessageRole.ASSISTANT,
        text = null,
        source = if (preferLocal) MessageSource.LOCAL_MODEL else MessageSource.CLOUD_API,
        latencyMs = null,
        createdAt = Clock.System.now(),
        errorCode = result.telemetryId,
      )
    conversationRepository.saveMessage(message)
    return NanoAIResult.fatal(
      message = result.message,
      supportContact = result.supportContact,
      telemetryId = result.telemetryId,
      cause = result.cause,
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
