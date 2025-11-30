package com.vjaykrsna.nanoai.core.domain.image

import com.vjaykrsna.nanoai.core.domain.image.model.GeneratedImage
import com.vjaykrsna.nanoai.testing.assertRecoverableError
import com.vjaykrsna.nanoai.testing.assertSuccess
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ImageGalleryUseCaseTest {
  private lateinit var useCase: ImageGalleryUseCase
  private lateinit var imageGalleryRepository: ImageGalleryRepository

  @BeforeEach
  fun setup() {
    imageGalleryRepository = mockk(relaxed = true)

    useCase = ImageGalleryUseCase(imageGalleryRepository)
  }

  @Test
  fun `observeAllImages returns flow from repository`() = runTest {
    val images =
      listOf(
        GeneratedImage(
          id = UUID.randomUUID(),
          prompt = "A beautiful sunset",
          negativePrompt = "dark, gloomy",
          width = 512,
          height = 512,
          steps = 20,
          guidanceScale = 7.5f,
          filePath = "/images/sunset.jpg",
          thumbnailPath = "/thumbnails/sunset_thumb.jpg",
          createdAt = Instant.parse("2024-01-01T12:00:00Z"),
        )
      )
    val flow = flowOf(images)
    every { imageGalleryRepository.observeAllImages() } returns flow

    val result = useCase.observeAllImages()

    assert(result == flow)
  }

  @Test
  fun `getImageById returns success with image when found`() = runTest {
    val imageId = UUID.randomUUID()
    val image =
      GeneratedImage(
        id = imageId,
        prompt = "A beautiful sunset",
        negativePrompt = "dark, gloomy",
        width = 512,
        height = 512,
        steps = 20,
        guidanceScale = 7.5f,
        filePath = "/images/sunset.jpg",
        createdAt = Instant.parse("2024-01-01T12:00:00Z"),
      )
    coEvery { imageGalleryRepository.getImageById(imageId) } returns image

    val result = useCase.getImageById(imageId)

    val returnedImage = result.assertSuccess()
    assert(returnedImage == image)
  }

  @Test
  fun `getImageById returns success with null when not found`() = runTest {
    val imageId = UUID.randomUUID()
    coEvery { imageGalleryRepository.getImageById(imageId) } returns null

    val result = useCase.getImageById(imageId)

    val returnedImage = result.assertSuccess()
    assert(returnedImage == null)
  }

  @Test
  fun `getImageById returns recoverable error when repository fails`() = runTest {
    val imageId = UUID.randomUUID()
    val exception = IllegalStateException("Database error")
    coEvery { imageGalleryRepository.getImageById(imageId) } throws exception

    val result = useCase.getImageById(imageId)

    result.assertRecoverableError()
  }

  @Test
  fun `getImageById rethrows cancellation`() = runTest {
    val imageId = UUID.randomUUID()
    coEvery { imageGalleryRepository.getImageById(imageId) } throws CancellationException("cancel")

    assertFailsWith<CancellationException> { useCase.getImageById(imageId) }
  }

  @Test
  fun `saveImage returns success when repository succeeds`() = runTest {
    val image =
      GeneratedImage(
        id = UUID.randomUUID(),
        prompt = "A beautiful sunset",
        negativePrompt = "dark, gloomy",
        width = 512,
        height = 512,
        steps = 20,
        guidanceScale = 7.5f,
        filePath = "/images/sunset.jpg",
        createdAt = Instant.parse("2024-01-01T12:00:00Z"),
      )
    coEvery { imageGalleryRepository.saveImage(image) } returns Unit

    val result = useCase.saveImage(image)

    result.assertSuccess()
  }

  @Test
  fun `saveImage returns recoverable error when repository fails`() = runTest {
    val image =
      GeneratedImage(
        id = UUID.randomUUID(),
        prompt = "A beautiful sunset",
        negativePrompt = "dark, gloomy",
        width = 512,
        height = 512,
        steps = 20,
        guidanceScale = 7.5f,
        filePath = "/images/sunset.jpg",
        createdAt = Instant.parse("2024-01-01T12:00:00Z"),
      )
    val exception = IOException("Storage error")
    coEvery { imageGalleryRepository.saveImage(image) } throws exception

    val result = useCase.saveImage(image)

    result.assertRecoverableError()
  }

  @Test
  fun `deleteImage returns success when repository succeeds`() = runTest {
    val imageId = UUID.randomUUID()
    coEvery { imageGalleryRepository.deleteImage(imageId) } returns Unit

    val result = useCase.deleteImage(imageId)

    result.assertSuccess()
  }

  @Test
  fun `deleteImage returns recoverable error when repository fails`() = runTest {
    val imageId = UUID.randomUUID()
    val exception = IllegalArgumentException("Database error")
    coEvery { imageGalleryRepository.deleteImage(imageId) } throws exception

    val result = useCase.deleteImage(imageId)

    result.assertRecoverableError()
  }

  @Test
  fun `deleteAllImages returns success when repository succeeds`() = runTest {
    coEvery { imageGalleryRepository.deleteAllImages() } returns Unit

    val result = useCase.deleteAllImages()

    result.assertSuccess()
  }

  @Test
  fun `deleteAllImages returns recoverable error when repository fails`() = runTest {
    val exception = IllegalStateException("Database error")
    coEvery { imageGalleryRepository.deleteAllImages() } throws exception

    val result = useCase.deleteAllImages()

    result.assertRecoverableError()
  }
}
