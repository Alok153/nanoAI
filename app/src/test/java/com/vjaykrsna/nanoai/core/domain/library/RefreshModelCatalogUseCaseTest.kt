package com.vjaykrsna.nanoai.core.domain.library

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.testing.assertIsSuccess
import com.vjaykrsna.nanoai.testing.assertRecoverableError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
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

    result.assertIsSuccess()
    coVerify(exactly = 1) { repository.replaceCatalog(models) }
    coVerify(exactly = 1) { repository.recordRefreshSuccess(any(), models.size) }
  }

  @Test
  fun `wraps repository failures with descriptive message`() = runTest {
    val models = listOf(sampleModel("model-failure"))
    fakeSource.catalog = models
    val underlying = IllegalStateException("repository locked")
    coEvery { repository.replaceCatalog(models) } throws underlying

    val result = useCase()

    val error = result.assertRecoverableError()
    assertThat(error.message).isEqualTo("Failed to replace model catalog")
    assertThat(error.cause).isInstanceOf(IllegalStateException::class.java)
    assertThat(error.cause?.message).isEqualTo("Failed to replace model catalog")
    assertThat(error.cause?.cause).isEqualTo(underlying)
    coVerify(exactly = 0) { repository.recordRefreshSuccess(any(), any()) }
  }

  @Test
  fun `returns cached success when remote fetch fails`() = runTest {
    fakeSource.error = IOException("remote unavailable")

    val result = useCase()

    result.assertIsSuccess()
    coVerify(exactly = 1) { repository.recordOfflineFallback("IOException", 0, any()) }
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
    var error: Throwable? = null

    override suspend fun fetchCatalog(): List<ModelPackage> = error?.let { throw it } ?: catalog
  }
}
