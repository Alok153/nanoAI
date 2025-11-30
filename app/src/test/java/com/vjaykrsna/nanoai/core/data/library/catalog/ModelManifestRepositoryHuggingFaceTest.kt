package com.vjaykrsna.nanoai.core.data.library.catalog

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.db.entities.ModelPackageEntity
import com.vjaykrsna.nanoai.core.data.db.entities.ModelPackageWithManifests
import com.vjaykrsna.nanoai.core.data.library.catalog.network.ModelCatalogService
import com.vjaykrsna.nanoai.core.data.library.huggingface.HuggingFaceManifestFetcher
import com.vjaykrsna.nanoai.core.device.DeviceIdentityProvider
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadManifest
import com.vjaykrsna.nanoai.core.domain.model.library.VerificationOutcome
import com.vjaykrsna.nanoai.core.network.ConnectivityStatusProvider
import com.vjaykrsna.nanoai.core.telemetry.TelemetryReporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ModelManifestRepositoryHuggingFaceTest {
  private val service: ModelCatalogService = mockk()
  private val localDataSource: ModelCatalogLocalDataSource = mockk(relaxed = true)
  private val deviceIdentityProvider: DeviceIdentityProvider = mockk()
  private val telemetryReporter: TelemetryReporter = mockk(relaxed = true)
  private val huggingFaceManifestFetcher: HuggingFaceManifestFetcher = mockk()
  private val connectivityStatusProvider: ConnectivityStatusProvider = mockk()
  private val clock: Clock = FixedClock

  private lateinit var repository: ModelManifestRepositoryImpl

  @BeforeEach
  fun setUp() {
    repository =
      ModelManifestRepositoryImpl(
        service = service,
        localDataSource = localDataSource,
        json = Json,
        deviceIdentityProvider = deviceIdentityProvider,
        telemetryReporter = telemetryReporter,
        huggingFaceManifestFetcher = huggingFaceManifestFetcher,
        connectivityStatusProvider = connectivityStatusProvider,
        clock = clock,
      )
  }

  @Test
  fun `refreshManifest resolves Hugging Face locator`() = runTest {
    val modelId = "gemma"
    val expectedManifest =
      DownloadManifest(
        modelId = modelId,
        version = "main",
        checksumSha256 = "${"d".repeat(64)}",
        sizeBytes = 1024L,
        downloadUrl =
          "https://huggingface.co/google/gemma-2-2b-it/resolve/main/LiteRT/model.bin?download=1",
        signature = null,
        publicKeyUrl = null,
        expiresAt = null,
        fetchedAt = clock.now(),
      )
    coEvery { connectivityStatusProvider.isOnline() } returns true
    coEvery { localDataSource.getModel(modelId) } returns
      huggingFaceModel("hf://repo?artifact=path")
    coEvery { huggingFaceManifestFetcher.fetchManifest(any()) } returns expectedManifest

    val result = repository.refreshManifest(modelId, "main")

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    val manifest = (result as NanoAIResult.Success).value
    assertThat(manifest).isEqualTo(expectedManifest)
    coVerify { localDataSource.cacheManifest(any()) }
  }

  @Test
  fun `reportVerification is no-op for Hugging Face models`() = runTest {
    val modelId = "gemma"
    coEvery { localDataSource.getModel(modelId) } returns
      huggingFaceModel("hf://repo?artifact=path")

    val result =
      repository.reportVerification(
        modelId = modelId,
        version = "main",
        checksumSha256 = "${"a".repeat(64)}",
        status = VerificationOutcome.SUCCESS,
        failureReason = null,
      )

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
  }

  private fun huggingFaceModel(manifestUrl: String): ModelPackageWithManifests {
    val modelEntity = mockk<ModelPackageEntity>(relaxed = true)
    every { modelEntity.manifestUrl } returns manifestUrl
    return ModelPackageWithManifests(model = modelEntity, manifests = emptyList())
  }

  private object FixedClock : Clock {
    private val fixed = Instant.parse("2024-01-01T00:00:00Z")

    override fun now(): Instant = fixed
  }
}
