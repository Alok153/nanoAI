package com.vjaykrsna.nanoai.core.domain.image

import com.vjaykrsna.nanoai.core.domain.image.model.GeneratedImage
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/** Reactive stream contract for generated images. */
interface ImageGalleryStreamRepository {
  fun observeAllImages(): Flow<List<GeneratedImage>>
}

/** Mutation contract for gallery operations that complete once. */
interface ImageGalleryMutationRepository {
  suspend fun getImageById(id: UUID): GeneratedImage?

  suspend fun saveImage(image: GeneratedImage)

  suspend fun deleteImage(id: UUID)

  suspend fun deleteAllImages()
}

/** Repository for managing generated images with metadata. */
interface ImageGalleryRepository : ImageGalleryStreamRepository, ImageGalleryMutationRepository
