package com.vjaykrsna.nanoai.feature.library.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.NanoAIResult as CommonNanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import com.vjaykrsna.nanoai.feature.library.domain.CancelModelDownloadUseCase
import com.vjaykrsna.nanoai.feature.library.domain.DeleteModelUseCase
import com.vjaykrsna.nanoai.feature.library.domain.ObserveDownloadProgressUseCase
import com.vjaykrsna.nanoai.feature.library.domain.ObserveDownloadTaskUseCase
import com.vjaykrsna.nanoai.feature.library.domain.ObserveDownloadTasksUseCase
import com.vjaykrsna.nanoai.feature.library.domain.PauseModelDownloadUseCase
import com.vjaykrsna.nanoai.feature.library.domain.QueueModelDownloadUseCase
import com.vjaykrsna.nanoai.feature.library.domain.ResumeModelDownloadUseCase
import com.vjaykrsna.nanoai.feature.library.domain.RetryModelDownloadUseCase
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
  private val queueModelDownloadUseCase: QueueModelDownloadUseCase,
  private val pauseModelDownloadUseCase: PauseModelDownloadUseCase,
  private val resumeModelDownloadUseCase: ResumeModelDownloadUseCase,
  private val cancelModelDownloadUseCase: CancelModelDownloadUseCase,
  private val retryModelDownloadUseCase: RetryModelDownloadUseCase,
  private val deleteModelUseCase: DeleteModelUseCase,
  private val observeDownloadTaskUseCase: ObserveDownloadTaskUseCase,
  private val observeDownloadTasksUseCase: ObserveDownloadTasksUseCase,
  private val observeDownloadProgressUseCase: ObserveDownloadProgressUseCase,
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
        when (val result = queueModelDownloadUseCase(modelId)) {
          is CommonNanoAIResult.Success -> monitorDownloadTask(result.value, modelId)
          is CommonNanoAIResult.RecoverableError ->
            _errorEvents.emit(
              LibraryError.DownloadFailed(modelId, result.message ?: "Unable to start download")
            )
          is CommonNanoAIResult.FatalError ->
            _errorEvents.emit(
              LibraryError.DownloadFailed(modelId, result.message ?: "Unable to start download")
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
      when (val result = pauseModelDownloadUseCase(taskId)) {
        is CommonNanoAIResult.Success -> Unit
        is CommonNanoAIResult.RecoverableError ->
          _errorEvents.emit(
            LibraryError.PauseFailed(taskId.toString(), result.message ?: "Failed to pause")
          )
        is CommonNanoAIResult.FatalError ->
          _errorEvents.emit(
            LibraryError.PauseFailed(taskId.toString(), result.message ?: "Failed to pause")
          )
      }
    }
  }

  fun resumeDownload(taskId: UUID) {
    viewModelScope.launch {
      when (val result = resumeModelDownloadUseCase(taskId)) {
        is CommonNanoAIResult.Success -> Unit
        is CommonNanoAIResult.RecoverableError ->
          _errorEvents.emit(
            LibraryError.ResumeFailed(taskId.toString(), result.message ?: "Failed to resume")
          )
        is CommonNanoAIResult.FatalError ->
          _errorEvents.emit(
            LibraryError.ResumeFailed(taskId.toString(), result.message ?: "Failed to resume")
          )
      }
    }
  }

  fun cancelDownload(taskId: UUID) {
    viewModelScope.launch {
      when (val result = cancelModelDownloadUseCase(taskId)) {
        is CommonNanoAIResult.Success -> Unit
        is CommonNanoAIResult.RecoverableError ->
          _errorEvents.emit(
            LibraryError.CancelFailed(taskId.toString(), result.message ?: "Failed to cancel")
          )
        is CommonNanoAIResult.FatalError ->
          _errorEvents.emit(
            LibraryError.CancelFailed(taskId.toString(), result.message ?: "Failed to cancel")
          )
      }
    }
  }

  fun retryDownload(taskId: UUID) {
    viewModelScope.launch {
      when (val result = retryModelDownloadUseCase(taskId)) {
        is CommonNanoAIResult.Success -> Unit
        is CommonNanoAIResult.RecoverableError ->
          _errorEvents.emit(
            LibraryError.RetryFailed(taskId.toString(), result.message ?: "Failed to retry")
          )
        is CommonNanoAIResult.FatalError ->
          _errorEvents.emit(
            LibraryError.RetryFailed(taskId.toString(), result.message ?: "Failed to retry")
          )
      }
    }
  }

  fun deleteModel(modelId: String) {
    updateLoadingState(isStarting = true)

    viewModelScope.launch {
      try {
        when (val result = deleteModelUseCase(modelId)) {
          is CommonNanoAIResult.Success -> Unit
          is CommonNanoAIResult.RecoverableError ->
            _errorEvents.emit(LibraryError.DeleteFailed(modelId, result.message ?: "Unknown error"))
          is CommonNanoAIResult.FatalError ->
            _errorEvents.emit(LibraryError.DeleteFailed(modelId, result.message ?: "Unknown error"))
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
    observeDownloadProgressUseCase(taskId).stateIn(viewModelScope, SharingStarted.Eagerly, 0f)

  fun observeDownloadTasks(): StateFlow<List<DownloadTask>> =
    observeDownloadTasksUseCase().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private fun monitorDownloadTask(taskId: UUID, modelId: String) {
    downloadObservers[taskId]?.cancel()
    val job =
      viewModelScope.launch launch@{
        val taskFlow = observeDownloadTaskUseCase(taskId)
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
