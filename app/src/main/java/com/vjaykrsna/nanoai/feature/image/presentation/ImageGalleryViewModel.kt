package com.vjaykrsna.nanoai.feature.image.presentation

import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.common.error.toErrorEnvelope
import com.vjaykrsna.nanoai.core.domain.image.ImageGalleryUseCase
import com.vjaykrsna.nanoai.core.domain.image.model.GeneratedImage
import com.vjaykrsna.nanoai.shared.state.NanoAIViewEvent
import com.vjaykrsna.nanoai.shared.state.NanoAIViewState
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHost
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for image gallery screen.
 *
 * Manages gallery state and image deletion.
 */
@HiltViewModel
class ImageGalleryViewModel
@Inject
constructor(
  private val imageGalleryUseCase: ImageGalleryUseCase,
  @MainImmediateDispatcher mainDispatcher: CoroutineDispatcher,
) :
  ViewModelStateHost<ImageGalleryUiState, ImageGalleryUiEvent>(
    initialState = ImageGalleryUiState(),
    dispatcher = mainDispatcher,
  ) {

  private companion object {
    private const val DELETE_IMAGE_ERROR = "Failed to delete image"
    private const val DELETE_ALL_ERROR = "Failed to delete all images"
  }

  init {
    observeImages()
  }

  private fun observeImages() {
    viewModelScope.launch(dispatcher) {
      imageGalleryUseCase.observeAllImages().collectLatest { images ->
        updateState { copy(images = images.toPersistentList(), errorMessage = null) }
      }
    }
  }

  fun deleteImage(id: UUID) {
    viewModelScope.launch(dispatcher) {
      when (val result = imageGalleryUseCase.deleteImage(id)) {
        is NanoAIResult.Success -> emitEvent(ImageGalleryUiEvent.ImageDeleted)
        is NanoAIResult.RecoverableError -> {
          val envelope = result.toErrorEnvelope(DELETE_IMAGE_ERROR)
          emitGalleryError(ImageGalleryError.DeleteFailed(envelope.userMessage), envelope)
        }
        is NanoAIResult.FatalError -> {
          val envelope = result.toErrorEnvelope(DELETE_IMAGE_ERROR)
          emitGalleryError(ImageGalleryError.DeleteFailed(envelope.userMessage), envelope)
        }
      }
    }
  }

  fun deleteAllImages() {
    viewModelScope.launch(dispatcher) {
      when (val result = imageGalleryUseCase.deleteAllImages()) {
        is NanoAIResult.Success -> emitEvent(ImageGalleryUiEvent.AllImagesDeleted)
        is NanoAIResult.RecoverableError -> {
          val envelope = result.toErrorEnvelope(DELETE_ALL_ERROR)
          emitGalleryError(ImageGalleryError.DeleteAllFailed(envelope.userMessage), envelope)
        }
        is NanoAIResult.FatalError -> {
          val envelope = result.toErrorEnvelope(DELETE_ALL_ERROR)
          emitGalleryError(ImageGalleryError.DeleteAllFailed(envelope.userMessage), envelope)
        }
      }
    }
  }

  private suspend fun emitGalleryError(error: ImageGalleryError, envelope: NanoAIErrorEnvelope) {
    updateState { copy(errorMessage = envelope.userMessage) }
    emitEvent(ImageGalleryUiEvent.ErrorRaised(error, envelope))
  }
}

data class ImageGalleryUiState(
  val images: PersistentList<GeneratedImage> = persistentListOf(),
  val errorMessage: String? = null,
) : NanoAIViewState

/** Error states for image gallery operations. */
sealed interface ImageGalleryError {
  val message: String

  data class DeleteFailed(override val message: String) : ImageGalleryError

  data class DeleteAllFailed(override val message: String) : ImageGalleryError

  data class LoadFailed(override val message: String) : ImageGalleryError
}

sealed interface ImageGalleryUiEvent : NanoAIViewEvent {
  data object ImageDeleted : ImageGalleryUiEvent

  data object AllImagesDeleted : ImageGalleryUiEvent

  data class ErrorRaised(val error: ImageGalleryError, val envelope: NanoAIErrorEnvelope) :
    ImageGalleryUiEvent
}
