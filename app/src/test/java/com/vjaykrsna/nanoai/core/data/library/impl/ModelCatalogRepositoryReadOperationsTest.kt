package com.vjaykrsna.nanoai.core.data.library.impl

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import io.mockk.coEvery
import io.mockk.every
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ModelCatalogRepositoryReadOperationsTest {

  @TempDir lateinit var tempDir: File

  private lateinit var fixture: ModelCatalogRepositoryTestFixture

  @BeforeEach
  fun setUp() {
    fixture = createModelCatalogRepositoryFixture(tempDir)
  }

  @Test
  fun `getAllModels returns mapped domain models`() = runTest {
    val entityA =
      fixture.existingEntity(
        modelId = "model-a",
        checksum = "sum-a",
        signature = null,
        downloadTaskId = null,
        installState = InstallState.NOT_INSTALLED,
      )
    val entityB =
      fixture.existingEntity(
        modelId = "model-b",
        checksum = "sum-b",
        signature = null,
        downloadTaskId = null,
        installState = InstallState.INSTALLED,
      )
    coEvery { fixture.modelPackageReadDao.getAll() } returns listOf(entityA, entityB)

    val models = fixture.repository.getAllModels()

    assertThat(models.map { it.modelId }).containsExactly("model-a", "model-b")
    assertThat(models.first { it.modelId == "model-a" }.checksumSha256).isEqualTo("sum-a")
  }

  @Test
  fun `getModel returns null when dao missing`() = runTest {
    coEvery { fixture.modelPackageReadDao.getById("missing") } returns null

    val model = fixture.repository.getModel("missing")

    assertThat(model).isNull()
  }

  @Test
  fun `getModelById emits mapped values`() = runTest {
    val entity =
      fixture.existingEntity(
        modelId = "observed",
        checksum = "sum",
        signature = null,
        downloadTaskId = null,
        installState = InstallState.INSTALLED,
      )
    every { fixture.modelPackageReadDao.observeById("observed") } returns flowOf(entity)

    val emitted = fixture.repository.getModelById("observed").first()

    assertThat(emitted?.modelId).isEqualTo("observed")
    assertThat(emitted?.installState).isEqualTo(InstallState.INSTALLED)
  }

  @Test
  fun `getInstalledModels filters by install state`() = runTest {
    val entityInstalled =
      fixture.existingEntity(
        modelId = "installed",
        checksum = "sum",
        signature = null,
        downloadTaskId = null,
        installState = InstallState.INSTALLED,
      )
    coEvery { fixture.modelPackageReadDao.getByInstallState(InstallState.INSTALLED) } returns
      listOf(entityInstalled)

    val models = fixture.repository.getInstalledModels()

    assertThat(models).hasSize(1)
    assertThat(models.first().installState).isEqualTo(InstallState.INSTALLED)
  }

  @Test
  fun `observeAllModels exposes dao flow`() = runTest {
    val entity =
      fixture.existingEntity(
        modelId = "flow",
        checksum = "sum",
        signature = null,
        downloadTaskId = null,
        installState = InstallState.NOT_INSTALLED,
      )

    val testFixture =
      createModelCatalogRepositoryFixture(tempDir) {
        every { observeAll() } returns flowOf(listOf(entity))
      }

    val models = testFixture.repository.observeAllModels().first()

    assertThat(models).hasSize(1)
    assertThat(models.first().modelId).isEqualTo("flow")
  }

  @Test
  fun `observeInstalledModels exposes dao flow`() = runTest {
    val entity =
      fixture.existingEntity(
        modelId = "installed-flow",
        checksum = "sum",
        signature = null,
        downloadTaskId = null,
        installState = InstallState.INSTALLED,
      )

    val testFixture =
      createModelCatalogRepositoryFixture(tempDir) {
        every { observeInstalled() } returns flowOf(listOf(entity))
      }

    val models = testFixture.repository.observeInstalledModels().first()

    assertThat(models).hasSize(1)
    assertThat(models.first().installState).isEqualTo(InstallState.INSTALLED)
  }
}
