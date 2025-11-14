package com.vjaykrsna.nanoai.feature.image.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.image.ImageGalleryUseCase
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ImageGenerationViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  private lateinit var imageGalleryUseCase: ImageGalleryUseCase
  private lateinit var viewModel: ImageGenerationViewModel

  @BeforeEach
  fun setup() {
    imageGalleryUseCase = mockk(relaxed = true)
    viewModel = ImageGenerationViewModel(imageGalleryUseCase, dispatcher)
  }

  @Test
  fun generateImage_emitsErrorEvent_whenSaveFails() =
    runTest(dispatcher) {
      coEvery { imageGalleryUseCase.saveImage(any()) } returns
        NanoAIResult.recoverable("Failed to save image", cause = IllegalStateException("disk full"))
      viewModel.updatePrompt("A photorealistic fox")
      val eventDeferred = async { viewModel.events.first() }

      viewModel.generateImage()
      advanceUntilIdle()

      val event = eventDeferred.await() as ImageGenerationUiEvent.ErrorRaised
      assertThat(event.error).isInstanceOf(ImageGenerationError.GenerationError::class.java)
      assertThat(event.envelope.userMessage).contains("Failed to save image")
      val uiState = viewModel.state.value
      assertThat(uiState.isGenerating).isFalse()
      assertThat(uiState.generatedImagePath).isNull()
      assertThat(uiState.errorMessage).contains("Failed to save image")
      coVerify { imageGalleryUseCase.saveImage(any()) }
    }

  @Test
  fun generateImage_setsPlaceholderPath_whenSaveSucceeds() =
    runTest(dispatcher) {
      coEvery { imageGalleryUseCase.saveImage(any()) } returns NanoAIResult.success(Unit)
      viewModel.updatePrompt("Neon skyline")

      viewModel.generateImage()
      advanceUntilIdle()

      val uiState = viewModel.state.value
      assertThat(uiState.isGenerating).isFalse()
      assertThat(uiState.generatedImagePath).isNotNull()
      assertThat(uiState.errorMessage).isEqualTo("Image generation not yet implemented")
      coVerify { imageGalleryUseCase.saveImage(any()) }
    }
}
