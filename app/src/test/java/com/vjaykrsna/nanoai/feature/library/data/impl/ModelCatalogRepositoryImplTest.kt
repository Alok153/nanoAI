package com.vjaykrsna.nanoai.feature.library.data.impl

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.daos.ChatThreadDao
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.toEntity
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import com.vjaykrsna.nanoai.model.catalog.DeliveryType
import com.vjaykrsna.nanoai.model.catalog.ModelPackageEntity
import com.vjaykrsna.nanoai.model.catalog.ModelPackageReadDao
import com.vjaykrsna.nanoai.model.catalog.ModelPackageWriteDao
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ModelCatalogRepositoryImplTest {

  @TempDir lateinit var tempDir: File

  private val readDao =
    mockk<ModelPackageReadDao>() {
      every { observeAll() } returns kotlinx.coroutines.flow.flowOf(emptyList())
      every { observeInstalled() } returns kotlinx.coroutines.flow.flowOf(emptyList())
    }
  private val writeDao = mockk<ModelPackageWriteDao>(relaxed = true)
  private val chatThreadDao = mockk<ChatThreadDao>(relaxed = true)
  private val context = mockk<Context>()
  private val repository by lazy {
    every { context.filesDir } returns tempDir
    ModelCatalogRepositoryImpl(
      readDao,
      writeDao,
      chatThreadDao,
      context,
      kotlinx.datetime.Clock.System
    )
  }

  @Test
  fun `replaceCatalog preserves integrity metadata when incoming values blank`() = runTest {
    val existing =
      existingEntity(
        modelId = "model-integrity",
        checksum = "existing-checksum",
        signature = "existing-signature",
        downloadTaskId = UUID.fromString("f8f0b23e-1111-4e75-9f6a-2fd2e3c4f5a6"),
        installState = InstallState.INSTALLED,
      )
    coEvery { readDao.getAll() } returns listOf(existing)
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
        ),
      )
    val captured = slot<List<ModelPackageEntity>>()
    coJustRun { writeDao.replaceCatalog(capture(captured)) }

    repository.replaceCatalog(models)

    val saved = captured.captured.single()
    assertThat(saved.checksumSha256).isEqualTo(existing.checksumSha256)
    assertThat(saved.signature).isEqualTo(existing.signature)
    assertThat(saved.downloadTaskId).isEqualTo(existing.downloadTaskId)
    assertThat(saved.installState).isEqualTo(existing.installState)
  }

  @Test
  fun `replaceCatalog retains cached entries not present in incoming list`() = runTest {
    val existingPrimary =
      existingEntity(
        "primary-model",
        checksum = "checksum-a",
        signature = null,
        downloadTaskId = null,
        installState = InstallState.INSTALLED
      )
    val existingSecondary =
      existingEntity(
        "secondary-model",
        checksum = "checksum-b",
        signature = null,
        downloadTaskId = null,
        installState = InstallState.NOT_INSTALLED
      )
    coEvery { readDao.getAll() } returns listOf(existingPrimary, existingSecondary)
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
        ),
      )
    val captured = slot<List<ModelPackageEntity>>()
    coJustRun { writeDao.replaceCatalog(capture(captured)) }

    repository.replaceCatalog(incomingModels)

    val saved = captured.captured
    assertThat(saved.map(ModelPackageEntity::modelId))
      .containsAtLeast("primary-model", "secondary-model")
  }

  @Test
  fun `deleteModelFiles removes nested model directories`() = runTest {
    val modelsDir = File(tempDir, "models").apply { mkdirs() }
    val modelDir = File(modelsDir, "model-delete").apply { mkdirs() }
    File(modelDir, "weights.bin").writeText("binary")
    File(modelDir, "metadata.json").writeText("meta")

    repository.deleteModelFiles("model-delete")

    assertThat(modelDir.exists()).isFalse()
    assertThat(modelsDir.list()?.toList()).doesNotContain("model-delete")
  }

  @Test
  fun `recordRefreshSuccess updates status observers`() = runTest {
    repository.recordRefreshSuccess(source = "RemoteSource", modelCount = 5)

    val status = repository.observeRefreshStatus().first()
    assertThat(status.lastSuccessSource).isEqualTo("RemoteSource")
    assertThat(status.lastSuccessCount).isEqualTo(5)
    assertThat(status.lastSuccessAt).isNotNull()
  }

  @Test
  fun `recordOfflineFallback captures cached metadata`() = runTest {
    repository.recordOfflineFallback(reason = "IOException", cachedCount = 2, message = "503")

    val status = repository.observeRefreshStatus().first()
    assertThat(status.lastFallbackReason).isEqualTo("IOException")
    assertThat(status.lastFallbackCachedCount).isEqualTo(2)
    assertThat(status.lastFallbackMessage).isEqualTo("503")
    assertThat(status.lastFallbackAt).isNotNull()
  }

  private fun existingEntity(
    modelId: String,
    checksum: String,
    signature: String?,
    downloadTaskId: UUID?,
    installState: InstallState,
  ): ModelPackageEntity =
    ModelPackage(
        modelId = modelId,
        displayName = "Existing $modelId",
        version = "1.0",
        providerType = ProviderType.CLOUD_API,
        deliveryType = DeliveryType.CLOUD_FALLBACK,
        minAppVersion = 5,
        sizeBytes = 128,
        capabilities = setOf("text"),
        installState = installState,
        downloadTaskId = downloadTaskId,
        manifestUrl = "https://example.com/$modelId",
        checksumSha256 = checksum,
        signature = signature,
        createdAt = Instant.parse("2025-10-08T00:00:00Z"),
        updatedAt = Instant.parse("2025-10-08T12:00:00Z"),
      )
      .toEntity()
}
