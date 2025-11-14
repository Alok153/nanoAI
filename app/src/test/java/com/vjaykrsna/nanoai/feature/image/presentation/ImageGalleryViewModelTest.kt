package com.vjaykrsna.nanoai.feature.image.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.image.ImageGalleryUseCase
import com.vjaykrsna.nanoai.core.domain.image.model.GeneratedImage
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ImageGalleryViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  private lateinit var imageGalleryUseCase: ImageGalleryUseCase
  private lateinit var viewModel: ImageGalleryViewModel
  private lateinit var imagesFlow: MutableStateFlow<List<GeneratedImage>>

  @BeforeEach
  fun setup() {
    imageGalleryUseCase = mockk(relaxed = true)
    imagesFlow = MutableStateFlow(emptyList())
    every { imageGalleryUseCase.observeAllImages() } returns imagesFlow
    viewModel = ImageGalleryViewModel(imageGalleryUseCase, dispatcher)
  }

  @Test
  fun deleteImage_emitsSuccessEvent_whenRepositoryCompletes() =
    runTest(dispatcher) {
      coEvery { imageGalleryUseCase.deleteImage(any()) } returns NanoAIResult.success(Unit)
      val eventDeferred = async { viewModel.events.first() }

      viewModel.deleteImage(UUID.randomUUID())
      advanceUntilIdle()

      assertThat(eventDeferred.await()).isEqualTo(ImageGalleryUiEvent.ImageDeleted)
    }

  @Test
  fun deleteImage_emitsErrorEvent_whenRepositoryFails() =
    runTest(dispatcher) {
      coEvery { imageGalleryUseCase.deleteImage(any()) } returns
        NanoAIResult.recoverable("Failed to delete image", cause = IllegalStateException("db down"))
      val targetId = UUID.randomUUID()
      val eventDeferred = async { viewModel.events.first() }

      viewModel.deleteImage(targetId)
      advanceUntilIdle()

      val event = eventDeferred.await() as ImageGalleryUiEvent.ErrorRaised
      assertThat(event.envelope.userMessage).contains("Failed to delete image")
      assertThat(viewModel.state.value.errorMessage).contains("Failed to delete image")
    }

  @Test
  fun deleteAllImages_emitsErrorEvent_whenRepositoryFails() =
    runTest(dispatcher) {
      coEvery { imageGalleryUseCase.deleteAllImages() } returns
        NanoAIResult.recoverable(
          "Failed to delete all images",
          cause = IllegalStateException("db down"),
        )
      val eventDeferred = async { viewModel.events.first() }

      viewModel.deleteAllImages()
      advanceUntilIdle()

      val event = eventDeferred.await() as ImageGalleryUiEvent.ErrorRaised
      assertThat(event.envelope.userMessage).contains("Failed to delete all images")
    }

  @Test
  fun deleteAllImages_emitsSuccessEvent_whenRepositoryCompletes() =
    runTest(dispatcher) {
      coEvery { imageGalleryUseCase.deleteAllImages() } returns NanoAIResult.success(Unit)
      val eventDeferred = async { viewModel.events.first() }

      viewModel.deleteAllImages()
      advanceUntilIdle()

      assertThat(eventDeferred.await()).isEqualTo(ImageGalleryUiEvent.AllImagesDeleted)
    }
}
