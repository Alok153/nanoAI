package com.vjaykrsna.nanoai.core.runtime

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

  override suspend fun generate(request: LocalGenerationRequest): Result<LocalGenerationResult> {
    val model =
      modelCatalogRepository.getModel(request.modelId)
        ?: return Result.failure(IllegalArgumentException("Model not found: ${request.modelId}"))

    return when (model.providerType) {
      ProviderType.MEDIA_PIPE -> mediaPipeInferenceService.generate(request)
      ProviderType.LEAP -> leapInferenceService.generate(request)
      else ->
        Result.failure(
          UnsupportedOperationException("Unsupported provider type: ${model.providerType}")
        )
    }
  }
}
