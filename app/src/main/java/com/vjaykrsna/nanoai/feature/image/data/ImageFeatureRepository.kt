package com.vjaykrsna.nanoai.feature.image.data

import com.vjaykrsna.nanoai.core.domain.image.ImageGalleryUseCase
import com.vjaykrsna.nanoai.core.domain.image.model.GeneratedImage
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import com.vjaykrsna.nanoai.feature.image.domain.ImageFeatureRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Data source abstraction for persisting and reading generated images. */
interface ImageFeatureDataSource {
  fun observeGallery(): Flow<List<GeneratedImage>>

  suspend fun getImage(id: UUID): NanoAIResult<GeneratedImage?>

  suspend fun saveImage(image: GeneratedImage): NanoAIResult<Unit>

  suspend fun deleteImage(id: UUID): NanoAIResult<Unit>

  suspend fun clearGallery(): NanoAIResult<Unit>
}

/** Data source implementation that reuses the shared image gallery use case. */
@Singleton
class CoreImageFeatureDataSource
@Inject
constructor(private val imageGalleryUseCase: ImageGalleryUseCase) : ImageFeatureDataSource {

  override fun observeGallery(): Flow<List<GeneratedImage>> = imageGalleryUseCase.observeAllImages()

  override suspend fun getImage(id: UUID): NanoAIResult<GeneratedImage?> =
    imageGalleryUseCase.getImageById(id)

  override suspend fun saveImage(image: GeneratedImage): NanoAIResult<Unit> =
    imageGalleryUseCase.saveImage(image)

  override suspend fun deleteImage(id: UUID): NanoAIResult<Unit> =
    imageGalleryUseCase.deleteImage(id)

  override suspend fun clearGallery(): NanoAIResult<Unit> = imageGalleryUseCase.deleteAllImages()
}

/** Repository implementation for image gallery operations. */
@Singleton
class DefaultImageFeatureRepository
@Inject
constructor(private val dataSource: ImageFeatureDataSource) : ImageFeatureRepository {

  override fun observeGallery(): Flow<List<GeneratedImage>> = dataSource.observeGallery()

  override suspend fun getImage(id: UUID): NanoAIResult<GeneratedImage?> = dataSource.getImage(id)

  override suspend fun saveImage(image: GeneratedImage): NanoAIResult<Unit> =
    dataSource.saveImage(image)

  override suspend fun deleteImage(id: UUID): NanoAIResult<Unit> = dataSource.deleteImage(id)

  override suspend fun clearGallery(): NanoAIResult<Unit> = dataSource.clearGallery()
}
