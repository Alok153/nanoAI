package com.vjaykrsna.nanoai.feature.library.ui

import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

class ModelLibraryCardMetadataTest {

  @Test
  fun buildModelMetadataTags_includesAvailableFields() {
    val model =
      baseModelPackage()
        .copy(
          license = "Apache-2.0",
          architectures = listOf("transformer"),
          modelType = "text",
          languages = listOf("en"),
        )

    val metadata = buildModelMetadataTags(model)

    assertEquals(
      listOf("License: Apache-2.0", "Arch: transformer", "Type: text", "Lang: en"),
      metadata,
    )
  }

  @Test
  fun buildModelMetadataTags_returnsEmptyWhenNoMetadata() {
    val metadata = buildModelMetadataTags(baseModelPackage())

    assertTrue(metadata.isEmpty())
  }

  @Test
  fun buildCapabilityTags_combinesPipelineLibraryAndTags() {
    val summary =
      baseHuggingFaceSummary()
        .copy(
          pipelineTag = "text-generation",
          libraryName = "transformers",
          tags = listOf("featured", "recommended"),
        )

    val tags = buildCapabilityTags(summary)

    assertEquals(listOf("text-generation", "transformers", "featured", "recommended"), tags)
  }

  @Test
  fun buildMetadataLines_includesFormattedDetails() {
    val summary =
      baseHuggingFaceSummary()
        .copy(
          license = "mit",
          languages = listOf("en", "fr"),
          baseModel = "llama",
          architectures = listOf("transformer"),
          modelType = "text",
          totalSizeBytes = 2_048,
          createdAt = Instant.parse("2024-01-01T00:00:00Z"),
          lastModified = Instant.parse("2024-02-01T00:00:00Z"),
        )

    val metadata = buildMetadataLines(summary)

    val expectedUpdated = formatUpdated(summary.lastModified!!)
    val expectedCreated = formatUpdated(summary.createdAt!!)
    assertEquals(
      listOf(
        "License: mit",
        "Languages: en, fr",
        "Base model: llama",
        "Architectures: transformer",
        "Type: text",
        "Size: 2.0 KB",
        expectedUpdated,
        expectedCreated,
      ),
      metadata,
    )
  }

  private fun baseModelPackage(): ModelPackage =
    ModelPackage(
      modelId = "model",
      displayName = "Test Model",
      version = "1.0.0",
      providerType = ProviderType.MEDIA_PIPE,
      deliveryType = DeliveryType.LOCAL_ARCHIVE,
      minAppVersion = 1,
      sizeBytes = 0,
      capabilities = emptySet(),
      installState = InstallState.NOT_INSTALLED,
      manifestUrl = "https://example.com",
      createdAt = Instant.parse("2024-01-01T00:00:00Z"),
      updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
    )

  private fun baseHuggingFaceSummary(): HuggingFaceModelSummary =
    HuggingFaceModelSummary(
      modelId = "id",
      displayName = "Display",
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
}
