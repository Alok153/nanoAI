package com.vjaykrsna.nanoai.feature.chat.domain

import com.vjaykrsna.nanoai.core.data.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.data.repository.InferencePreferenceRepository
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.model.InferenceMode
import com.vjaykrsna.nanoai.core.model.MessageSource
import com.vjaykrsna.nanoai.core.network.CloudGatewayClient
import com.vjaykrsna.nanoai.core.network.CloudGatewayResult
import com.vjaykrsna.nanoai.core.network.ConnectivityStatusProvider
import com.vjaykrsna.nanoai.core.network.dto.CompletionMessageDto
import com.vjaykrsna.nanoai.core.network.dto.CompletionRequestDto
import com.vjaykrsna.nanoai.core.network.dto.CompletionRole
import com.vjaykrsna.nanoai.core.runtime.LocalGenerationRequest
import com.vjaykrsna.nanoai.core.runtime.LocalModelRuntime
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates local MediaPipe inference with cloud fallback via the gateway client.
 */
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
         * Generate a response using either the local runtime or the cloud gateway based on
         * [preferLocal] and availability. The returned [InferenceResult] captures success or
         * failure codes for downstream use cases.
         */
        suspend fun generateResponse(
            prompt: String,
            personaId: UUID?,
            options: GenerationOptions = GenerationOptions(),
        ): InferenceResult {
            val installedModels = modelCatalogRepository.getInstalledModels()
            val localCandidates = installedModels.filterNot { it.providerType == ProviderType.CLOUD_API }
            val inferencePreference = inferencePreferenceRepository.observeInferencePreference().first()
            val userPrefersLocal = inferencePreference.mode == InferenceMode.LOCAL_FIRST
            val isOnline = isOnline()
            val preferLocal = resolvePreference(localCandidates.isNotEmpty(), isOnline, userPrefersLocal)

            val preferredLocalModel = selectLocalModel(localCandidates, options.localModelPreference)

            if (preferLocal && preferredLocalModel != null) {
                val localResult = runLocalInference(preferredLocalModel, prompt, options)
                if (localResult is InferenceResult.Success) {
                    return localResult
                }
                // Fallback to cloud when local failed but network is available.
                if (localResult is InferenceResult.Error && !isOnline) {
                    return localResult
                }
            }

            if (!isOnline) {
                return InferenceResult.Error(
                    errorCode = "OFFLINE",
                    message = "Device is offline and cloud inference is unavailable",
                )
            }

            val cloudResult = runCloudInference(prompt, options)
            if (cloudResult is InferenceResult.Success) {
                return cloudResult
            }

            // As a resilience measure, attempt local inference even when not preferred if cloud fails.
            if (!preferLocal && preferredLocalModel != null) {
                val localResult = runLocalInference(preferredLocalModel, prompt, options)
                if (localResult is InferenceResult.Success) {
                    return localResult
                }
            }

            return cloudResult
        }

        private fun resolvePreference(
            hasLocalCandidate: Boolean,
            isOnline: Boolean,
            userPrefersLocal: Boolean,
        ): Boolean {
            if (!hasLocalCandidate) return false
            if (!isOnline) return true
            return userPrefersLocal
        }

        private suspend fun runLocalInference(
            model: ModelPackage,
            prompt: String,
            options: GenerationOptions,
        ): InferenceResult {
            if (!localModelRuntime.isModelReady(model.modelId)) {
                return InferenceResult.Error(
                    errorCode = "LOCAL_MODEL_MISSING",
                    message = "Model ${model.displayName} is not installed or ready",
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
                )

            return localModelRuntime
                .generate(request)
                .fold(
                    onSuccess = { result ->
                        InferenceResult.Success(
                            text = result.text,
                            source = MessageSource.LOCAL_MODEL,
                            latencyMs = result.latencyMs,
                            metadata =
                                result.metadata +
                                    mapOf(
                                        "modelId" to model.modelId,
                                        "providerType" to model.providerType.name,
                                    ),
                        )
                    },
                    onFailure = { throwable ->
                        InferenceResult.Error(
                            errorCode = "LOCAL_INFERENCE_ERROR",
                            message = throwable.message,
                            cause = throwable,
                            metadata = mapOf("modelId" to model.modelId),
                        )
                    },
                )
        }

        private suspend fun runCloudInference(
            prompt: String,
            options: GenerationOptions,
        ): InferenceResult {
            val providers = apiProviderConfigRepository.getEnabledProviders()
            val provider =
                providers.firstOrNull()
                    ?: return InferenceResult.Error(
                        errorCode = "NO_CLOUD_PROVIDER",
                        message = "No enabled cloud provider configured",
                    )

            val cloudModelId =
                options.cloudModel
                    ?: modelCatalogRepository
                        .getAllModels()
                        .firstOrNull { it.providerType == ProviderType.CLOUD_API }
                        ?.modelId
                    ?: return InferenceResult.Error(
                        errorCode = "NO_CLOUD_MODEL",
                        message = "No cloud model configured for provider",
                    )

            val messages =
                buildList {
                    options.systemPrompt?.takeIf { it.isNotBlank() }?.let { promptText ->
                        add(CompletionMessageDto(role = CompletionRole.SYSTEM, content = promptText))
                    }
                    add(CompletionMessageDto(role = CompletionRole.USER, content = prompt))
                }

            val request =
                CompletionRequestDto(
                    model = cloudModelId,
                    messages = messages,
                    temperature = options.temperature?.toDouble(),
                    topP = options.topP?.toDouble(),
                    maxTokens = options.maxOutputTokens,
                    stream = false,
                    metadata = options.metadata,
                )

            return when (val result = cloudGatewayClient.createCompletion(provider, request)) {
                is CloudGatewayResult.Success -> {
                    val choice =
                        result.data.choices.firstOrNull()
                            ?: return InferenceResult.Error(
                                errorCode = "EMPTY_RESPONSE",
                                message = "Cloud provider returned no choices",
                                metadata = mapOf("providerId" to provider.providerId),
                            )
                    InferenceResult.Success(
                        text = choice.message.content,
                        source = MessageSource.CLOUD_API,
                        latencyMs = result.latencyMs,
                        metadata =
                            mapOf(
                                "providerId" to provider.providerId,
                                "modelId" to cloudModelId,
                                "finishReason" to choice.finishReason,
                            ),
                    )
                }
                CloudGatewayResult.Unauthorized ->
                    InferenceResult.Error(
                        errorCode = "UNAUTHORIZED",
                        message = "Cloud credentials rejected",
                        metadata = mapOf("providerId" to provider.providerId),
                    )
                CloudGatewayResult.RateLimited ->
                    InferenceResult.Error(
                        errorCode = "RATE_LIMIT",
                        message = "Cloud provider rate limit exceeded",
                        metadata = mapOf("providerId" to provider.providerId),
                    )
                is CloudGatewayResult.HttpError ->
                    InferenceResult.Error(
                        errorCode = "HTTP_${result.statusCode}",
                        message = result.message,
                        metadata = mapOf("providerId" to provider.providerId),
                    )
                is CloudGatewayResult.NetworkError ->
                    InferenceResult.Error(
                        errorCode = "NETWORK_ERROR",
                        message = result.throwable.message,
                        cause = result.throwable,
                        metadata = mapOf("providerId" to provider.providerId),
                    )
                is CloudGatewayResult.UnknownError ->
                    InferenceResult.Error(
                        errorCode = "UNKNOWN_ERROR",
                        message = result.throwable.message,
                        cause = result.throwable,
                        metadata = mapOf("providerId" to provider.providerId),
                    )
            }
        }

        private fun selectLocalModel(
            localModels: List<ModelPackage>,
            preferredModelId: String?,
        ): ModelPackage? {
            if (preferredModelId != null) {
                localModels.firstOrNull { it.modelId == preferredModelId }?.let { return it }
            }
            return localModels.firstOrNull()
        }
    }
