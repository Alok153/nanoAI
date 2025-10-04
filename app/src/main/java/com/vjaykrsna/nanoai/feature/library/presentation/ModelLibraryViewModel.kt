package com.vjaykrsna.nanoai.feature.library.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.domain.ModelDownloadsAndExportUseCase
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val FLOW_STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class ModelLibraryViewModel
@Inject
constructor(
  private val modelDownloadsAndExportUseCase: ModelDownloadsAndExportUseCase,
  private val modelCatalogRepository: ModelCatalogRepository,
) : ViewModel() {
  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorEvents = MutableSharedFlow<LibraryError>()
  val errorEvents = _errorEvents.asSharedFlow()

  private val _selectedFilter = MutableStateFlow<InstallStateFilter>(InstallStateFilter.ALL)
  val selectedFilter: StateFlow<InstallStateFilter> = _selectedFilter.asStateFlow()

  val allModels: StateFlow<List<ModelPackage>> =
    modelCatalogRepository
      .observeAllModels()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS), emptyList())

  val installedModels: StateFlow<List<ModelPackage>> =
    modelCatalogRepository
      .observeInstalledModels()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS), emptyList())

  val filteredModels: StateFlow<List<ModelPackage>> =
    combine(allModels, _selectedFilter) { models, filter ->
        when (filter) {
          InstallStateFilter.ALL -> models
          InstallStateFilter.INSTALLED ->
            models.filter { it.installState == InstallState.INSTALLED }
          InstallStateFilter.DOWNLOADING ->
            models.filter {
              it.installState == InstallState.DOWNLOADING || it.installState == InstallState.PAUSED
            }
          InstallStateFilter.AVAILABLE ->
            models.filter { it.installState == InstallState.NOT_INSTALLED }
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS), emptyList())

  val queuedDownloads: StateFlow<List<DownloadTask>> =
    modelDownloadsAndExportUseCase
      .getQueuedDownloads()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS), emptyList())

  fun downloadModel(modelId: String) {
    viewModelScope.launch {
      _isLoading.value = true
      runCatching { modelDownloadsAndExportUseCase.downloadModel(modelId) }
        .onSuccess { result ->
          result.onFailure { error ->
            _errorEvents.emit(
              LibraryError.DownloadFailed(modelId, error.message ?: "Unknown error"),
            )
          }
        }
        .onFailure { error ->
          _errorEvents.emit(LibraryError.UnexpectedError(error.message ?: "Unexpected error"))
        }
      _isLoading.value = false
    }
  }

  fun pauseDownload(taskId: UUID) {
    viewModelScope.launch {
      runCatching { modelDownloadsAndExportUseCase.pauseDownload(taskId) }
        .onFailure { error ->
          _errorEvents.emit(
            LibraryError.PauseFailed(taskId.toString(), error.message ?: "Failed to pause"),
          )
        }
    }
  }

  fun resumeDownload(taskId: UUID) {
    viewModelScope.launch {
      runCatching { modelDownloadsAndExportUseCase.resumeDownload(taskId) }
        .onFailure { error ->
          _errorEvents.emit(
            LibraryError.ResumeFailed(taskId.toString(), error.message ?: "Failed to resume"),
          )
        }
    }
  }

  fun cancelDownload(taskId: UUID) {
    viewModelScope.launch {
      runCatching { modelDownloadsAndExportUseCase.cancelDownload(taskId) }
        .onFailure { error ->
          _errorEvents.emit(
            LibraryError.CancelFailed(taskId.toString(), error.message ?: "Failed to cancel"),
          )
        }
    }
  }

  fun retryDownload(taskId: UUID) {
    viewModelScope.launch {
      runCatching { modelDownloadsAndExportUseCase.retryFailedDownload(taskId) }
        .onFailure { error ->
          _errorEvents.emit(
            LibraryError.RetryFailed(taskId.toString(), error.message ?: "Failed to retry"),
          )
        }
    }
  }

  fun deleteModel(modelId: String) {
    viewModelScope.launch {
      _isLoading.value = true
      runCatching { modelDownloadsAndExportUseCase.deleteModel(modelId) }
        .onSuccess { result ->
          result.onFailure { error ->
            _errorEvents.emit(
              LibraryError.DeleteFailed(modelId, error.message ?: "Unknown error"),
            )
          }
        }
        .onFailure { error ->
          _errorEvents.emit(LibraryError.UnexpectedError(error.message ?: "Unexpected error"))
        }
      _isLoading.value = false
    }
  }

  fun observeDownloadProgress(taskId: UUID): StateFlow<Float> =
    modelDownloadsAndExportUseCase
      .getDownloadProgress(taskId)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS), 0f)

  fun setFilter(filter: InstallStateFilter) {
    _selectedFilter.value = filter
  }
}

enum class InstallStateFilter {
  ALL,
  INSTALLED,
  DOWNLOADING,
  AVAILABLE,
}

sealed class LibraryError {
  data class DownloadFailed(
    val modelId: String,
    val message: String,
  ) : LibraryError()

  data class PauseFailed(
    val taskId: String,
    val message: String,
  ) : LibraryError()

  data class ResumeFailed(
    val taskId: String,
    val message: String,
  ) : LibraryError()

  data class CancelFailed(
    val taskId: String,
    val message: String,
  ) : LibraryError()

  data class RetryFailed(
    val taskId: String,
    val message: String,
  ) : LibraryError()

  data class DeleteFailed(
    val modelId: String,
    val message: String,
  ) : LibraryError()

  data class UnexpectedError(
    val message: String,
  ) : LibraryError()
}
