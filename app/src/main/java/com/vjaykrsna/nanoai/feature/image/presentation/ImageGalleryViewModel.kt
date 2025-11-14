package com.vjaykrsna.nanoai.feature.image.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.image.ImageGalleryUseCase
import com.vjaykrsna.nanoai.core.domain.image.model.GeneratedImage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for image gallery screen.
 *
 * Manages gallery state and image deletion.
 */
@HiltViewModel
class ImageGalleryViewModel
@Inject
constructor(private val imageGalleryUseCase: ImageGalleryUseCase) : ViewModel() {

  private companion object {
    private const val STATE_FLOW_STOP_TIMEOUT_MS = 5000L
    private const val DELETE_IMAGE_ERROR = "Failed to delete image"
    private const val DELETE_ALL_ERROR = "Failed to delete all images"
  }

  val images: StateFlow<List<GeneratedImage>> =
    imageGalleryUseCase
      .observeAllImages()
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STATE_FLOW_STOP_TIMEOUT_MS),
        emptyList(),
      )

  private val _events = MutableSharedFlow<ImageGalleryEvent>()
  val events = _events.asSharedFlow()

  fun deleteImage(id: UUID) {
    viewModelScope.launch {
      when (val result = imageGalleryUseCase.deleteImage(id)) {
        is NanoAIResult.Success -> _events.emit(ImageGalleryEvent.ImageDeleted)
        is NanoAIResult.RecoverableError ->
          _events.emit(ImageGalleryEvent.Error(result.message.ifBlank { DELETE_IMAGE_ERROR }))
        is NanoAIResult.FatalError ->
          _events.emit(ImageGalleryEvent.Error(result.message.ifBlank { DELETE_IMAGE_ERROR }))
      }
    }
  }

  fun deleteAllImages() {
    viewModelScope.launch {
      when (val result = imageGalleryUseCase.deleteAllImages()) {
        is NanoAIResult.Success -> _events.emit(ImageGalleryEvent.AllImagesDeleted)
        is NanoAIResult.RecoverableError ->
          _events.emit(ImageGalleryEvent.Error(result.message.ifBlank { DELETE_ALL_ERROR }))
        is NanoAIResult.FatalError ->
          _events.emit(ImageGalleryEvent.Error(result.message.ifBlank { DELETE_ALL_ERROR }))
      }
    }
  }
}

/** Events for image gallery screen. */
sealed interface ImageGalleryEvent {
  data object ImageDeleted : ImageGalleryEvent

  data object AllImagesDeleted : ImageGalleryEvent

  data class Error(val message: String) : ImageGalleryEvent
}
