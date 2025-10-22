package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import javax.inject.Inject
import javax.inject.Singleton

/** Use case for managing installed models (delete, etc.). */
@Singleton
class ManageModelUseCase
@Inject
constructor(private val modelCatalogRepository: ModelCatalogRepository) {
  /** Delete a model if not active in any chat session. */
  suspend fun deleteModel(modelId: String): NanoAIResult<Unit> {
    return try {
      val inUse = modelCatalogRepository.isModelActiveInSession(modelId)
      if (inUse) {
        return NanoAIResult.recoverable(
          message = "Model $modelId is active in a conversation",
          context = mapOf("modelId" to modelId),
        )
      }

      modelCatalogRepository.deleteModelFiles(modelId)
      modelCatalogRepository.updateInstallState(modelId, InstallState.NOT_INSTALLED)
      modelCatalogRepository.updateDownloadTaskId(modelId, null)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to delete model $modelId",
        cause = e,
        context = mapOf("modelId" to modelId),
      )
    }
  }
}
