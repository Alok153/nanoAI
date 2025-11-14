package com.vjaykrsna.nanoai.feature.image.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.image.ImageGalleryUseCase
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.mockk
import java.util.UUID
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
class ImageGalleryViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  private lateinit var imageGalleryUseCase: ImageGalleryUseCase
  private lateinit var viewModel: ImageGalleryViewModel

  @BeforeEach
  fun setup() {
    imageGalleryUseCase = mockk(relaxed = true)
    viewModel = ImageGalleryViewModel(imageGalleryUseCase)
  }

  @Test
  fun deleteImage_emitsSuccessEvent_whenRepositoryCompletes() =
    runTest(dispatcher) {
      coEvery { imageGalleryUseCase.deleteImage(any()) } returns NanoAIResult.success(Unit)
      val eventDeferred = async { viewModel.events.first() }

      viewModel.deleteImage(UUID.randomUUID())
      advanceUntilIdle()

      assertThat(eventDeferred.await()).isEqualTo(ImageGalleryEvent.ImageDeleted)
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

      val event = eventDeferred.await()
      assertThat(event).isInstanceOf(ImageGalleryEvent.Error::class.java)
      val error = event as ImageGalleryEvent.Error
      assertThat(error.message).contains("Failed to delete image")
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

      val event = eventDeferred.await()
      assertThat(event).isInstanceOf(ImageGalleryEvent.Error::class.java)
      val error = event as ImageGalleryEvent.Error
      assertThat(error.message).contains("Failed to delete all images")
    }

  @Test
  fun deleteAllImages_emitsSuccessEvent_whenRepositoryCompletes() =
    runTest(dispatcher) {
      coEvery { imageGalleryUseCase.deleteAllImages() } returns NanoAIResult.success(Unit)
      val eventDeferred = async { viewModel.events.first() }

      viewModel.deleteAllImages()
      advanceUntilIdle()

      assertThat(eventDeferred.await()).isEqualTo(ImageGalleryEvent.AllImagesDeleted)
    }
}
