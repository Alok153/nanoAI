package com.vjaykrsna.nanoai.feature.library.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.onFailure
import com.vjaykrsna.nanoai.core.common.onSuccess
import com.vjaykrsna.nanoai.core.domain.library.DownloadModelUseCase
import com.vjaykrsna.nanoai.core.domain.library.ManageModelUseCase
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadUiCoordinator
@Inject
constructor(
  private val downloadModelUseCase: DownloadModelUseCase,
  private val manageModelUseCase: ManageModelUseCase,
) : ViewModel() {

  private val downloadObservers = mutableMapOf<UUID, Job>()
  private val activeOperations = AtomicInteger(0)
  private val _isLoading = MutableSharedFlow<Boolean>()
  val isLoading: StateFlow<Boolean> =
    _isLoading.stateIn(viewModelScope, SharingStarted.Eagerly, false)

  private val _errorEvents = MutableSharedFlow<LibraryError>()
  val errorEvents = _errorEvents.asSharedFlow()

  fun downloadModel(modelId: String) {
    updateLoadingState(isStarting = true)

    viewModelScope.launch {
      try {
        val result = downloadModelUseCase.downloadModel(modelId)
        result
          .onSuccess { taskId -> monitorDownloadTask(taskId, modelId) }
          .onFailure { error ->
            _errorEvents.emit(
              LibraryError.DownloadFailed(modelId, error.message ?: "Unknown error")
            )
          }
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (error: Throwable) {
        _errorEvents.emit(LibraryError.UnexpectedError(error.message ?: "Unexpected error"))
      } finally {
        updateLoadingState(isStarting = false)
      }
    }
  }

  fun pauseDownload(taskId: UUID) {
    viewModelScope.launch {
      runCatching { downloadModelUseCase.pauseDownload(taskId) }
        .onFailure { error ->
          _errorEvents.emit(
            LibraryError.PauseFailed(taskId.toString(), error.message ?: "Failed to pause")
          )
        }
    }
  }

  fun resumeDownload(taskId: UUID) {
    viewModelScope.launch {
      runCatching { downloadModelUseCase.resumeDownload(taskId) }
        .onFailure { error ->
          _errorEvents.emit(
            LibraryError.ResumeFailed(taskId.toString(), error.message ?: "Failed to resume")
          )
        }
    }
  }

  fun cancelDownload(taskId: UUID) {
    viewModelScope.launch {
      runCatching { downloadModelUseCase.cancelDownload(taskId) }
        .onFailure { error ->
          _errorEvents.emit(
            LibraryError.CancelFailed(taskId.toString(), error.message ?: "Failed to cancel")
          )
        }
    }
  }

  fun retryDownload(taskId: UUID) {
    viewModelScope.launch {
      runCatching { downloadModelUseCase.retryFailedDownload(taskId) }
        .onFailure { error ->
          _errorEvents.emit(
            LibraryError.RetryFailed(taskId.toString(), error.message ?: "Failed to retry")
          )
        }
    }
  }

  fun deleteModel(modelId: String) {
    updateLoadingState(isStarting = true)

    viewModelScope.launch {
      try {
        val result = manageModelUseCase.deleteModel(modelId)
        result.onFailure { error ->
          _errorEvents.emit(LibraryError.DeleteFailed(modelId, error.message ?: "Unknown error"))
        }
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (error: Throwable) {
        _errorEvents.emit(LibraryError.UnexpectedError(error.message ?: "Unexpected error"))
      } finally {
        updateLoadingState(isStarting = false)
      }
    }
  }

  fun observeDownloadProgress(taskId: UUID): StateFlow<Float> =
    downloadModelUseCase
      .getDownloadProgress(taskId)
      .stateIn(viewModelScope, SharingStarted.Eagerly, 0f)

  fun observeDownloadTasks(): StateFlow<List<DownloadTask>> =
    downloadModelUseCase
      .observeDownloadTasks()
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private fun monitorDownloadTask(taskId: UUID, modelId: String) {
    downloadObservers[taskId]?.cancel()
    val job =
      viewModelScope.launch launch@{
        val taskFlow = downloadModelUseCase.observeDownloadTask(taskId)
        taskFlow.collect { task ->
          when (task?.status) {
            DownloadStatus.FAILED -> {
              _errorEvents.emit(
                LibraryError.DownloadFailed(modelId, task.errorMessage ?: "Unknown error")
              )
              cancel()
            }
            DownloadStatus.COMPLETED,
            DownloadStatus.CANCELLED -> cancel()
            else -> Unit
          }
        }
      }
    downloadObservers[taskId] = job
    job.invokeOnCompletion { downloadObservers.remove(taskId) }
  }

  override fun onCleared() {
    super.onCleared()
    downloadObservers.values.forEach { observer -> observer.cancel() }
    downloadObservers.clear()
  }

  private fun updateLoadingState(isStarting: Boolean) {
    val count =
      if (isStarting) {
        activeOperations.incrementAndGet()
      } else {
        activeOperations.updateAndGet { current -> (current - 1).coerceAtLeast(0) }
      }
    val shouldEmit = isStarting && count == 1 || !isStarting && count == 0
    if (shouldEmit) {
      viewModelScope.launch { _isLoading.emit(isStarting) }
    }
  }
}
