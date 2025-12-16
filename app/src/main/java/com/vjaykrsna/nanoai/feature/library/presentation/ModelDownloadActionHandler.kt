package com.vjaykrsna.nanoai.feature.library.presentation

import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Handles local download lifecycle operations. */
internal class ModelDownloadActionHandler(
  private val downloadCoordinator: DownloadUiCoordinator,
  private val dispatcher: CoroutineDispatcher,
  private val scope: CoroutineScope,
) {

  fun downloadModel(modelId: String) {
    scope.launch(dispatcher) { downloadCoordinator.downloadModel(modelId) }
  }

  fun pauseDownload(taskId: UUID) {
    scope.launch(dispatcher) { downloadCoordinator.pauseDownload(taskId) }
  }

  fun resumeDownload(taskId: UUID) {
    scope.launch(dispatcher) { downloadCoordinator.resumeDownload(taskId) }
  }

  fun cancelDownload(taskId: UUID) {
    scope.launch(dispatcher) { downloadCoordinator.cancelDownload(taskId) }
  }

  fun retryDownload(taskId: UUID) {
    scope.launch(dispatcher) { downloadCoordinator.retryDownload(taskId) }
  }

  fun deleteModel(modelId: String) {
    downloadCoordinator.deleteModel(modelId)
  }

  fun observeDownloadProgress(taskId: UUID): StateFlow<Float> =
    downloadCoordinator.observeDownloadProgress(taskId)
}
