package com.vjaykrsna.nanoai.core.runtime

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogRepository
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import javax.inject.Inject
import javax.inject.Singleton

/** A dispatcher that routes inference requests to the appropriate [InferenceService]. */
@Singleton
class LocalModelRuntimeImpl
@Inject
constructor(
  private val modelCatalogRepository: ModelCatalogRepository,
  private val mediaPipeInferenceService: MediaPipeInferenceService,
  private val leapInferenceService: LeapInferenceService,
) : LocalModelRuntime {

  override suspend fun isModelReady(modelId: String): Boolean {
    val model = modelCatalogRepository.getModel(modelId) ?: return false

    return when (model.providerType) {
      ProviderType.MEDIA_PIPE -> mediaPipeInferenceService.isModelReady(modelId)
      ProviderType.LEAP -> leapInferenceService.isModelReady(modelId)
      else -> false
    }
  }

  override suspend fun hasReadyModel(models: List<ModelPackage>): Boolean {
    for (model in models) {
      if (isModelReady(model.modelId)) {
        return true
      }
    }
    return false
  }

  override suspend fun generate(
    request: LocalGenerationRequest
  ): NanoAIResult<LocalGenerationResult> {
    val model =
      modelCatalogRepository.getModel(request.modelId)
        ?: return NanoAIResult.recoverable(
          message = "Model ${request.modelId} is not installed",
          telemetryId = "LOCAL_MODEL_NOT_FOUND",
          context = mapOf("modelId" to request.modelId),
        )

    return when (model.providerType) {
      ProviderType.MEDIA_PIPE -> mediaPipeInferenceService.generate(request)
      ProviderType.LEAP -> leapInferenceService.generate(request)
      else ->
        NanoAIResult.recoverable(
          message = "Unsupported provider type: ${model.providerType}",
          telemetryId = "UNSUPPORTED_PROVIDER",
          context = mapOf("modelId" to request.modelId, "providerType" to model.providerType.name),
        )
    }
  }
}
