package com.vjaykrsna.nanoai.feature.library.data.impl

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.domain.InstallState
import com.vjaykrsna.nanoai.feature.library.domain.ProviderType
import com.vjaykrsna.nanoai.shared.model.catalog.DeliveryType
import com.vjaykrsna.nanoai.shared.model.catalog.ModelPackageEntity
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.slot
import java.io.File
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ModelCatalogRepositoryCatalogSyncTest {

  @TempDir lateinit var tempDir: File

  private lateinit var fixture: ModelCatalogRepositoryTestFixture

  @BeforeEach
  fun setUp() {
    fixture = createModelCatalogRepositoryFixture(tempDir)
  }

  @Test
  fun `replaceCatalog preserves integrity metadata when incoming values blank`() = runTest {
    val existing =
      fixture.existingEntity(
        modelId = "model-integrity",
        checksum = "existing-checksum",
        signature = "existing-signature",
        downloadTaskId = UUID.fromString("f8f0b23e-1111-4e75-9f6a-2fd2e3c4f5a6"),
        installState = InstallState.INSTALLED,
      )
    coEvery { fixture.modelPackageReadDao.getAll() } returns listOf(existing)
    val models =
      listOf(
        ModelPackage(
          modelId = existing.modelId,
          displayName = "Model Integrity",
          version = "2.0",
          providerType = ProviderType.CLOUD_API,
          deliveryType = DeliveryType.CLOUD_FALLBACK,
          minAppVersion = 10,
          sizeBytes = 42,
          capabilities = setOf("text"),
          installState = InstallState.NOT_INSTALLED,
          downloadTaskId = null,
          manifestUrl = "https://example.com/model-integrity",
          checksumSha256 = "",
          signature = "",
          createdAt = Instant.parse("2025-10-09T00:00:00Z"),
          updatedAt = Instant.parse("2025-10-10T00:00:00Z"),
        )
      )
    val captured = slot<List<ModelPackageEntity>>()
    coJustRun { fixture.modelPackageWriteDao.replaceCatalog(capture(captured)) }

    fixture.repository.replaceCatalog(models)

    val saved = captured.captured.single()
    assertThat(saved.checksumSha256).isEqualTo(existing.checksumSha256)
    assertThat(saved.signature).isEqualTo(existing.signature)
    assertThat(saved.downloadTaskId).isEqualTo(existing.downloadTaskId)
    assertThat(saved.installState).isEqualTo(existing.installState)
  }

  @Test
  fun `replaceCatalog retains cached entries not present in incoming list`() = runTest {
    val existingPrimary =
      fixture.existingEntity(
        modelId = "primary-model",
        checksum = "checksum-a",
        signature = null,
        downloadTaskId = null,
        installState = InstallState.INSTALLED,
      )
    val existingSecondary =
      fixture.existingEntity(
        modelId = "secondary-model",
        checksum = "checksum-b",
        signature = null,
        downloadTaskId = null,
        installState = InstallState.NOT_INSTALLED,
      )
    coEvery { fixture.modelPackageReadDao.getAll() } returns
      listOf(existingPrimary, existingSecondary)
    val incomingModels =
      listOf(
        ModelPackage(
          modelId = existingPrimary.modelId,
          displayName = "Primary Model",
          version = "3.0",
          providerType = ProviderType.CLOUD_API,
          deliveryType = DeliveryType.CLOUD_FALLBACK,
          minAppVersion = 10,
          sizeBytes = 1024,
          capabilities = setOf("text"),
          installState = InstallState.NOT_INSTALLED,
          downloadTaskId = null,
          manifestUrl = "https://example.com/primary",
          checksumSha256 = existingPrimary.checksumSha256,
          signature = existingPrimary.signature,
          createdAt = Instant.parse("2025-10-09T00:00:00Z"),
          updatedAt = Instant.parse("2025-10-11T00:00:00Z"),
        )
      )
    val captured = slot<List<ModelPackageEntity>>()
    coJustRun { fixture.modelPackageWriteDao.replaceCatalog(capture(captured)) }

    fixture.repository.replaceCatalog(incomingModels)

    val saved = captured.captured
    assertThat(saved.map(ModelPackageEntity::modelId))
      .containsAtLeast("primary-model", "secondary-model")
  }
}
