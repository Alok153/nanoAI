package com.vjaykrsna.nanoai.model.download

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.device.DeviceIdentityProvider
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import com.vjaykrsna.nanoai.model.catalog.DownloadManifestEntity
import com.vjaykrsna.nanoai.model.catalog.ModelCatalogDao
import com.vjaykrsna.nanoai.model.catalog.ModelCatalogLocalDataSource
import com.vjaykrsna.nanoai.model.catalog.ModelManifestRepositoryImpl
import com.vjaykrsna.nanoai.model.catalog.ModelPackageEntity
import com.vjaykrsna.nanoai.model.catalog.ModelPackageWithManifests
import com.vjaykrsna.nanoai.model.catalog.network.ModelCatalogService
import com.vjaykrsna.nanoai.model.catalog.network.dto.ManifestVerificationRequestDto
import com.vjaykrsna.nanoai.model.catalog.network.dto.ManifestVerificationResponseDto
import com.vjaykrsna.nanoai.model.catalog.network.dto.ManifestVerificationResponseStatusDto
import com.vjaykrsna.nanoai.model.catalog.network.dto.ModelManifestDto
import com.vjaykrsna.nanoai.telemetry.TelemetryReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Regression coverage for manifest integrity handling. Verifies manifests must be signed, use
 * HTTPS, and persist metadata for WorkManager integrity validation.
 */
class ModelDownloadWorkerIntegrityTest {
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
  }
  private val fakeDao = FakeModelCatalogDao()
  private val localDataSource = ModelCatalogLocalDataSource(fakeDao)
  private val clock = Clock.System
  private val fakeDeviceIdProvider =
    object : DeviceIdentityProvider {
      override fun deviceId(): String = "device-test"
    }
  private val fakeService = FakeModelCatalogService()
  private val telemetryReporter = TelemetryReporter()
  private val repository =
    ModelManifestRepositoryImpl(
      service = fakeService,
      localDataSource = localDataSource,
      json = json,
      deviceIdentityProvider = fakeDeviceIdProvider,
      telemetryReporter = telemetryReporter,
      clock = clock,
    )

  @Test
  fun `refreshManifest rejects non-hex checksum`() = runBlocking {
    fakeService.manifest =
      ModelManifestDto(
        modelId = "persona-text-delta",
        version = "1.2.0",
        checksumSha256 = "deadbeef",
        sizeBytes = 1234,
        downloadUrl = "https://cdn.nanoai.app/models/persona-text-delta.tgz",
        signature = "signed",
      )

    val result = repository.refreshManifest("persona-text-delta", "1.2.0")

    val fatal = result as? NanoAIResult.FatalError
    assertThat(fatal).isNotNull()
    assertThat(fatal!!.message).contains("checksum")
    assertThat(fakeDao.cachedManifest).isNull()
  }

  @Test
  fun `refreshManifest rejects non-https download urls`() = runBlocking {
    fakeService.manifest =
      ModelManifestDto(
        modelId = "persona-text-delta",
        version = "1.2.0",
        checksumSha256 = VALID_CHECKSUM,
        sizeBytes = 8192,
        downloadUrl = "http://insecure.nanoai.app/model.bin",
        signature = "signed",
      )

    val result = repository.refreshManifest("persona-text-delta", "1.2.0")

    val fatal = result as? NanoAIResult.FatalError
    assertThat(fatal).isNotNull()
    assertThat(fatal!!.message).contains("HTTPS")
    assertThat(fakeDao.cachedManifest).isNull()
  }

  @Test
  fun `refreshManifest caches manifest metadata when payload valid`() = runBlocking {
    fakeService.manifest =
      ModelManifestDto(
        modelId = "persona-text-delta",
        version = "1.2.0",
        checksumSha256 = VALID_CHECKSUM,
        sizeBytes = 16_384,
        downloadUrl = "https://cdn.nanoai.app/persona-text-delta.tgz",
        signature = "signature",
      )

    val result = repository.refreshManifest("persona-text-delta", "1.2.0")

    val success = result as? NanoAIResult.Success
    assertThat(success).isNotNull()
    assertThat(fakeDao.cachedManifest).isNotNull()
    assertThat(fakeDao.cachedManifest!!.checksumSha256).isEqualTo(VALID_CHECKSUM)
    assertThat(fakeDao.updatedIntegrity["persona-text-delta"]).isEqualTo(VALID_CHECKSUM)
  }

  private class FakeModelCatalogService : ModelCatalogService {
    var manifest: ModelManifestDto? = null

    override suspend fun getModelManifest(modelId: String, version: String): ModelManifestDto =
      manifest ?: error("Manifest not set for $modelId:$version")

    override suspend fun verifyModelPackage(
      modelId: String,
      request: ManifestVerificationRequestDto,
    ): ManifestVerificationResponseDto =
      ManifestVerificationResponseDto(
        status = ManifestVerificationResponseStatusDto.ACCEPTED,
      )
  }

  private class FakeModelCatalogDao : ModelCatalogDao {
    var cachedManifest: DownloadManifestEntity? = null
    val updatedIntegrity = mutableMapOf<String, String>()

    override suspend fun upsertManifest(manifest: DownloadManifestEntity) {
      cachedManifest = manifest
    }

    override suspend fun updateIntegrityMetadata(
      modelId: String,
      checksum: String,
      signature: String?,
      updatedAt: Instant,
    ) {
      updatedIntegrity[modelId] = checksum
    }

    override suspend fun insert(model: ModelPackageEntity) = throw UnsupportedOperationException()

    override suspend fun insertAll(models: List<ModelPackageEntity>) =
      throw UnsupportedOperationException()

    override suspend fun update(model: ModelPackageEntity) = throw UnsupportedOperationException()

    override suspend fun delete(model: ModelPackageEntity) = throw UnsupportedOperationException()

    override suspend fun getById(modelId: String): ModelPackageEntity? =
      throw UnsupportedOperationException()

    override fun observeById(modelId: String): Flow<ModelPackageEntity?> =
      throw UnsupportedOperationException()

    override suspend fun getAll(): List<ModelPackageEntity> = throw UnsupportedOperationException()

    override fun observeAll(): Flow<List<ModelPackageEntity>> =
      throw UnsupportedOperationException()

    override suspend fun getByInstallState(state: InstallState): List<ModelPackageEntity> =
      throw UnsupportedOperationException()

    override fun observeInstalled(): Flow<List<ModelPackageEntity>> =
      throw UnsupportedOperationException()

    override suspend fun getByProviderType(providerType: ProviderType): List<ModelPackageEntity> =
      throw UnsupportedOperationException()

    override suspend fun getByCapability(capability: String): List<ModelPackageEntity> =
      throw UnsupportedOperationException()

    override suspend fun getDownloading(): List<ModelPackageEntity> =
      throw UnsupportedOperationException()

    override suspend fun updateInstallState(
      modelId: String,
      state: InstallState,
      updatedAt: Instant,
    ) = throw UnsupportedOperationException()

    override suspend fun updateDownloadTaskId(
      modelId: String,
      taskId: String?,
      updatedAt: Instant
    ) = throw UnsupportedOperationException()

    override suspend fun deleteAll() = throw UnsupportedOperationException()

    override suspend fun getTotalInstalledSize(): Long? = throw UnsupportedOperationException()

    override suspend fun countInstalled(): Int = throw UnsupportedOperationException()

    override suspend fun upsertManifests(manifests: List<DownloadManifestEntity>) =
      throw UnsupportedOperationException()

    override suspend fun getManifest(modelId: String, version: String): DownloadManifestEntity? =
      cachedManifest

    override suspend fun getLatestManifest(modelId: String): DownloadManifestEntity? =
      cachedManifest

    override suspend fun deleteManifest(modelId: String, version: String) = Unit

    override suspend fun deleteExpiredManifests(now: Instant) = Unit

    override suspend fun clearManifests() {
      cachedManifest = null
    }

    override suspend fun getModelWithManifests(modelId: String): ModelPackageWithManifests? =
      throw UnsupportedOperationException()

    override fun observeModelWithManifests(modelId: String): Flow<ModelPackageWithManifests?> =
      throw UnsupportedOperationException()
  }

  companion object {
    private const val VALID_CHECKSUM =
      "9f2a8a4c7cb0a2aa73ec718f7f9b0d4c70bda2f286f7f0bb7b3b92d4abf65c2e"
  }
}
