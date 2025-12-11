package com.vjaykrsna.nanoai.feature.image.data

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.image.ImageGalleryRepository
import com.vjaykrsna.nanoai.core.domain.image.ImageGalleryUseCase
import com.vjaykrsna.nanoai.core.domain.image.model.GeneratedImage
import com.vjaykrsna.nanoai.core.model.NanoAISuccess
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoreImageFeatureDataSourceTest {
  private val fakeRepository = FakeImageGalleryRepository()
  private val useCase = ImageGalleryUseCase(fakeRepository)
  private val dataSource = CoreImageFeatureDataSource(useCase)

  @Test
  fun saveImage_delegatesToUseCase() = runTest {
    val image = sampleImage()

    val result = dataSource.saveImage(image)

    assertThat(fakeRepository.saved).containsExactly(image)
    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
  }

  @Test
  fun observeGallery_streamsFromUseCase() = runTest {
    val image = sampleImage()
    fakeRepository.images.value = listOf(image)

    val observed = dataSource.observeGallery().first()

    assertThat(observed).containsExactly(image)
  }

  @Test
  fun clearGallery_delegatesToUseCase() = runTest {
    val result = dataSource.clearGallery()

    assertThat(fakeRepository.cleared).isTrue()
    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
  }

  private fun sampleImage(): GeneratedImage =
    GeneratedImage(
      id = UUID.randomUUID(),
      prompt = "prompt",
      negativePrompt = "",
      width = 512,
      height = 512,
      steps = 20,
      guidanceScale = 7.5f,
      filePath = "file://image.png",
      thumbnailPath = null,
      createdAt = Clock.System.now(),
    )

  private class FakeImageGalleryRepository : ImageGalleryRepository {
    val images = MutableStateFlow<List<GeneratedImage>>(emptyList())
    val saved = mutableListOf<GeneratedImage>()
    val deleted = mutableListOf<UUID>()
    var cleared: Boolean = false

    override fun observeAllImages() = images

    override suspend fun getImageById(id: UUID): GeneratedImage? =
      images.value.firstOrNull { it.id == id }

    override suspend fun saveImage(image: GeneratedImage) {
      saved += image
      images.value = images.value + image
    }

    override suspend fun deleteImage(id: UUID) {
      deleted += id
      images.value = images.value.filterNot { it.id == id }
    }

    override suspend fun deleteAllImages() {
      cleared = true
      images.value = emptyList()
    }
  }
}
