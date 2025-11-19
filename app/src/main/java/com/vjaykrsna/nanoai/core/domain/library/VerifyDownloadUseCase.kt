package com.vjaykrsna.nanoai.core.domain.library

import android.database.sqlite.SQLiteException
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
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
  @OneShot("Verify download checksum")
  suspend fun invoke(modelId: String): NanoAIResult<Boolean> =
    guardVerificationOperation(
      message = "Failed to verify checksum for model $modelId",
      context = mapOf("modelId" to modelId),
    ) {
      when (val validation = validateModelAndChecksums(modelId)) {
        is ValidationResult.Success -> {
          val (model, expectedChecksum, actualChecksum) = validation.data
          val matches = expectedChecksum.equals(actualChecksum, ignoreCase = true)
          updateInstallState(modelId, model, matches)
          NanoAIResult.success(matches)
        }
        is ValidationResult.Error -> validation.error
      }
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
      model.downloadTaskId?.let { downloadManager.updateTaskStatus(it, DownloadStatus.COMPLETED) }
    } else {
      modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR)
    }
  }

  private inline fun <T> guardVerificationOperation(
    message: String,
    context: Map<String, String>,
    block: () -> NanoAIResult<T>,
  ): NanoAIResult<T> {
    return try {
      block()
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (sqliteException: SQLiteException) {
      NanoAIResult.recoverable(message = message, cause = sqliteException, context = context)
    } catch (ioException: IOException) {
      NanoAIResult.recoverable(message = message, cause = ioException, context = context)
    } catch (illegalStateException: IllegalStateException) {
      NanoAIResult.recoverable(message = message, cause = illegalStateException, context = context)
    } catch (illegalArgumentException: IllegalArgumentException) {
      NanoAIResult.recoverable(
        message = message,
        cause = illegalArgumentException,
        context = context,
      )
    }
  }
}
