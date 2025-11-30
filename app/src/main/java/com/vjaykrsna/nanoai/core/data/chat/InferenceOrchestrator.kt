package com.vjaykrsna.nanoai.core.data.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.chat.InferenceOrchestrator.Companion.LOCAL_INFERENCE_FAILURE
import com.vjaykrsna.nanoai.core.domain.chat.InferenceConfiguration
import com.vjaykrsna.nanoai.core.domain.chat.InferenceResult
import com.vjaykrsna.nanoai.core.domain.chat.InferenceSuccessData
import com.vjaykrsna.nanoai.core.domain.chat.PromptAttachments
import com.vjaykrsna.nanoai.core.domain.chat.PromptImage
import com.vjaykrsna.nanoai.core.domain.chat.PromptInferenceGateway
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogRepository
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.core.domain.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.domain.repository.InferencePreferenceRepository
import com.vjaykrsna.nanoai.core.model.InferenceMode
import com.vjaykrsna.nanoai.core.model.MessageSource
import com.vjaykrsna.nanoai.core.network.CloudGatewayClient
import com.vjaykrsna.nanoai.core.network.CloudGatewayResult
import com.vjaykrsna.nanoai.core.network.ConnectivityStatusProvider
import com.vjaykrsna.nanoai.core.network.dto.CompletionMessageDto
import com.vjaykrsna.nanoai.core.network.dto.CompletionRequestDto
import com.vjaykrsna.nanoai.core.network.dto.CompletionResponseDto
import com.vjaykrsna.nanoai.core.network.dto.CompletionRole
import com.vjaykrsna.nanoai.core.runtime.LocalGenerationRequest
import com.vjaykrsna.nanoai.core.runtime.LocalModelRuntime
import java.io.ByteArrayInputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Coordinates local MediaPipe inference with cloud fallback via the gateway client. */
@Singleton
class InferenceOrchestrator
@Inject
constructor(
  private val modelCatalogRepository: ModelCatalogRepository,
  private val apiProviderConfigRepository: ApiProviderConfigRepository,
  private val inferencePreferenceRepository: InferencePreferenceRepository,
  private val localModelRuntime: LocalModelRuntime,
  private val cloudGatewayClient: CloudGatewayClient,
  private val connectivityStatusProvider: ConnectivityStatusProvider,
) : PromptInferenceGateway {
  /** True when the device currently has validated internet connectivity. */
  override suspend fun isOnline(): Boolean = connectivityStatusProvider.isOnline()

  /** True when any installed local model is ready for inference. */
  override suspend fun hasLocalModelAvailable(): Boolean {
    val installed = modelCatalogRepository.getInstalledModels()
    val localCandidates = installed.filterNot { it.providerType == ProviderType.CLOUD_API }
    if (localCandidates.isEmpty()) return false
    return localModelRuntime.hasReadyModel(localCandidates)
  }

  override suspend fun generateResponse(
    prompt: String,
    personaId: UUID?,
    configuration: InferenceConfiguration,
    attachments: PromptAttachments,
  ): InferenceResult {
    val installedModels = modelCatalogRepository.getInstalledModels()
    val localCandidates = installedModels.filterNot { it.providerType == ProviderType.CLOUD_API }
    val inferencePreference = inferencePreferenceRepository.observeInferencePreference().first()
    val userPrefersLocal = inferencePreference.mode == InferenceMode.LOCAL_FIRST
    val isOnline = isOnline()
    val preferLocal = resolvePreference(localCandidates.isNotEmpty(), isOnline, userPrefersLocal)
    val preferredLocalModel = selectLocalModel(localCandidates, configuration.localModelPreference)

    val result =
      if (preferLocal) {
        val localResult =
          runLocalInference(preferredLocalModel!!, prompt, configuration, attachments)
        if (localResult is NanoAIResult.Success) {
          localResult
        } else if (!isOnline) {
          localResult
        } else {
          runCloudInference(prompt, configuration)
        }
      } else {
        if (!isOnline) {
          offlineError()
        } else {
          val cloudResult = runCloudInference(prompt, configuration)
          if (cloudResult is NanoAIResult.Success) {
            cloudResult
          } else if (preferredLocalModel != null) {
            runLocalInference(preferredLocalModel, prompt, configuration, attachments)
          } else {
            cloudResult
          }
        }
      }
    return result.withPersona(personaId)
  }

  suspend fun generateResponse(prompt: String, personaId: UUID?): InferenceResult =
    generateResponse(prompt, personaId, InferenceConfiguration(), PromptAttachments())

  private fun resolvePreference(
    hasLocalCandidate: Boolean,
    isOnline: Boolean,
    userPrefersLocal: Boolean,
  ): Boolean =
    when {
      !hasLocalCandidate -> false
      !isOnline -> true
      else -> userPrefersLocal
    }

  private suspend fun runLocalInference(
    model: ModelPackage,
    prompt: String,
    options: InferenceConfiguration,
    attachments: PromptAttachments,
  ): InferenceResult {
    if (!localModelRuntime.isModelReady(model.modelId)) {
      return NanoAIResult.recoverable(
        message = "Model ${model.displayName} is not installed or ready",
        telemetryId = "LOCAL_MODEL_MISSING",
        context = mapOf("modelId" to model.modelId),
      )
    }

    val request =
      LocalGenerationRequest(
        modelId = model.modelId,
        prompt = prompt,
        systemPrompt = options.systemPrompt,
        temperature = options.temperature,
        topP = options.topP,
        maxOutputTokens = options.maxOutputTokens,
        image = attachments.image.toBitmap(),
        audio = attachments.audio?.bytes,
      )

    return when (val result = localModelRuntime.generate(request)) {
      is NanoAIResult.Success ->
        NanoAIResult.success(
          InferenceSuccessData(
            text = result.value.text,
            source = MessageSource.LOCAL_MODEL,
            latencyMs = result.value.latencyMs,
            metadata =
              result.value.metadata +
                mapOf("modelId" to model.modelId, "providerType" to model.providerType.name),
          )
        )
      is NanoAIResult.RecoverableError ->
        result.copy(
          message = result.message.ifBlank { LOCAL_INFERENCE_FAILURE },
          context = result.context.withModelContext(model.modelId),
        )
      is NanoAIResult.FatalError ->
        result.copy(
          message = result.message.ifBlank { LOCAL_INFERENCE_FAILURE },
          context = result.context.withModelContext(model.modelId),
        )
    }
  }

  private suspend fun runCloudInference(
    prompt: String,
    options: InferenceConfiguration,
  ): InferenceResult {
    val provider = selectCloudProvider()
    val cloudModelId = resolveCloudModel(options.cloudModel)
    return when {
      provider == null -> noProviderError()
      cloudModelId == null -> missingModelError()
      else -> {
        val messages = buildCloudMessages(prompt, options)
        val request = buildCompletionRequest(cloudModelId, messages, options)
        executeCloudRequest(provider, cloudModelId, request)
      }
    }
  }

  private suspend fun selectCloudProvider(): APIProviderConfig? {
    val providers = apiProviderConfigRepository.getEnabledProviders()
    return providers.firstOrNull()
  }

  private suspend fun resolveCloudModel(localPreference: String?): String? {
    localPreference
      ?.takeIf { it.isNotBlank() }
      ?.let {
        return it
      }
    return modelCatalogRepository
      .getAllModels()
      .firstOrNull { it.providerType == ProviderType.CLOUD_API }
      ?.modelId
  }

  private fun buildCloudMessages(
    prompt: String,
    options: InferenceConfiguration,
  ): List<CompletionMessageDto> {
    val messages = mutableListOf<CompletionMessageDto>()
    options.systemPrompt
      ?.takeIf { it.isNotBlank() }
      ?.let { promptText ->
        messages += CompletionMessageDto(role = CompletionRole.SYSTEM, content = promptText)
      }
    messages += CompletionMessageDto(role = CompletionRole.USER, content = prompt)
    return messages
  }

  private fun buildCompletionRequest(
    cloudModelId: String,
    messages: List<CompletionMessageDto>,
    options: InferenceConfiguration,
  ): CompletionRequestDto =
    CompletionRequestDto(
      model = cloudModelId,
      messages = messages,
      temperature = options.temperature?.toDouble(),
      topP = options.topP?.toDouble(),
      maxTokens = options.maxOutputTokens,
      stream = false,
      metadata = options.metadata,
    )

  private suspend fun executeCloudRequest(
    provider: APIProviderConfig,
    cloudModelId: String,
    request: CompletionRequestDto,
  ): InferenceResult {
    val result = cloudGatewayClient.createCompletion(provider, request)
    return when (result) {
      is CloudGatewayResult.Success ->
        mapCompletionSuccess(result, provider.providerId, cloudModelId)
      CloudGatewayResult.Unauthorized -> unauthorizedError(provider.providerId)
      CloudGatewayResult.RateLimited -> rateLimitError(provider.providerId)
      is CloudGatewayResult.HttpError -> httpError(result, provider.providerId)
      is CloudGatewayResult.NetworkError -> networkError(result, provider.providerId)
      is CloudGatewayResult.UnknownError -> unknownError(result, provider.providerId)
    }
  }

  private fun mapCompletionSuccess(
    result: CloudGatewayResult.Success<CompletionResponseDto>,
    providerId: String,
    modelId: String,
  ): InferenceResult {
    val choice =
      result.data.choices.firstOrNull()
        ?: return NanoAIResult.recoverable(
          message = "Cloud provider returned no choices",
          telemetryId = "EMPTY_RESPONSE",
          context = mapOf("providerId" to providerId),
        )
    return NanoAIResult.success(
      InferenceSuccessData(
        text = choice.message.content,
        source = MessageSource.CLOUD_API,
        latencyMs = result.latencyMs,
        metadata =
          mapOf(
            "providerId" to providerId,
            "modelId" to modelId,
            "finishReason" to choice.finishReason,
          ),
      )
    )
  }

  private fun unauthorizedError(providerId: String): InferenceResult =
    NanoAIResult.recoverable(
      message = "Cloud credentials rejected",
      telemetryId = "UNAUTHORIZED",
      context = mapOf("providerId" to providerId),
    )

  private fun rateLimitError(providerId: String): InferenceResult =
    NanoAIResult.recoverable(
      message = "Cloud provider rate limit exceeded",
      telemetryId = "RATE_LIMIT",
      context = mapOf("providerId" to providerId),
    )

  private fun httpError(result: CloudGatewayResult.HttpError, providerId: String): InferenceResult =
    NanoAIResult.recoverable(
      message = result.message ?: "HTTP error ${result.statusCode}",
      telemetryId = "HTTP_${result.statusCode}",
      context = mapOf("providerId" to providerId),
    )

  private fun networkError(
    result: CloudGatewayResult.NetworkError,
    providerId: String,
  ): InferenceResult =
    NanoAIResult.recoverable(
      message = result.throwable.message ?: "Network error occurred",
      telemetryId = "NETWORK_ERROR",
      cause = result.throwable,
      context = mapOf("providerId" to providerId),
    )

  private fun unknownError(
    result: CloudGatewayResult.UnknownError,
    @Suppress("UnusedParameter") providerId: String,
  ): InferenceResult =
    NanoAIResult.fatal(
      message = result.throwable.message ?: "Unknown error occurred",
      supportContact = null,
      telemetryId = "UNKNOWN_ERROR",
      cause = result.throwable,
    )

  private fun noProviderError(): InferenceResult =
    NanoAIResult.recoverable(
      message = "No enabled cloud provider configured",
      telemetryId = "NO_CLOUD_PROVIDER",
    )

  private fun missingModelError(): InferenceResult =
    NanoAIResult.recoverable(
      message = "No cloud model configured for provider",
      telemetryId = "NO_CLOUD_MODEL",
    )

  private fun offlineError(): InferenceResult =
    NanoAIResult.recoverable(
      message = "Device is offline and cloud inference is unavailable",
      telemetryId = "OFFLINE",
    )

  private fun selectLocalModel(
    localModels: List<ModelPackage>,
    preferredModelId: String?,
  ): ModelPackage? {
    if (preferredModelId != null) {
      localModels
        .firstOrNull { it.modelId == preferredModelId }
        ?.let {
          return it
        }
    }
    return localModels.firstOrNull()
  }

  private fun InferenceResult.withPersona(personaId: UUID?): InferenceResult {
    if (personaId == null) return this
    val extra = mapOf("personaId" to personaId.toString())
    return when (this) {
      is NanoAIResult.Success -> {
        val updatedData = value.copy(metadata = value.metadata + extra)
        NanoAIResult.success(updatedData)
      }
      is NanoAIResult.RecoverableError -> copy(context = context + extra)
      is NanoAIResult.FatalError -> this
    }
  }

  private fun Map<String, String>.withModelContext(modelId: String): Map<String, String> {
    if (this["modelId"] == modelId) return this
    return this + ("modelId" to modelId)
  }

  private fun PromptImage?.toBitmap(): Bitmap? {
    this ?: return null
    return runCatching { BitmapFactory.decodeStream(ByteArrayInputStream(bytes)) }.getOrNull()
  }

  companion object {
    private const val LOCAL_INFERENCE_FAILURE = "Local inference failed"
  }
}
