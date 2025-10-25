package com.vjaykrsna.nanoai.feature.library.data.huggingface

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.network.ConnectivityStatusProvider
import com.vjaykrsna.nanoai.feature.library.data.huggingface.network.HuggingFaceService
import com.vjaykrsna.nanoai.feature.library.data.huggingface.network.dto.HuggingFaceModelListingDto
import com.vjaykrsna.nanoai.feature.library.data.huggingface.network.dto.ModelCardDataDto
import com.vjaykrsna.nanoai.feature.library.data.huggingface.network.dto.ModelConfigDto
import com.vjaykrsna.nanoai.feature.library.data.huggingface.network.dto.ModelSiblingDto
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceCatalogQuery
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceModelSummary
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HuggingFaceCatalogRepositoryImplTest {

  private lateinit var service: HuggingFaceService
  private lateinit var cacheDataSource: HuggingFaceModelCacheDataSource
  private lateinit var repository: HuggingFaceCatalogRepositoryImpl
  private lateinit var connectivityStatusProvider: ConnectivityStatusProvider
  private lateinit var fixedClock: Clock

  @Before
  fun setup() {
    service = mockk()
    cacheDataSource = mockk(relaxed = true)
    connectivityStatusProvider = mockk()
    fixedClock = mockk()
    repository =
      HuggingFaceCatalogRepositoryImpl(
        service,
        cacheDataSource,
        connectivityStatusProvider,
        fixedClock,
      )

    // Mock online connectivity for most tests
    coEvery { connectivityStatusProvider.isOnline() } returns true
  }

  @Test
  fun `listModels returns cached models when available and fresh`() = runTest {
    val query = HuggingFaceCatalogQuery(limit = 10, offset = 0)
    val cachedModels = listOf(createTestModel("cached-model"))
    val expiryTime = Instant.parse("2024-01-01T00:00:00Z")

    coEvery { fixedClock.now() } returns Instant.parse("2024-01-01T05:00:00Z") // 5 hours later
    coEvery { cacheDataSource.getFreshModels(10, 0, any()) } returns cachedModels

    val result = repository.listModels(query)

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    assertThat((result as NanoAIResult.Success).value).isEqualTo(cachedModels)
  }

  @Test
  fun `listModels fetches from network when cache is empty`() = runTest {
    val query = HuggingFaceCatalogQuery(limit = 10, offset = 0)
    val networkModels = listOf(createTestDto("network-model"))
    val expiryTime = Instant.parse("2024-01-01T00:00:00Z")

    coEvery { fixedClock.now() } returns
      Instant.parse("2024-01-01T07:00:00Z") // 7 hours later (past TTL)
    coEvery { cacheDataSource.getFreshModels(10, 0, expiryTime) } returns emptyList()
    coEvery {
      service.listModels(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
      )
    } returns networkModels
    coEvery { cacheDataSource.storeModels(any()) } returns Unit
    coEvery { cacheDataSource.pruneOlderThan(any()) } returns Unit

    val result = repository.listModels(query)

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    val models = (result as NanoAIResult.Success).value
    assertThat(models).hasSize(1)
    assertThat(models[0].modelId).isEqualTo("network-model")
  }

  @Test
  fun `listModels correctly parses rich metadata from network response`() = runTest {
    val networkDto =
      HuggingFaceModelListingDto(
        modelId = "test-model",
        author = "test-author",
        pipelineTag = "text-generation",
        libraryName = "transformers",
        tags = listOf("tag1", "tag2"),
        likes = 100,
        downloads = 1000,
        gated = JsonPrimitive(false),
        disabled = false,
        cardData =
          ModelCardDataDto(
            license = "apache-2.0",
            languages = JsonArray(listOf(JsonPrimitive("en"), JsonPrimitive("es"))),
            baseModel = JsonPrimitive("base-model-123"),
            datasets = JsonArray(listOf(JsonPrimitive("dataset1"), JsonPrimitive("dataset2"))),
            summary = "A test model summary",
            description = "A detailed description of the test model",
          ),
        config = ModelConfigDto(architectures = listOf("Transformer", "BERT"), modelType = "bert"),
        siblings =
          listOf(
            ModelSiblingDto(filename = "model.bin", sizeBytes = 1000000L),
            ModelSiblingDto(filename = "config.json", sizeBytes = 2000L),
          ),
        trendingScore = 50,
        createdAt = "2024-01-01T00:00:00Z",
        lastModified = "2024-01-02T00:00:00Z",
        isPrivate = false,
      )

    val query = HuggingFaceCatalogQuery(limit = 10, offset = 0)
    val expiryTime = Instant.parse("2024-01-01T00:00:00Z")

    coEvery { fixedClock.now() } returns Instant.parse("2024-01-01T07:00:00Z") // Past TTL
    coEvery { cacheDataSource.getFreshModels(10, 0, any()) } returns emptyList()
    coEvery {
      service.listModels(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
      )
    } returns listOf(networkDto)
    coEvery { cacheDataSource.storeModels(any()) } returns Unit
    coEvery { cacheDataSource.pruneOlderThan(any()) } returns Unit

    val result = repository.listModels(query)

    assertTrue(result is NanoAIResult.Success)
    val models = (result as NanoAIResult.Success).value
    assertEquals(1, models.size)
    val model = models[0]

    assertEquals("test-model", model.modelId)
    assertEquals("test-author", model.author)
    assertEquals("text-generation", model.pipelineTag)
    assertEquals("transformers", model.libraryName)
    assertEquals(listOf("tag1", "tag2"), model.tags)
    assertEquals(100L, model.likes)
    assertEquals(1000L, model.downloads)
    assertEquals("apache-2.0", model.license)
    assertEquals(listOf("en", "es"), model.languages)
    assertEquals("base-model-123", model.baseModel)
    assertEquals(listOf("dataset1", "dataset2"), model.datasets)
    assertEquals(listOf("Transformer", "BERT"), model.architectures)
    assertEquals("bert", model.modelType)
    assertFalse(model.hasGatedAccess)
    assertFalse(model.isDisabled)
    assertEquals(1002000L, model.totalSizeBytes) // Sum of siblings
    assertEquals("A test model summary", model.summary)
    assertEquals("A detailed description of the test model", model.description)
    assertEquals(50L, model.trendingScore)
    assertEquals(Instant.parse("2024-01-01T00:00:00Z"), model.createdAt)
    assertEquals(Instant.parse("2024-01-02T00:00:00Z"), model.lastModified)
    assertFalse(model.isPrivate)
  }

  private fun createTestModel(modelId: String) =
    HuggingFaceModelSummary(
      modelId = modelId,
      displayName = modelId,
      author = null,
      pipelineTag = null,
      libraryName = null,
      tags = emptyList(),
      likes = 0,
      downloads = 0,
      trendingScore = null,
      createdAt = null,
      lastModified = null,
      isPrivate = false,
    )

  @Test
  fun `listModels skips cache when search query is present`() = runTest {
    val queryWithSearch = HuggingFaceCatalogQuery(search = "bert", limit = 10, offset = 0)
    val cachedModels = listOf(createTestModel("cached-model"))
    val networkModels = listOf(createTestDto("network-model"))
    val expiryTime = Instant.parse("2024-01-01T00:00:00Z")

    coEvery { fixedClock.now() } returns Instant.parse("2024-01-01T05:00:00Z") // 5 hours later
    coEvery { cacheDataSource.getFreshModels(10, 0, expiryTime) } returns cachedModels
    coEvery {
      service.listModels(
        search = "bert",
        sort = any(),
        direction = any(),
        limit = any(),
        skip = any(),
        pipelineTag = any(),
        library = any(),
        includePrivate = any(),
        expandAuthor = any(),
        expandDownloads = any(),
        expandLikes = any(),
        expandPipelineTag = any(),
        expandTags = any(),
        expandLibraryName = any(),
        expandCreatedAt = any(),
        expandLastModified = any(),
        expandTrendingScore = any(),
        expandPrivate = any(),
        expandGated = any(),
        expandDisabled = any(),
        expandCardData = any(),
        expandConfig = any(),
        expandBaseModels = any(),
        expandSiblings = any(),
      )
    } returns networkModels
    coEvery { cacheDataSource.storeModels(any()) } returns Unit
    coEvery { cacheDataSource.pruneOlderThan(any()) } returns Unit

    val result = repository.listModels(queryWithSearch)

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    val models = (result as NanoAIResult.Success).value
    assertThat(models).hasSize(1)
    assertThat(models[0].modelId)
      .isEqualTo("network-model") // Should return network results, not cached
  }

  @Test
  fun `listModels skips cache when pipeline filter is present`() = runTest {
    val queryWithPipeline =
      HuggingFaceCatalogQuery(pipelineTag = "text-generation", limit = 10, offset = 0)
    val cachedModels = listOf(createTestModel("cached-model"))
    val networkModels = listOf(createTestDto("network-model"))
    val expiryTime = Instant.parse("2024-01-01T00:00:00Z")

    coEvery { fixedClock.now() } returns Instant.parse("2024-01-01T05:00:00Z") // 5 hours later
    coEvery { cacheDataSource.getFreshModels(10, 0, expiryTime) } returns cachedModels
    coEvery {
      service.listModels(
        search = any(),
        sort = any(),
        direction = any(),
        limit = any(),
        skip = any(),
        pipelineTag = "text-generation",
        library = any(),
        includePrivate = any(),
        expandAuthor = any(),
        expandDownloads = any(),
        expandLikes = any(),
        expandPipelineTag = any(),
        expandTags = any(),
        expandLibraryName = any(),
        expandCreatedAt = any(),
        expandLastModified = any(),
        expandTrendingScore = any(),
        expandPrivate = any(),
        expandGated = any(),
        expandDisabled = any(),
        expandCardData = any(),
        expandConfig = any(),
        expandBaseModels = any(),
        expandSiblings = any(),
      )
    } returns networkModels
    coEvery { cacheDataSource.storeModels(any()) } returns Unit
    coEvery { cacheDataSource.pruneOlderThan(any()) } returns Unit

    val result = repository.listModels(queryWithPipeline)

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    val models = (result as NanoAIResult.Success).value
    assertThat(models).hasSize(1)
    assertThat(models[0].modelId)
      .isEqualTo("network-model") // Should return network results, not cached
  }

  private fun createTestDto(modelId: String) =
    HuggingFaceModelListingDto(modelId = modelId, likes = 0, downloads = 0, isPrivate = false)
}
