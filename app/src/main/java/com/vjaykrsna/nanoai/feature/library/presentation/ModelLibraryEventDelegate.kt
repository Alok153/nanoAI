package com.vjaykrsna.nanoai.feature.library.presentation

import android.net.Uri
import com.vjaykrsna.nanoai.core.common.fold
import com.vjaykrsna.nanoai.core.common.onFailure
import com.vjaykrsna.nanoai.core.domain.library.DownloadModelUseCase
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceToModelPackageConverter
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.library.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryUiEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal class ModelLibraryEventDelegate
@Inject
constructor(
  private val modelCatalogUseCase: ModelCatalogUseCase,
  private val refreshModelCatalogUseCase: RefreshModelCatalogUseCase,
  private val downloadModelUseCase: DownloadModelUseCase,
  private val hfToModelConverter: HuggingFaceToModelPackageConverter,
  private val huggingFaceLibraryViewModel: HuggingFaceLibraryViewModel,
  private val downloadManager: DownloadManager,
  private val stateStore: ModelLibraryStateStore,
  private val dispatcher: CoroutineDispatcher,
  private val scope: CoroutineScope,
) {
  private val started = AtomicBoolean(false)
  private val _errorEvents = MutableSharedFlow<LibraryError>()
  val errorEvents = _errorEvents.asSharedFlow()

  private val _uiEvents = MutableSharedFlow<LibraryUiEvent>()
  val uiEvents = _uiEvents.asSharedFlow()

  private val huggingFaceCoordinator =
    HuggingFaceDownloadCoordinator(
      converter = hfToModelConverter,
      modelCatalogUseCase = modelCatalogUseCase,
      downloadModelUseCase = downloadModelUseCase,
      emitError = { error -> _errorEvents.emit(error) },
    )

  val downloadActions =
    ModelDownloadActionHandler(
      downloadModelUseCase = downloadModelUseCase,
      downloadManager = downloadManager,
      dispatcher = dispatcher,
      scope = scope,
      emitError = { error -> _errorEvents.emit(error) },
    )

  fun start() {
    if (started.compareAndSet(false, true)) {
      scope.launch(dispatcher) {
        huggingFaceLibraryViewModel.downloadRequests.collect { model ->
          huggingFaceCoordinator.process(model)
        }
      }
      scope.launch(dispatcher) {
        downloadManager.errorEvents.collect { error -> _errorEvents.emit(error) }
      }
    }
  }

  fun refreshCatalog() {
    if (!stateStore.beginRefresh()) return

    scope.launch(dispatcher) {
      try {
        refreshModelCatalogUseCase().onFailure { error ->
          handleRefreshFailure(
            error = error.cause ?: Exception(error.message),
            emitError = { emitted -> _errorEvents.emit(emitted) },
            modelCatalogUseCase = modelCatalogUseCase,
          )
        }
      } finally {
        stateStore.completeRefresh()
      }
    }
  }

  fun requestLocalModelImport() {
    scope.launch(dispatcher) { _uiEvents.emit(LibraryUiEvent.RequestLocalModelImport) }
  }

  fun importLocalModel(@Suppress("UnusedParameter") uri: Uri) {
    scope.launch(dispatcher) {
      _errorEvents.emit(
        LibraryError.UnexpectedError(
          "Manual import isn't available yet. Check curated or Hugging Face tabs for downloads."
        )
      )
    }
  }

  fun requestHuggingFaceDownload(model: HuggingFaceModelSummary) {
    huggingFaceLibraryViewModel.requestDownload(model)
  }
}

private suspend fun handleRefreshFailure(
  error: Throwable,
  emitError: suspend (LibraryError) -> Unit,
  modelCatalogUseCase: ModelCatalogUseCase,
) {
  if (error is CancellationException) throw error

  val rawMessage = error.message?.takeIf { it.isNotBlank() }
  val userMessage = buildString {
    append("Failed to refresh model catalog")
    rawMessage?.let { append(": ").append(it) }
  }

  emitError(LibraryError.UnexpectedError(userMessage))

  val cachedCount =
    modelCatalogUseCase.getAllModels().fold(onSuccess = { it.size }, onFailure = { 0 })
  modelCatalogUseCase.recordOfflineFallback(
    reason = error::class.simpleName ?: "UnknownError",
    cachedCount = cachedCount,
    message = rawMessage,
  )
}
