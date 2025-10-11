package com.vjaykrsna.nanoai.feature.library.domain

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.data.catalog.ModelCatalogSource
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import com.vjaykrsna.nanoai.model.catalog.DeliveryType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

class RefreshModelCatalogUseCaseTest {

  private val fakeSource = FakeModelCatalogSource()
  private val repository = mockk<ModelCatalogRepository>(relaxed = true)
  private val useCase = RefreshModelCatalogUseCase(fakeSource, repository)

  @Test
  fun `returns success when catalog is replaced`() = runTest {
    val models = listOf(sampleModel("model-success"))
    fakeSource.catalog = models

    val result = useCase()

    assertThat(result.isSuccess).isTrue()
    coVerify(exactly = 1) { repository.replaceCatalog(models) }
  }

  @Test
  fun `wraps repository failures with descriptive message`() = runTest {
    val models = listOf(sampleModel("model-failure"))
    fakeSource.catalog = models
    val underlying = IllegalStateException("repository locked")
    coEvery { repository.replaceCatalog(models) } throws underlying

    val result = useCase()

    assertThat(result.isFailure).isTrue()
    val failure = result.exceptionOrNull()
    assertThat(failure).isNotNull()
    assertThat(failure).hasMessageThat().contains("Failed to replace model catalog")
    assertThat(failure?.cause).isEqualTo(underlying)
  }

  private fun sampleModel(id: String): ModelPackage =
    ModelPackage(
      modelId = id,
      displayName = "Sample $id",
      version = "1.0.0",
      providerType = ProviderType.CLOUD_API,
      deliveryType = DeliveryType.CLOUD_FALLBACK,
      minAppVersion = 1,
      sizeBytes = 2048,
      capabilities = setOf("text"),
      installState = InstallState.NOT_INSTALLED,
      downloadTaskId = UUID.randomUUID(),
      manifestUrl = "https://example.com/$id",
      checksumSha256 = null,
      signature = null,
      createdAt = Instant.parse("2025-10-10T00:00:00Z"),
      updatedAt = Instant.parse("2025-10-10T00:00:00Z"),
    )

  private class FakeModelCatalogSource : ModelCatalogSource {
    var catalog: List<ModelPackage> = emptyList()

    override suspend fun fetchCatalog(): List<ModelPackage> = catalog
  }
}
