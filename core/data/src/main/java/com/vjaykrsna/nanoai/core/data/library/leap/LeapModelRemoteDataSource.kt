package com.vjaykrsna.nanoai.core.data.library.leap

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.library.catalog.ModelCatalogDataSource
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Instant

/**
 * A remote data source for Leap models. This class is responsible for fetching the curated list of
 * Leap models from the Leap API.
 */
@Singleton
class LeapModelRemoteDataSource
@Inject
constructor(private val modelCatalogDataSource: ModelCatalogDataSource) {

  /**
   * Fetches the curated list of Leap models.
   *
   * @return A list of [ModelPackage]s representing the Leap models.
   */
  suspend fun getModels(): List<ModelPackage> {
    // Use DataStore with network fallback
    val result =
      modelCatalogDataSource.refreshCatalog {
        // TODO: Replace with actual network request to Leap API
        // For now, return hardcoded models as before
        listOf(
          LeapModel(
              modelId = "phi3-mini-4k-instruct-f16",
              displayName = "Phi-3 Mini 4K Instruct",
              version = "1.0",
              sizeBytes = 2_000_000_000,
              downloadUrl = "https://leap.liquid.ai/models/phi3-mini-4k-instruct-f16.bundle",
              checksumSha256 = "1234567890abcdef",
              createdAt = Instant.parse("2024-01-01T00:00:00Z"),
              updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
              summary = "A small, powerful model for on-device text generation.",
              description =
                "The Phi-3 Mini 4K Instruct is a 3.8B parameter, lightweight, state-of-the-art open model by Microsoft.",
              author = "Microsoft",
              license = "MIT",
              languages = listOf("en"),
              tags = listOf("text-generation", "phi3"),
            )
            .toModelPackage(),
          LeapModel(
              modelId = "gemma-2b-it-f16",
              displayName = "Gemma 2B IT",
              version = "1.0",
              sizeBytes = 1_500_000_000,
              downloadUrl = "https://leap.liquid.ai/models/gemma-2b-it-f16.bundle",
              checksumSha256 = "abcdef1234567890",
              createdAt = Instant.parse("2024-01-01T00:00:00Z"),
              updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
              summary = "A smaller, instruction-tuned version of the Gemma model.",
              description =
                "Gemma is a family of lightweight, state-of-the-art open models built by Google DeepMind.",
              author = "Google",
              license = "Gemma Terms of Use",
              languages = listOf("en"),
              tags = listOf("text-generation", "gemma"),
            )
            .toModelPackage(),
        )
      }

    return when (result) {
      is NanoAIResult.Success -> result.value
      else -> emptyList()
    }
  }
}
