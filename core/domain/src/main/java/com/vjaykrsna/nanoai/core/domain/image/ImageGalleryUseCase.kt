package com.vjaykrsna.nanoai.core.domain.image

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.common.annotations.ReactiveStream
import com.vjaykrsna.nanoai.core.domain.image.model.GeneratedImage
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/** Use case for image gallery operations. */
@Singleton
class ImageGalleryUseCase
@Inject
constructor(private val imageGalleryRepository: ImageGalleryRepository) {
  /** Observe all images (reactive stream - not wrapped in NanoAIResult). */
  @ReactiveStream("Generated image catalog updates")
  fun observeAllImages(): Flow<List<GeneratedImage>> = imageGalleryRepository.observeAllImages()

  /** Get a specific image by ID. */
  @OneShot("Fetch a single image by identifier")
  suspend fun getImageById(id: UUID): NanoAIResult<GeneratedImage?> =
    guardGalleryOperation(
      message = "Failed to get image $id",
      context = mapOf("imageId" to id.toString()),
    ) {
      val image = imageGalleryRepository.getImageById(id)
      NanoAIResult.success(image)
    }

  /** Save an image to the gallery. */
  @OneShot("Persist a generated image to disk + DB")
  suspend fun saveImage(image: GeneratedImage): NanoAIResult<Unit> =
    guardGalleryOperation(
      message = "Failed to save image ${image.id}",
      context = mapOf("imageId" to image.id.toString(), "prompt" to image.prompt),
    ) {
      imageGalleryRepository.saveImage(image)
      NanoAIResult.success(Unit)
    }

  /** Delete a specific image from the gallery. */
  @OneShot("Delete a single generated image")
  suspend fun deleteImage(id: UUID): NanoAIResult<Unit> =
    guardGalleryOperation(
      message = "Failed to delete image $id",
      context = mapOf("imageId" to id.toString()),
    ) {
      imageGalleryRepository.deleteImage(id)
      NanoAIResult.success(Unit)
    }

  /** Delete all images from the gallery. */
  @OneShot("Delete the entire gallery")
  suspend fun deleteAllImages(): NanoAIResult<Unit> =
    guardGalleryOperation(message = "Failed to delete all images") {
      imageGalleryRepository.deleteAllImages()
      NanoAIResult.success(Unit)
    }

  private inline fun <T> guardGalleryOperation(
    message: String,
    context: Map<String, String> = emptyMap(),
    block: () -> NanoAIResult<T>,
  ): NanoAIResult<T> {
    return try {
      block()
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (ioException: IOException) {
      NanoAIResult.recoverable(message = message, cause = ioException, context = context)
    } catch (illegalStateException: IllegalStateException) {
      NanoAIResult.recoverable(message = message, cause = illegalStateException, context = context)
    } catch (illegalArgumentException: IllegalArgumentException) {
      NanoAIResult.recoverable(
        message = message,
        cause = illegalArgumentException,
        context = context,
      )
    } catch (securityException: SecurityException) {
      NanoAIResult.recoverable(message = message, cause = securityException, context = context)
    } catch (exception: Exception) {
      NanoAIResult.recoverable(message = message, cause = exception, context = context)
    }
  }
}
