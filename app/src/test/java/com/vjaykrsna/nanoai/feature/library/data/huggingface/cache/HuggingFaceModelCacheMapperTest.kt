package com.vjaykrsna.nanoai.feature.library.data.huggingface.cache

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.library.data.huggingface.entities.HuggingFaceModelCacheEntity
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceModelSummary
import kotlinx.datetime.Instant
import org.junit.Test

class HuggingFaceModelCacheMapperTest {

  @Test
  fun `toEntity maps all fields correctly`() {
    val now = Instant.parse("2024-01-01T12:00:00Z")
    val domain =
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

    val entity = HuggingFaceModelCacheMapper.toEntity(domain, now)

    assertThat(entity.modelId).isEqualTo("test-model")
    assertThat(entity.displayName).isEqualTo("Test Model")
    assertThat(entity.author).isEqualTo("test-author")
    assertThat(entity.pipelineTag).isEqualTo("text-generation")
    assertThat(entity.libraryName).isEqualTo("transformers")
    assertThat(entity.tags).containsExactly("tag1", "tag2")
    assertThat(entity.likes).isEqualTo(100)
    assertThat(entity.downloads).isEqualTo(1000)
    assertThat(entity.license).isEqualTo("apache-2.0")
    assertThat(entity.languages).containsExactly("en", "es")
    assertThat(entity.baseModel).isEqualTo("base-model")
    assertThat(entity.datasets).containsExactly("dataset1")
    assertThat(entity.architectures).containsExactly("Transformer")
    assertThat(entity.modelType).isEqualTo("bert")
    assertThat(entity.baseModelRelations).containsExactly("relation1")
    assertThat(entity.hasGatedAccess).isFalse()
    assertThat(entity.isDisabled).isFalse()
    assertThat(entity.totalSizeBytes).isEqualTo(1000000L)
    assertThat(entity.summary).isEqualTo("A summary")
    assertThat(entity.description).isEqualTo("A description")
    assertThat(entity.trendingScore).isEqualTo(50)
    assertThat(entity.createdAt).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"))
    assertThat(entity.lastModified).isEqualTo(Instant.parse("2024-01-02T00:00:00Z"))
    assertThat(entity.isPrivate).isFalse()
    assertThat(entity.fetchedAt).isEqualTo(now)
  }

  @Test
  fun `toEntity uses current time when fetchedAt not provided`() {
    val fixedTime = Instant.parse("2024-01-01T12:00:00Z")
    val domain =
      HuggingFaceModelSummary(
        modelId = "test-model",
        displayName = "Test Model",
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

    val entity = HuggingFaceModelCacheMapper.toEntity(domain)

    assertThat(entity.fetchedAt).isAtLeast(fixedTime)
  }

  @Test
  fun `toDomain maps all fields correctly`() {
    val entity =
      HuggingFaceModelCacheEntity(
        modelId = "test-model",
        displayName = "Test Model",
        author = "test-author",
        pipelineTag = "text-generation",
        libraryName = "transformers",
        tags = listOf("tag1", "tag2"),
        likes = 100,
        downloads = 1000,
        license = "apache-2.0",
        languages = listOf("en", "es"),
        baseModel = "base-model",
        datasets = listOf("dataset1"),
        architectures = listOf("Transformer"),
        modelType = "bert",
        baseModelRelations = listOf("relation1"),
        hasGatedAccess = false,
        isDisabled = false,
        totalSizeBytes = 1000000L,
        summary = "A summary",
        description = "A description",
        trendingScore = 50,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        lastModified = Instant.parse("2024-01-02T00:00:00Z"),
        isPrivate = false,
        fetchedAt = Instant.parse("2024-01-01T12:00:00Z"),
      )

    val domain = HuggingFaceModelCacheMapper.toDomain(entity)

    assertThat(domain.modelId).isEqualTo("test-model")
    assertThat(domain.displayName).isEqualTo("Test Model")
    assertThat(domain.author).isEqualTo("test-author")
    assertThat(domain.pipelineTag).isEqualTo("text-generation")
    assertThat(domain.libraryName).isEqualTo("transformers")
    assertThat(domain.tags).containsExactly("tag1", "tag2")
    assertThat(domain.likes).isEqualTo(100)
    assertThat(domain.downloads).isEqualTo(1000)
    assertThat(domain.license).isEqualTo("apache-2.0")
    assertThat(domain.languages).containsExactly("en", "es")
    assertThat(domain.baseModel).isEqualTo("base-model")
    assertThat(domain.datasets).containsExactly("dataset1")
    assertThat(domain.architectures).containsExactly("Transformer")
    assertThat(domain.modelType).isEqualTo("bert")
    assertThat(domain.baseModelRelations).containsExactly("relation1")
    assertThat(domain.hasGatedAccess).isFalse()
    assertThat(domain.isDisabled).isFalse()
    assertThat(domain.totalSizeBytes).isEqualTo(1000000L)
    assertThat(domain.summary).isEqualTo("A summary")
    assertThat(domain.description).isEqualTo("A description")
    assertThat(domain.trendingScore).isEqualTo(50)
    assertThat(domain.createdAt).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"))
    assertThat(domain.lastModified).isEqualTo(Instant.parse("2024-01-02T00:00:00Z"))
    assertThat(domain.isPrivate).isFalse()
  }

  @Test
  fun `round trip conversion preserves all data`() {
    val original =
      HuggingFaceModelSummary(
        modelId = "test-model",
        displayName = "Test Model",
        author = "test-author",
        pipelineTag = "text-generation",
        libraryName = "transformers",
        tags = listOf("tag1", "tag2"),
        likes = 100,
        downloads = 1000,
        license = "apache-2.0",
        languages = listOf("en", "es"),
        baseModel = "base-model",
        datasets = listOf("dataset1"),
        architectures = listOf("Transformer"),
        modelType = "bert",
        baseModelRelations = listOf("relation1"),
        hasGatedAccess = true,
        isDisabled = false,
        totalSizeBytes = 1000000L,
        summary = "A summary",
        description = "A description",
        trendingScore = 50,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        lastModified = Instant.parse("2024-01-02T00:00:00Z"),
        isPrivate = false,
      )

    val entity = HuggingFaceModelCacheMapper.toEntity(original)
    val roundTrip = HuggingFaceModelCacheMapper.toDomain(entity)

    assertThat(roundTrip).isEqualTo(original)
  }
}
