package com.vjaykrsna.nanoai.feature.image.data

import com.vjaykrsna.nanoai.feature.image.data.db.GeneratedImageDao
import com.vjaykrsna.nanoai.feature.image.data.db.toDomain
import com.vjaykrsna.nanoai.feature.image.data.db.toEntity
import com.vjaykrsna.nanoai.feature.image.domain.model.GeneratedImage
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Repository for managing generated images with metadata. */
interface ImageGalleryRepository {
  fun observeAllImages(): Flow<List<GeneratedImage>>

  suspend fun getImageById(id: UUID): GeneratedImage?

  suspend fun saveImage(image: GeneratedImage)

  suspend fun deleteImage(id: UUID)

  suspend fun deleteAllImages()
}

@Singleton
class ImageGalleryRepositoryImpl
@Inject
constructor(private val generatedImageDao: GeneratedImageDao) : ImageGalleryRepository {

  override fun observeAllImages(): Flow<List<GeneratedImage>> =
    generatedImageDao.observeAll().map { entities -> entities.map { it.toDomain() } }

  override suspend fun getImageById(id: UUID): GeneratedImage? =
    generatedImageDao.getById(id.toString())?.toDomain()

  override suspend fun saveImage(image: GeneratedImage) {
    generatedImageDao.insert(image.toEntity())
  }

  override suspend fun deleteImage(id: UUID) {
    generatedImageDao.deleteById(id.toString())
  }

  override suspend fun deleteAllImages() {
    generatedImageDao.deleteAll()
  }
}
