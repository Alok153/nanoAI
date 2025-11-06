package com.vjaykrsna.nanoai.core.data.library.impl

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.core.domain.model.toEntity
import io.mockk.coEvery
import io.mockk.coVerify
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ModelCatalogRepositoryWriteOperationsTest {

  @TempDir lateinit var tempDir: File

  private lateinit var fixture: ModelCatalogRepositoryTestFixture

  @BeforeEach
  fun setUp() {
    fixture = createModelCatalogRepositoryFixture(tempDir)
  }

  @Test
  fun `updateModelState delegates to dao with clock timestamp`() = runTest {
    val expectedTime = Instant.parse("2025-12-01T00:00:00Z")
    fixture.clock.advanceTo(expectedTime)

    fixture.repository.updateModelState("model-state", InstallState.DOWNLOADING)

    coVerify {
      fixture.modelPackageWriteDao.updateInstallState(
        "model-state",
        InstallState.DOWNLOADING,
        expectedTime,
      )
    }
  }

  @Test
  fun `updateInstallState alias routes through updateModelState`() = runTest {
    val expectedTime = Instant.parse("2025-12-04T00:00:00Z")
    fixture.clock.advanceTo(expectedTime)

    fixture.repository.updateInstallState("model-alias", InstallState.INSTALLED)

    coVerify {
      fixture.modelPackageWriteDao.updateInstallState(
        "model-alias",
        InstallState.INSTALLED,
        expectedTime,
      )
    }
  }

  @Test
  fun `upsertModel writes entity`() = runTest {
    val model =
      ModelPackage(
        modelId = "upsert",
        displayName = "Upsert",
        version = "1.0",
        providerType = ProviderType.CLOUD_API,
        deliveryType = DeliveryType.CLOUD_FALLBACK,
        minAppVersion = 1,
        sizeBytes = 10,
        capabilities = setOf("text"),
        installState = InstallState.NOT_INSTALLED,
        downloadTaskId = null,
        manifestUrl = "https://example.com/upsert",
        checksumSha256 = "checksum",
        signature = null,
        createdAt = Instant.parse("2025-10-01T00:00:00Z"),
        updatedAt = Instant.parse("2025-10-01T00:00:00Z"),
      )

    fixture.repository.upsertModel(model)

    coVerify { fixture.modelPackageWriteDao.insert(model.toEntity()) }
  }

  @Test
  fun `updateDownloadTaskId persists uuid as string`() = runTest {
    val taskId = UUID.fromString("b64f3d87-5bdc-4c49-b7b3-2f2d7862889f")
    val expectedTime = Instant.parse("2025-12-02T00:00:00Z")
    fixture.clock.advanceTo(expectedTime)

    fixture.repository.updateDownloadTaskId("model-task", taskId)

    coVerify {
      fixture.modelPackageWriteDao.updateDownloadTaskId(
        "model-task",
        taskId.toString(),
        expectedTime,
      )
    }
  }

  @Test
  fun `updateChecksum delegates to integrity metadata update`() = runTest {
    val expectedTime = Instant.parse("2025-12-03T00:00:00Z")
    fixture.clock.advanceTo(expectedTime)

    fixture.repository.updateChecksum("model-checksum", "new-sum")

    coVerify {
      fixture.modelPackageWriteDao.updateIntegrityMetadata(
        "model-checksum",
        "new-sum",
        null,
        expectedTime,
      )
    }
  }

  @Test
  fun `isModelActiveInSession reflects dao count`() = runTest {
    coEvery { fixture.chatThreadDao.countActiveByModel("active") } returns 2
    coEvery { fixture.chatThreadDao.countActiveByModel("idle") } returns 0

    assertThat(fixture.repository.isModelActiveInSession("active")).isTrue()
    assertThat(fixture.repository.isModelActiveInSession("idle")).isFalse()
  }

  @Test
  fun `deleteModelFiles removes nested model directories`() = runTest {
    val modelsDir = File(tempDir, "models").apply { mkdirs() }
    val modelDir = File(modelsDir, "model-delete").apply { mkdirs() }
    File(modelDir, "weights.bin").writeText("binary")
    File(modelDir, "metadata.json").writeText("meta")

    fixture.repository.deleteModelFiles("model-delete")

    assertThat(modelDir.exists()).isFalse()
    assertThat(modelsDir.list()?.toList()).doesNotContain("model-delete")
  }

  @Test
  fun `recordRefreshSuccess updates status observers`() = runTest {
    fixture.repository.recordRefreshSuccess(source = "RemoteSource", modelCount = 5)

    val status = fixture.repository.observeRefreshStatus().first()
    assertThat(status.lastSuccessSource).isEqualTo("RemoteSource")
    assertThat(status.lastSuccessCount).isEqualTo(5)
    assertThat(status.lastSuccessAt).isNotNull()
  }

  @Test
  fun `recordOfflineFallback captures cached metadata`() = runTest {
    fixture.repository.recordOfflineFallback(
      reason = "IOException",
      cachedCount = 2,
      message = "503",
    )

    val status = fixture.repository.observeRefreshStatus().first()
    assertThat(status.lastFallbackReason).isEqualTo("IOException")
    assertThat(status.lastFallbackCachedCount).isEqualTo(2)
    assertThat(status.lastFallbackMessage).isEqualTo("503")
    assertThat(status.lastFallbackAt).isNotNull()
  }
}
