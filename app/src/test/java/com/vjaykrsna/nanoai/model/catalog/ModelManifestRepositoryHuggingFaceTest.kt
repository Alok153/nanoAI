package com.vjaykrsna.nanoai.model.catalog

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.device.DeviceIdentityProvider
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import com.vjaykrsna.nanoai.model.catalog.network.ModelCatalogService
import com.vjaykrsna.nanoai.model.huggingface.HuggingFaceManifestFetcher
import com.vjaykrsna.nanoai.model.huggingface.HuggingFaceManifestRequest
import com.vjaykrsna.nanoai.telemetry.TelemetryReporter
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class ModelManifestRepositoryHuggingFaceTest {
  private val service: ModelCatalogService = mockk(relaxed = true)
  private val localDataSource: ModelCatalogLocalDataSource = mockk(relaxed = true)
  private val json = Json { ignoreUnknownKeys = true }
  private val deviceIdentityProvider: DeviceIdentityProvider = mockk(relaxed = true)
  private val telemetryReporter: TelemetryReporter = mockk(relaxed = true)
  private val huggingFaceManifestFetcher: HuggingFaceManifestFetcher = mockk(relaxed = true)
  private val clock: Clock = FixedClock

  private lateinit var repository: ModelManifestRepository

  @Before
  fun setUp() {
    repository =
      ModelManifestRepositoryImpl(
        service = service,
        localDataSource = localDataSource,
        json = json,
        deviceIdentityProvider = deviceIdentityProvider,
        telemetryReporter = telemetryReporter,
        huggingFaceManifestFetcher = huggingFaceManifestFetcher,
        clock = clock,
      )
  }

  @Test
  fun `refreshManifest resolves Hugging Face locator`() = runTest {
    val modelId = "gemma"
    val manifestUrl = "hf://google/gemma-2-2b-it?artifact=LiteRT/model.bin&revision=main"
    val modelEntity =
      ModelPackageEntity(
        modelId = modelId,
        displayName = "Gemma",
        version = "main",
        providerType = ProviderType.MEDIA_PIPE,
        deliveryType = DeliveryType.LOCAL_ARCHIVE,
        minAppVersion = 1,
        sizeBytes = 0,
        capabilities = setOf("chat"),
        installState = InstallState.NOT_INSTALLED,
        downloadTaskId = null,
        manifestUrl = manifestUrl,
        checksumSha256 = null,
        signature = null,
        createdAt = clock.now(),
        updatedAt = clock.now(),
      )
    coEvery { localDataSource.getModel(modelId) } returns
      ModelPackageWithManifests(modelEntity, emptyList())
    coJustRun { localDataSource.cacheManifest(any()) }
    coJustRun { localDataSource.updateIntegrityMetadata(any(), any(), any()) }

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
    coEvery { huggingFaceManifestFetcher.fetchManifest(any()) } returns expectedManifest

    val result = repository.refreshManifest(modelId, "main")

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    val manifest = (result as NanoAIResult.Success).value
    assertThat(manifest).isEqualTo(expectedManifest)

    coVerify(exactly = 0) { service.getModelManifest(any(), any()) }
    coVerify {
      huggingFaceManifestFetcher.fetchManifest(
        match<HuggingFaceManifestRequest> { it.repository == "google/gemma-2-2b-it" }
      )
    }
  }

  @Test
  fun `reportVerification is no-op for Hugging Face models`() = runTest {
    val modelId = "gemma"
    val manifestUrl = "hf://google/gemma-2-2b-it?artifact=model.bin"
    val modelEntity =
      ModelPackageEntity(
        modelId = modelId,
        displayName = "Gemma",
        version = "main",
        providerType = ProviderType.MEDIA_PIPE,
        deliveryType = DeliveryType.LOCAL_ARCHIVE,
        minAppVersion = 1,
        sizeBytes = 0,
        capabilities = emptySet(),
        installState = InstallState.NOT_INSTALLED,
        downloadTaskId = null,
        manifestUrl = manifestUrl,
        checksumSha256 = null,
        signature = null,
        createdAt = clock.now(),
        updatedAt = clock.now(),
      )
    coEvery { localDataSource.getModel(modelId) } returns
      ModelPackageWithManifests(modelEntity, emptyList())

    val result =
      repository.reportVerification(
        modelId = modelId,
        version = "main",
        checksumSha256 = "${"a".repeat(64)}",
        status = VerificationOutcome.SUCCESS,
        failureReason = null,
      )

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    coVerify { service wasNot Called }
  }

  private object FixedClock : Clock {
    private val fixed = Instant.parse("2024-01-01T00:00:00Z")

    override fun now(): Instant = fixed
  }
}
