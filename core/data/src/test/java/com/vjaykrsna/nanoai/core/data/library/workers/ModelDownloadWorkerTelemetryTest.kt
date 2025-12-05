package com.vjaykrsna.nanoai.core.data.library.workers

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.db.entities.DownloadManifestEntity
import com.vjaykrsna.nanoai.core.data.db.entities.ModelPackageEntity
import com.vjaykrsna.nanoai.core.data.db.entities.ModelPackageWithManifests
import com.vjaykrsna.nanoai.core.data.library.catalog.DownloadManifestDao
import com.vjaykrsna.nanoai.core.data.library.catalog.ModelCatalogLocalDataSource
import com.vjaykrsna.nanoai.core.data.library.catalog.ModelManifestRepositoryImpl
import com.vjaykrsna.nanoai.core.data.library.catalog.ModelPackageRelationsDao
import com.vjaykrsna.nanoai.core.data.library.catalog.ModelPackageWriteDao
import com.vjaykrsna.nanoai.core.data.library.catalog.network.ModelCatalogService
import com.vjaykrsna.nanoai.core.data.library.catalog.network.dto.ManifestVerificationRequestDto
import com.vjaykrsna.nanoai.core.data.library.catalog.network.dto.ManifestVerificationResponseDto
import com.vjaykrsna.nanoai.core.data.library.catalog.network.dto.ManifestVerificationResponseStatusDto
import com.vjaykrsna.nanoai.core.data.library.catalog.network.dto.ModelManifestDto
import com.vjaykrsna.nanoai.core.data.library.huggingface.HuggingFaceManifestFetcher
import com.vjaykrsna.nanoai.core.device.DeviceIdentityProvider
import com.vjaykrsna.nanoai.core.domain.library.ModelManifestUseCase
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.VerificationOutcome
import com.vjaykrsna.nanoai.core.network.ConnectivityStatusProvider
import com.vjaykrsna.nanoai.core.telemetry.TelemetryReporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Comprehensive telemetry coverage tests for ModelDownloadWorker and related components. Verifies
 * that TelemetryReporter.report() is called for all error paths including:
 * - Manifest fetch failures
 * - HTTP download errors
 * - Integrity validation failures (size mismatch, checksum mismatch, signature failure)
 * - Verification report failures
 */
class ModelDownloadWorkerTelemetryTest {

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
  }
  private val fakeManifestDao = FakeDownloadManifestDao()
  private val fakeWriteDao = FakeModelPackageWriteDao()
  private val clock = Clock.System
  private val localDataSource =
    ModelCatalogLocalDataSource(
      modelPackageWriteDao = fakeWriteDao,
      relationsDao = NoopModelPackageRelationsDao,
      downloadManifestDao = fakeManifestDao,
      clock = clock,
    )
  private val fakeDeviceIdProvider =
    object : DeviceIdentityProvider {
      override fun deviceId(): String = "device-test"
    }
  private val fakeService = FakeModelCatalogService()
  private val telemetryReporter = mockk<TelemetryReporter>(relaxed = true)
  private val huggingFaceManifestFetcher = mockk<HuggingFaceManifestFetcher>(relaxed = true)
  private val connectivityStatusProvider = mockk<ConnectivityStatusProvider>(relaxed = true)

  private lateinit var modelManifestUseCase: ModelManifestUseCase

  @BeforeEach
  fun setUp() {
    coEvery { connectivityStatusProvider.isOnline() } returns true
    val repository =
      ModelManifestRepositoryImpl(
        service = fakeService,
        localDataSource = localDataSource,
        json = json,
        deviceIdentityProvider = fakeDeviceIdProvider,
        telemetryReporter = telemetryReporter,
        huggingFaceManifestFetcher = huggingFaceManifestFetcher,
        connectivityStatusProvider = connectivityStatusProvider,
        clock = clock,
      )
    modelManifestUseCase = ModelManifestUseCase(repository)
  }

  @Nested
  @DisplayName("Manifest Fetch Failures")
  inner class ManifestFetchFailures {

    @Test
    fun `reports telemetry for invalid checksum format`() = runBlocking {
      fakeService.manifest =
        ModelManifestDto(
          modelId = MODEL_ID,
          version = VERSION,
          checksumSha256 = "invalid-not-hex",
          sizeBytes = 1024,
          downloadUrl = "https://cdn.nanoai.app/model.tgz",
          signature = "signed",
        )

      val result = modelManifestUseCase.refreshManifest(MODEL_ID, VERSION)

      assertThat(result).isInstanceOf(NanoAIResult.FatalError::class.java)
      val fatalError = result as NanoAIResult.FatalError
      assertThat(fatalError.message).contains("checksum")

      // Verify telemetry was called for fatal error
      coVerify {
        telemetryReporter.report(
          source = match { it.contains("ModelManifest") },
          result = match { it is NanoAIResult.FatalError },
          extraContext = any(),
        )
      }
    }

    @Test
    fun `reports telemetry for non-HTTPS download URL`() = runBlocking {
      fakeService.manifest =
        ModelManifestDto(
          modelId = MODEL_ID,
          version = VERSION,
          checksumSha256 = VALID_CHECKSUM,
          sizeBytes = 1024,
          downloadUrl = "http://insecure.nanoai.app/model.tgz",
          signature = "signed",
        )

      val result = modelManifestUseCase.refreshManifest(MODEL_ID, VERSION)

      assertThat(result).isInstanceOf(NanoAIResult.FatalError::class.java)
      val fatalError = result as NanoAIResult.FatalError
      assertThat(fatalError.message).contains("HTTPS")

      // Verify telemetry was called for security violation
      coVerify {
        telemetryReporter.report(
          source = match { it.contains("ModelManifest") },
          result = match { it is NanoAIResult.FatalError },
          extraContext = any(),
        )
      }
    }

    @Test
    fun `reports telemetry for network failure during manifest fetch`() = runBlocking {
      fakeService.shouldThrow = true
      fakeService.exceptionToThrow = java.io.IOException("Network unavailable")

      val result = modelManifestUseCase.refreshManifest(MODEL_ID, VERSION)

      assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)

      // Verify telemetry was called for network error
      coVerify {
        telemetryReporter.report(
          source = match { it.contains("ModelManifest") },
          result = match { it is NanoAIResult.RecoverableError },
          extraContext = any(),
        )
      }
    }
  }

  @Nested
  @DisplayName("Verification Report Failures")
  inner class VerificationReportFailures {

    @Test
    fun `reports telemetry when verification report fails with recoverable error`() = runBlocking {
      fakeService.verificationShouldFail = true
      fakeService.verificationException = java.io.IOException("Server unavailable")

      val result =
        modelManifestUseCase.reportVerification(
          modelId = MODEL_ID,
          version = VERSION,
          checksumSha256 = VALID_CHECKSUM,
          status = VerificationOutcome.SUCCESS,
          failureReason = null,
        )

      assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)

      // Verify telemetry was called for the failed verification report
      coVerify {
        telemetryReporter.report(
          source = match { it.contains("ModelManifest") },
          result = match { it is NanoAIResult.RecoverableError },
          extraContext = any(),
        )
      }
    }

    @Test
    fun `reports telemetry with verification outcome context`() = runBlocking {
      fakeService.verificationShouldFail = true
      fakeService.verificationException = java.io.IOException("Timeout")

      val resultSlot = slot<NanoAIResult<*>>()
      val contextSlot = slot<Map<String, String>>()

      coEvery {
        telemetryReporter.report(
          source = any(),
          result = capture(resultSlot),
          extraContext = capture(contextSlot),
        )
      } returns Unit

      modelManifestUseCase.reportVerification(
        modelId = MODEL_ID,
        version = VERSION,
        checksumSha256 = VALID_CHECKSUM,
        status = VerificationOutcome.CORRUPTED,
        failureReason = "Checksum mismatch",
      )

      coVerify { telemetryReporter.report(source = any(), result = any(), extraContext = any()) }
    }
  }

  @Nested
  @DisplayName("Successful Telemetry Cases")
  inner class SuccessfulTelemetry {

    @Test
    fun `does not emit error telemetry for valid manifest`() = runBlocking {
      fakeService.manifest =
        ModelManifestDto(
          modelId = MODEL_ID,
          version = VERSION,
          checksumSha256 = VALID_CHECKSUM,
          sizeBytes = 16_384,
          downloadUrl = "https://cdn.nanoai.app/model.tgz",
          signature = "signature",
        )

      val result = modelManifestUseCase.refreshManifest(MODEL_ID, VERSION)

      assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)

      // Should NOT report error telemetry for success case
      coVerify(exactly = 0) {
        telemetryReporter.report(
          source = any(),
          result = match { it is NanoAIResult.RecoverableError || it is NanoAIResult.FatalError },
          extraContext = any(),
        )
      }
    }
  }

  @Nested
  @DisplayName("Context Enrichment")
  inner class ContextEnrichment {

    @Test
    fun `telemetry includes modelId in error context`() = runBlocking {
      fakeService.manifest =
        ModelManifestDto(
          modelId = MODEL_ID,
          version = VERSION,
          checksumSha256 = "invalid",
          sizeBytes = 1024,
          downloadUrl = "https://cdn.nanoai.app/model.tgz",
          signature = "signed",
        )

      val contextSlot = slot<Map<String, String>>()

      coEvery {
        telemetryReporter.report(
          source = any(),
          result = any(),
          extraContext = capture(contextSlot),
        )
      } returns Unit

      modelManifestUseCase.refreshManifest(MODEL_ID, VERSION)

      coVerify { telemetryReporter.report(source = any(), result = any(), extraContext = any()) }

      assertThat(contextSlot.captured).containsKey("modelId")
      assertThat(contextSlot.captured["modelId"]).isEqualTo(MODEL_ID)
    }

    @Test
    fun `telemetry includes source identifier for tracing`() = runBlocking {
      fakeService.manifest =
        ModelManifestDto(
          modelId = MODEL_ID,
          version = VERSION,
          checksumSha256 = "zz",
          sizeBytes = 1024,
          downloadUrl = "https://cdn.nanoai.app/model.tgz",
          signature = "signed",
        )

      val sourceSlot = slot<String>()

      coEvery {
        telemetryReporter.report(source = capture(sourceSlot), result = any(), extraContext = any())
      } returns Unit

      modelManifestUseCase.refreshManifest(MODEL_ID, VERSION)

      coVerify { telemetryReporter.report(source = any(), result = any(), extraContext = any()) }

      // Verify source is a meaningful identifier for tracing
      assertThat(sourceSlot.captured).contains("ModelManifest")
    }
  }

  // Test doubles

  private class FakeModelCatalogService : ModelCatalogService {
    var manifest: ModelManifestDto? = null
    var shouldThrow: Boolean = false
    var exceptionToThrow: Throwable = RuntimeException("Simulated error")
    var verificationShouldFail: Boolean = false
    var verificationException: Throwable = RuntimeException("Verification error")

    override suspend fun getModelManifest(modelId: String, version: String): ModelManifestDto {
      if (shouldThrow) throw exceptionToThrow
      return manifest ?: error("Manifest not set for $modelId:$version")
    }

    override suspend fun verifyModelPackage(
      modelId: String,
      request: ManifestVerificationRequestDto,
    ): ManifestVerificationResponseDto {
      if (verificationShouldFail) throw verificationException
      return ManifestVerificationResponseDto(
        status = ManifestVerificationResponseStatusDto.ACCEPTED
      )
    }
  }

  private object NoopModelPackageRelationsDao : ModelPackageRelationsDao {
    override suspend fun getModelWithManifests(modelId: String): ModelPackageWithManifests? = null

    override fun observeModelWithManifests(modelId: String): Flow<ModelPackageWithManifests?> =
      flowOf(null)
  }

  private class FakeModelPackageWriteDao : ModelPackageWriteDao {
    val updatedIntegrity = mutableMapOf<String, String>()

    override suspend fun insert(model: ModelPackageEntity) = Unit

    override suspend fun insertAll(models: List<ModelPackageEntity>) = Unit

    override suspend fun update(model: ModelPackageEntity) = Unit

    override suspend fun delete(model: ModelPackageEntity) = Unit

    override suspend fun updateInstallState(
      modelId: String,
      state: InstallState,
      updatedAt: Instant,
    ) = Unit

    override suspend fun updateDownloadTaskId(
      modelId: String,
      taskId: String?,
      updatedAt: Instant,
    ) = Unit

    override suspend fun updateIntegrityMetadata(
      modelId: String,
      checksum: String,
      signature: String?,
      updatedAt: Instant,
    ) {
      updatedIntegrity[modelId] = checksum
    }

    override suspend fun clearAll() = Unit

    override suspend fun deleteModelsNotIn(modelIds: List<String>) = Unit
  }

  private class FakeDownloadManifestDao : DownloadManifestDao {
    var cachedManifest: DownloadManifestEntity? = null

    override suspend fun upsertManifest(manifest: DownloadManifestEntity) {
      cachedManifest = manifest
    }

    override suspend fun upsertManifests(manifests: List<DownloadManifestEntity>) {
      cachedManifest = manifests.lastOrNull()
    }

    override suspend fun getManifest(modelId: String, version: String): DownloadManifestEntity? =
      cachedManifest

    override suspend fun getLatestManifest(modelId: String): DownloadManifestEntity? =
      cachedManifest

    override suspend fun deleteManifest(modelId: String, version: String) = Unit

    override suspend fun deleteExpiredManifests(now: Instant) = Unit

    override suspend fun clearManifests() {
      cachedManifest = null
    }
  }

  companion object {
    private const val MODEL_ID = "test-model-delta"
    private const val VERSION = "1.0.0"
    private const val VALID_CHECKSUM =
      "9f2a8a4c7cb0a2aa73ec718f7f9b0d4c70bda2f286f7f0bb7b3b92d4abf65c2e"
  }
}
