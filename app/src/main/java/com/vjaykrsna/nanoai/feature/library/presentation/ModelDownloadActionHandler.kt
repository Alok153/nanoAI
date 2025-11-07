package com.vjaykrsna.nanoai.feature.library.presentation

import com.vjaykrsna.nanoai.core.common.onFailure
import com.vjaykrsna.nanoai.core.domain.library.DownloadModelUseCase
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Handles local download lifecycle operations. */
internal class ModelDownloadActionHandler(
  private val downloadModelUseCase: DownloadModelUseCase,
  private val downloadManager: DownloadManager,
  private val dispatcher: CoroutineDispatcher,
  private val scope: CoroutineScope,
  private val emitError: suspend (LibraryError) -> Unit,
) {

  fun downloadModel(modelId: String) {
    scope.launch(dispatcher) {
      downloadModelUseCase.downloadModel(modelId).onFailure { error ->
        emitError(
          LibraryError.DownloadFailed(
            modelId = modelId,
            message = error.message ?: "Failed to start download",
          )
        )
      }
    }
  }

  fun pauseDownload(taskId: UUID) {
    scope.launch(dispatcher) { downloadModelUseCase.pauseDownload(taskId) }
  }

  fun resumeDownload(taskId: UUID) {
    scope.launch(dispatcher) { downloadModelUseCase.resumeDownload(taskId) }
  }

  fun cancelDownload(taskId: UUID) {
    scope.launch(dispatcher) { downloadModelUseCase.cancelDownload(taskId) }
  }

  fun retryDownload(taskId: UUID) {
    scope.launch(dispatcher) { downloadModelUseCase.retryFailedDownload(taskId) }
  }

  fun deleteModel(modelId: String) {
    downloadManager.deleteModel(modelId)
  }

  fun observeDownloadProgress(taskId: UUID): StateFlow<Float> =
    downloadModelUseCase.getDownloadProgress(taskId).stateIn(scope, SharingStarted.Eagerly, 0f)
}
