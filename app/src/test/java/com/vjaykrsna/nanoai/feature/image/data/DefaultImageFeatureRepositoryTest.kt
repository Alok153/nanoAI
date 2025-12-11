package com.vjaykrsna.nanoai.feature.image.data

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.image.model.GeneratedImage
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import com.vjaykrsna.nanoai.core.model.NanoAISuccess
import com.vjaykrsna.nanoai.feature.image.domain.ImageGalleryFeatureUseCase
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultImageFeatureRepositoryTest {
  private val fakeDataSource = FakeImageFeatureDataSource()
  private val repository = DefaultImageFeatureRepository(fakeDataSource)
  private val useCase = ImageGalleryFeatureUseCase(repository)

  @Test
  fun saveImage_delegatesToDataSource() = runTest {
    val image = sampleImage()

    val result = repository.saveImage(image)

    assertThat(fakeDataSource.savedImages).containsExactly(image)
    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
  }

  @Test
  fun observeGallery_emitsFromDataSource() = runTest {
    val image = sampleImage()
    fakeDataSource.images.value = listOf(image)

    val observed = repository.observeGallery().first()

    assertThat(observed).containsExactly(image)
  }

  @Test
  fun useCase_reroutesDeleteCall() = runTest {
    val targetId = UUID.randomUUID()

    val result = useCase.deleteImage(targetId)

    assertThat(fakeDataSource.deletedIds).containsExactly(targetId)
    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
  }

  private fun sampleImage(): GeneratedImage =
    GeneratedImage(
      id = UUID.randomUUID(),
      prompt = "test",
      negativePrompt = "",
      width = 512,
      height = 512,
      steps = 1,
      guidanceScale = 1.0f,
      filePath = "path/to/file",
      thumbnailPath = null,
      createdAt = Instant.DISTANT_PAST,
    )
}

private class FakeImageFeatureDataSource : ImageFeatureDataSource {
  val images = MutableStateFlow<List<GeneratedImage>>(emptyList())
  val savedImages = mutableListOf<GeneratedImage>()
  val deletedIds = mutableListOf<UUID>()

  override fun observeGallery() = images

  override suspend fun getImage(id: UUID): NanoAIResult<GeneratedImage?> =
    NanoAIResult.success(images.value.firstOrNull { it.id == id })

  override suspend fun saveImage(image: GeneratedImage): NanoAIResult<Unit> {
    savedImages += image
    images.value = images.value + image
    return NanoAIResult.success(Unit)
  }

  override suspend fun deleteImage(id: UUID): NanoAIResult<Unit> {
    deletedIds += id
    images.value = images.value.filterNot { it.id == id }
    return NanoAIResult.success(Unit)
  }

  override suspend fun clearGallery(): NanoAIResult<Unit> {
    images.value = emptyList()
    return NanoAIResult.success(Unit)
  }
}
