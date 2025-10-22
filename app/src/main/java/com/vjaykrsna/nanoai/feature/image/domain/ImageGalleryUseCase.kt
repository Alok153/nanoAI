package com.vjaykrsna.nanoai.feature.image.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.feature.image.data.ImageGalleryRepository
import com.vjaykrsna.nanoai.feature.image.domain.model.GeneratedImage
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Use case for image gallery operations. */
@Singleton
class ImageGalleryUseCase
@Inject
constructor(private val imageGalleryRepository: ImageGalleryRepository) {
  /** Observe all images (reactive stream - not wrapped in NanoAIResult). */
  fun observeAllImages(): Flow<List<GeneratedImage>> {
    return imageGalleryRepository.observeAllImages()
  }

  /** Get a specific image by ID. */
  suspend fun getImageById(id: UUID): NanoAIResult<GeneratedImage?> {
    return try {
      val image = imageGalleryRepository.getImageById(id)
      NanoAIResult.success(image)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to get image $id",
        cause = e,
        context = mapOf("imageId" to id.toString()),
      )
    }
  }

  /** Save an image to the gallery. */
  suspend fun saveImage(image: GeneratedImage): NanoAIResult<Unit> {
    return try {
      imageGalleryRepository.saveImage(image)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to save image ${image.id}",
        cause = e,
        context = mapOf("imageId" to image.id.toString(), "prompt" to image.prompt),
      )
    }
  }

  /** Delete a specific image from the gallery. */
  suspend fun deleteImage(id: UUID): NanoAIResult<Unit> {
    return try {
      imageGalleryRepository.deleteImage(id)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to delete image $id",
        cause = e,
        context = mapOf("imageId" to id.toString()),
      )
    }
  }

  /** Delete all images from the gallery. */
  suspend fun deleteAllImages(): NanoAIResult<Unit> {
    return try {
      imageGalleryRepository.deleteAllImages()
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(message = "Failed to delete all images", cause = e)
    }
  }
}
