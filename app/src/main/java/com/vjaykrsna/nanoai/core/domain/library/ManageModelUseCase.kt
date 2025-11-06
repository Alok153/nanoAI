package com.vjaykrsna.nanoai.core.domain.library

import android.database.sqlite.SQLiteException
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/** Use case for managing installed models (delete, etc.). */
@Singleton
class ManageModelUseCase
@Inject
constructor(private val modelCatalogRepository: ModelCatalogRepository) {
  /** Delete a model if not active in any chat session. */
  suspend fun deleteModel(modelId: String): NanoAIResult<Unit> =
    guardModelCatalogOperation(
      message = "Failed to delete model $modelId",
      context = mapOf("modelId" to modelId),
    ) {
      val inUse = modelCatalogRepository.isModelActiveInSession(modelId)
      if (inUse) {
        return@guardModelCatalogOperation NanoAIResult.recoverable(
          message = "Model $modelId is active in a conversation",
          context = mapOf("modelId" to modelId),
        )
      }

      modelCatalogRepository.deleteModelFiles(modelId)
      modelCatalogRepository.updateInstallState(modelId, InstallState.NOT_INSTALLED)
      modelCatalogRepository.updateDownloadTaskId(modelId, null)
      NanoAIResult.success(Unit)
    }

  private inline fun <T> guardModelCatalogOperation(
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
