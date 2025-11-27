package com.vjaykrsna.nanoai.feature.library.presentation.util

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryFilterState
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelSort
import java.util.Locale
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

class ModelLibraryFiltersTest {

  @Test
  fun `text search matches metadata fields`() {
    val models =
      listOf(
        sampleModel(modelId = "alpha", displayName = "Alpha", summary = "great helper"),
        sampleModel(modelId = "beta", displayName = "Beta", description = "Vision pro"),
      )

    val filtered = models.filterBy(LibraryFilterState(localSearchQuery = "vision"))

    assertThat(filtered.map { it.modelId }).containsExactly("beta")
  }

  @Test
  fun `provider filter restricts catalog`() {
    val models =
      listOf(
        sampleModel(modelId = "a", displayName = "Alpha", provider = ProviderType.CLOUD_API),
        sampleModel(modelId = "b", displayName = "Beta", provider = ProviderType.MLC_LLM),
      )

    val filtered = models.filterBy(LibraryFilterState(localLibrary = ProviderType.MLC_LLM))

    assertThat(filtered).hasSize(1)
    assertThat(filtered.first().providerType).isEqualTo(ProviderType.MLC_LLM)
  }

  @Test
  fun `capability filter requires all selected entries`() {
    val models =
      listOf(
        sampleModel(modelId = "multi", capabilities = setOf("Vision", "Audio")),
        sampleModel(modelId = "single", capabilities = setOf("Vision")),
      )

    val filters = LibraryFilterState(selectedCapabilities = setOf("vision", "audio"))

    val filtered = models.filterBy(filters)

    assertThat(filtered.map { it.modelId }).containsExactly("multi")
  }

  @Test
  fun `pipeline filter enforces exact capability match`() {
    val models =
      listOf(
        sampleModel(modelId = "vision", capabilities = setOf("vision")),
        sampleModel(modelId = "chat", capabilities = setOf("text")),
      )

    val filtered = models.filterBy(LibraryFilterState(pipelineTag = "VISION"))

    assertThat(filtered.map { it.modelId }).containsExactly("vision")
  }

  @Test
  fun `recommended sort prioritizes installed and active downloads`() {
    val models =
      listOf(
        sampleModel(
          modelId = "installed",
          displayName = "Zeta",
          installState = InstallState.INSTALLED,
        ),
        sampleModel(
          modelId = "downloading",
          displayName = "Alpha",
          installState = InstallState.DOWNLOADING,
        ),
        sampleModel(
          modelId = "available",
          displayName = "Beta",
          installState = InstallState.NOT_INSTALLED,
        ),
      )

    val filters = LibraryFilterState(localSort = ModelSort.RECOMMENDED)
    val filtered = models.filterBy(filters)

    assertThat(filtered.map { it.modelId })
      .containsExactly("installed", "downloading", "available")
      .inOrder()
  }

  private fun sampleModel(
    modelId: String,
    displayName: String = modelId.uppercase(Locale.US),
    provider: ProviderType = ProviderType.CLOUD_API,
    capabilities: Set<String> = setOf("text"),
    installState: InstallState = InstallState.NOT_INSTALLED,
    summary: String? = null,
    description: String? = null,
  ): ModelPackage =
    ModelPackage(
      modelId = modelId,
      displayName = displayName,
      version = "1.0",
      providerType = provider,
      deliveryType = DeliveryType.LOCAL_ARCHIVE,
      minAppVersion = 1,
      sizeBytes = 1024,
      capabilities = capabilities,
      installState = installState,
      downloadTaskId = null,
      manifestUrl = "https://example.com/$modelId",
      checksumSha256 = null,
      signature = null,
      createdAt = Instant.parse("2024-01-01T00:00:00Z"),
      updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
      author = "NanoAI",
      license = "Apache-2.0",
      languages = emptyList(),
      baseModel = null,
      architectures = emptyList(),
      modelType = null,
      summary = summary,
      description = description,
    )
}
