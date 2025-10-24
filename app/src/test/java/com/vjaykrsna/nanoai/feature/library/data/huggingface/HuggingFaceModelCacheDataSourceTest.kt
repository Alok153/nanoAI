package com.vjaykrsna.nanoai.feature.library.data.huggingface

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.library.data.huggingface.dao.HuggingFaceModelCacheDao
import com.vjaykrsna.nanoai.feature.library.data.huggingface.entities.HuggingFaceModelCacheEntity
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceModelSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test

class HuggingFaceModelCacheDataSourceTest {

  private lateinit var dao: HuggingFaceModelCacheDao
  private lateinit var dataSource: HuggingFaceModelCacheDataSource
  private lateinit var fixedClock: Clock

  @Before
  fun setup() {
    dao = mockk(relaxed = true)
    fixedClock = mockk()
    dataSource = HuggingFaceModelCacheDataSource(dao, fixedClock)
  }

  @Test
  fun `getFreshModels calls DAO with correct parameters`() = runTest {
    val limit = 10
    val offset = 5
    val ttl = Instant.parse("2024-01-01T00:00:00Z")
    val expectedEntities = emptyList<HuggingFaceModelCacheEntity>()

    coEvery { dao.getFreshModels(ttl, limit, offset) } returns expectedEntities

    val result = dataSource.getFreshModels(limit, offset, ttl)

    assertThat(result).isEmpty()
    coVerify { dao.getFreshModels(ttl, limit, offset) }
  }

  @Test
  fun `storeModels converts domain models to entities and stores them`() = runTest {
    val now = Instant.parse("2024-01-01T12:00:00Z")
    val models =
      listOf(
        HuggingFaceModelSummary(
          modelId = "test-model",
          displayName = "Test Model",
          author = "test-author",
          pipelineTag = "text-generation",
          libraryName = "transformers",
          tags = listOf("tag1", "tag2"),
          likes = 100,
          downloads = 1000,
          trendingScore = 50,
          createdAt = Instant.parse("2024-01-01T00:00:00Z"),
          lastModified = Instant.parse("2024-01-02T00:00:00Z"),
          isPrivate = false,
        )
      )

    coEvery { fixedClock.now() } returns now

    dataSource.storeModels(models)

    coVerify {
      dao.upsertAll(
        match { entities ->
          entities.size == 1 &&
            entities[0].modelId == "test-model" &&
            entities[0].displayName == "Test Model" &&
            entities[0].author == "test-author" &&
            entities[0].license == "apache-2.0" &&
            entities[0].languages == listOf("en", "es") &&
            entities[0].totalSizeBytes == 1000000L &&
            entities[0].fetchedAt == now
        }
      )
    }
  }

  @Test
  fun `storeModels does nothing for empty list`() = runTest {
    dataSource.storeModels(emptyList())

    coVerify(exactly = 0) { dao.upsertAll(any()) }
  }

  @Test
  fun `replaceAll clears existing data and stores new models`() = runTest {
    val models =
      listOf(
        HuggingFaceModelSummary(
          modelId = "new-model",
          displayName = "New Model",
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
      )

    val now = Instant.parse("2024-01-01T12:00:00Z")
    coEvery { fixedClock.now() } returns now

    dataSource.replaceAll(models)

    coVerify {
      dao.clear()
      dao.upsertAll(
        match { entities ->
          entities.size == 1 && entities[0].modelId == "new-model" && entities[0].fetchedAt == now
        }
      )
    }
  }

  @Test
  fun `pruneOlderThan delegates to DAO`() = runTest {
    val ttl = Instant.parse("2024-01-01T00:00:00Z")

    dataSource.pruneOlderThan(ttl)

    coVerify { dao.deleteOlderThan(ttl) }
  }
}
