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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * ViewModel for image generation feature.
 *
 * Manages prompt input, positive/negative prompts, image generation state, and error handling.
 *
 * **Status: Experimental Preview**
 *
 * This feature is currently in preview mode with simulated image generation. The UI and state
 * management are fully implemented, but actual AI-powered image generation requires integration
 * with an on-device diffusion model (e.g., Stable Diffusion via MediaPipe or ONNX runtime).
 *
 * Current limitations:
 * - Image generation is simulated with a delay
 * - Generated images are placeholder paths only
 * - Thumbnail generation is not implemented
 *
 * @see ImageGalleryUseCase for image persistence
 */
@HiltViewModel
class ImageGenerationViewModel
@Inject
constructor(
  private val imageGalleryUseCase: ImageGalleryUseCase,
  @MainImmediateDispatcher mainDispatcher: CoroutineDispatcher,
) :
  ViewModelStateHost<ImageGenerationUiState, ImageGenerationUiEvent>(
    initialState = ImageGenerationUiState(),
    dispatcher = mainDispatcher,
  ) {

  private companion object {
    private const val SIMULATION_DELAY_MS = 2000L
    private const val PROMPT_VALIDATION_ERROR = "Prompt cannot be empty"
    private const val SAVE_IMAGE_ERROR = "Failed to save image"
    private const val PREVIEW_MODE_MESSAGE =
      "Preview mode: Image generation is simulated. Full implementation coming soon."
  }

  fun updatePrompt(prompt: String) {
    updateState { copy(prompt = prompt) }
  }

  fun updateNegativePrompt(negativePrompt: String) {
    updateState { copy(negativePrompt = negativePrompt) }
  }

  fun updateWidth(width: Int) {
    updateState { copy(width = width) }
  }

  fun updateHeight(height: Int) {
    updateState { copy(height = height) }
  }

  fun updateSteps(steps: Int) {
    updateState { copy(steps = steps) }
  }

  fun updateGuidanceScale(scale: Float) {
    updateState { copy(guidanceScale = scale) }
  }

  fun generateImage() {
    viewModelScope.launch(dispatcher) {
      val snapshot = state.value
      if (snapshot.prompt.isBlank()) {
        val envelope = NanoAIErrorEnvelope(userMessage = PROMPT_VALIDATION_ERROR)
        publishError(ImageGenerationError.ValidationError(envelope.userMessage), envelope)
        return@launch
      }

      updateState { copy(isGenerating = true, errorMessage = null) }

      // TODO: Implement actual image generation logic
      // For now, simulate generation
      delay(SIMULATION_DELAY_MS)

      // TODO: Replace with actual generated image path
      val imagePath = "simulated_path_${UUID.randomUUID()}.png"

      // Save generated image with metadata
      val generatedImage =
        GeneratedImage(
          id = UUID.randomUUID(),
          prompt = snapshot.prompt,
          negativePrompt = snapshot.negativePrompt,
          width = snapshot.width,
          height = snapshot.height,
          steps = snapshot.steps,
          guidanceScale = snapshot.guidanceScale,
          filePath = imagePath,
          thumbnailPath = null, // TODO: Generate thumbnail
          createdAt = Clock.System.now(),
        )

      when (val saveResult = imageGalleryUseCase.saveImage(generatedImage)) {
        is NanoAIResult.Success -> {
          updateState {
            copy(
              isGenerating = false,
              generatedImagePath = imagePath,
              errorMessage = PREVIEW_MODE_MESSAGE,
            )
          }
          emitEvent(ImageGenerationUiEvent.ImageGenerated(imagePath))
        }
        is NanoAIResult.RecoverableError -> {
          val envelope = saveResult.toErrorEnvelope(SAVE_IMAGE_ERROR)
          publishError(ImageGenerationError.GenerationError(envelope.userMessage), envelope)
          return@launch
        }
        is NanoAIResult.FatalError -> {
          val envelope = saveResult.toErrorEnvelope(SAVE_IMAGE_ERROR)
          publishError(ImageGenerationError.GenerationError(envelope.userMessage), envelope)
          return@launch
        }
      }
    }
  }

  fun clearImage() {
    updateState { copy(generatedImagePath = null) }
  }

  fun clearError() {
    updateState { copy(errorMessage = null) }
  }

  private suspend fun publishError(error: ImageGenerationError, envelope: NanoAIErrorEnvelope) {
    updateState {
      copy(isGenerating = false, generatedImagePath = null, errorMessage = envelope.userMessage)
    }
    emitEvent(ImageGenerationUiEvent.ErrorRaised(error, envelope))
  }
}

/** UI state for image generation screen. */
data class ImageGenerationUiState(
  val prompt: String = "",
  val negativePrompt: String = "",
  val width: Int = 512,
  val height: Int = 512,
  val steps: Int = 50,
  val guidanceScale: Float = 7.5f,
  val isGenerating: Boolean = false,
  val generatedImagePath: String? = null,
  val errorMessage: String? = null,
  /** Indicates this feature is in preview/experimental mode with simulated generation. */
  val isPreviewMode: Boolean = true,
) : NanoAIViewState

/** Error states for image generation. */
sealed interface ImageGenerationError {
  val message: String

  data class ValidationError(override val message: String) : ImageGenerationError

  data class GenerationError(override val message: String) : ImageGenerationError
}

sealed interface ImageGenerationUiEvent : NanoAIViewEvent {
  data class ErrorRaised(val error: ImageGenerationError, val envelope: NanoAIErrorEnvelope) :
    ImageGenerationUiEvent

  data class ImageGenerated(val path: String) : ImageGenerationUiEvent
}
