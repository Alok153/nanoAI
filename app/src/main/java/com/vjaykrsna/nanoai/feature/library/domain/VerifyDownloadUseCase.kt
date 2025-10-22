package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.data.DownloadManager
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Use case for verifying downloaded model checksums. */
@Singleton
class VerifyDownloadUseCase
@Inject
constructor(
  private val modelCatalogRepository: ModelCatalogRepository,
  private val downloadManager: DownloadManager,
) {
  /** Validate downloaded checksum and update install state accordingly. */
  suspend fun invoke(modelId: String): NanoAIResult<Boolean> =
    try {
      when (val validation = validateModelAndChecksums(modelId)) {
        is ValidationResult.Success -> {
          val (model, expectedChecksum, actualChecksum) = validation.data
          val matches = expectedChecksum.equals(actualChecksum, ignoreCase = true)
          updateInstallState(modelId, model, matches)
          NanoAIResult.success(matches)
        }
        is ValidationResult.Error -> validation.error
      }
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to verify checksum for model $modelId",
        cause = e,
        context = mapOf("modelId" to modelId),
      )
    }

  private suspend fun validateModelAndChecksums(modelId: String): ValidationResult {
    val model = modelCatalogRepository.getModelById(modelId).first()
    val expectedChecksum = model?.checksumSha256
    val actualChecksum = model?.let { downloadManager.getDownloadedChecksum(modelId) }

    return when {
      model == null ->
        ValidationResult.Error(
          NanoAIResult.recoverable(
            message = "Model $modelId not found",
            context = mapOf("modelId" to modelId),
          )
        )
      expectedChecksum == null -> {
        modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR)
        ValidationResult.Error(
          NanoAIResult.recoverable(
            message = "No checksum available for model $modelId",
            context = mapOf("modelId" to modelId),
          )
        )
      }
      actualChecksum == null -> {
        modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR)
        ValidationResult.Error(
          NanoAIResult.recoverable(
            message = "Downloaded checksum not available for model $modelId",
            context = mapOf("modelId" to modelId),
          )
        )
      }
      else -> ValidationResult.Success(Triple(model, expectedChecksum, actualChecksum))
    }
  }

  private sealed class ValidationResult {
    data class Success(val data: Triple<ModelPackage, String, String>) : ValidationResult()

    data class Error(val error: NanoAIResult<Boolean>) : ValidationResult()
  }

  private suspend fun updateInstallState(modelId: String, model: ModelPackage, matches: Boolean) {
    if (matches) {
      modelCatalogRepository.updateInstallState(modelId, InstallState.INSTALLED)
      modelCatalogRepository.updateChecksum(modelId, model.checksumSha256!!)
      model.downloadTaskId?.let {
        downloadManager.updateTaskStatus(
          it,
          com.vjaykrsna.nanoai.feature.library.model.DownloadStatus.COMPLETED,
        )
      }
    } else {
      modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR)
    }
  }
}
