package com.vjaykrsna.nanoai.feature.image.domain

import com.vjaykrsna.nanoai.core.domain.image.model.GeneratedImage
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Feature-level repository for generated image management. */
interface ImageFeatureRepository {
  fun observeGallery(): Flow<List<GeneratedImage>>

  suspend fun getImage(id: UUID): NanoAIResult<GeneratedImage?>

  suspend fun saveImage(image: GeneratedImage): NanoAIResult<Unit>

  suspend fun deleteImage(id: UUID): NanoAIResult<Unit>

  suspend fun clearGallery(): NanoAIResult<Unit>
}

/** Use case wrapper that enforces ViewModel → UseCase → Repository boundaries. */
class ImageGalleryFeatureUseCase
@Inject
constructor(private val repository: ImageFeatureRepository) {

  fun observeGallery(): Flow<List<GeneratedImage>> = repository.observeGallery()

  suspend fun getImage(id: UUID): NanoAIResult<GeneratedImage?> = repository.getImage(id)

  suspend fun saveImage(image: GeneratedImage): NanoAIResult<Unit> = repository.saveImage(image)

  suspend fun deleteImage(id: UUID): NanoAIResult<Unit> = repository.deleteImage(id)

  suspend fun clearGallery(): NanoAIResult<Unit> = repository.clearGallery()
}
