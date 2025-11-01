package com.vjaykrsna.nanoai.feature.chat.domain

import android.graphics.Bitmap
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.data.repository.InferencePreferenceRepository
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
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
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.domain.ProviderType
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
) {
  /** True when the device currently has validated internet connectivity. */
  suspend fun isOnline(): Boolean = connectivityStatusProvider.isOnline()

  /** True when any installed local model is ready for inference. */
  suspend fun hasLocalModelAvailable(): Boolean {
    val installed = modelCatalogRepository.getInstalledModels()
    val localCandidates = installed.filterNot { it.providerType == ProviderType.CLOUD_API }
    if (localCandidates.isEmpty()) return false
    return localModelRuntime.hasReadyModel(localCandidates)
  }

  /**
   * Generate a response using either the local runtime or the cloud gateway based on [preferLocal]
   * and availability. The returned [InferenceResult] captures success or failure codes for
   * downstream use cases.
   */
  suspend fun generateResponse(
    prompt: String,
    personaId: UUID?,
    options: InferenceConfiguration = InferenceConfiguration(),
    image: Bitmap? = null,
    audio: ByteArray? = null,
  ): InferenceResult {
    val installedModels = modelCatalogRepository.getInstalledModels()
    val localCandidates = installedModels.filterNot { it.providerType == ProviderType.CLOUD_API }
    val inferencePreference = inferencePreferenceRepository.observeInferencePreference().first()
    val userPrefersLocal = inferencePreference.mode == InferenceMode.LOCAL_FIRST
    val isOnline = isOnline()
    val preferLocal = resolvePreference(localCandidates.isNotEmpty(), isOnline, userPrefersLocal)
    val preferredLocalModel = selectLocalModel(localCandidates, options.localModelPreference)

    val result = if (preferLocal) {
      val localResult = runLocalInference(preferredLocalModel!!, prompt, options, image, audio)
      if (localResult is NanoAIResult.Success) {
        localResult
      } else if (!isOnline) {
        localResult
      } else {
        runCloudInference(prompt, options)
      }
    } else {
      if (!isOnline) {
        offlineError()
      } else {
        val cloudResult = runCloudInference(prompt, options)
        if (cloudResult is NanoAIResult.Success) {
          cloudResult
        } else if (preferredLocalModel != null) {
          runLocalInference(preferredLocalModel, prompt, options, image, audio)
        } else {
          cloudResult
        }
      }
    }
    return result.withPersona(personaId)
  }

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
    image: Bitmap? = null,
    audio: ByteArray? = null,
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
        image = image,
        audio = audio,
      )

    return localModelRuntime
      .generate(request)
      .fold(
        onSuccess = { result ->
          NanoAIResult.success(
            InferenceSuccessData(
              text = result.text,
              source = MessageSource.LOCAL_MODEL,
              latencyMs = result.latencyMs,
              metadata =
                result.metadata +
                  mapOf("modelId" to model.modelId, "providerType" to model.providerType.name),
            )
          )
        },
        onFailure = { throwable ->
          NanoAIResult.recoverable(
            message = throwable.message ?: "Local inference failed",
            telemetryId = "LOCAL_INFERENCE_ERROR",
            cause = throwable,
            context = mapOf("modelId" to model.modelId),
          )
        },
      )
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
}
