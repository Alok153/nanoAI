package com.vjaykrsna.nanoai.core.domain.library

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import com.vjaykrsna.nanoai.testing.assertIsSuccess
import com.vjaykrsna.nanoai.testing.assertRecoverableError
import com.vjaykrsna.nanoai.testing.assertSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class ModelDownloadsAndExportUseCaseCatalogTest :
  ModelDownloadsAndExportUseCaseTestBase() {

  @Test
  fun `verifyDownloadChecksum succeeds when checksums match`() = runTest {
    val modelId = "gemini-2.0-flash-lite"
    val checksum = "abc123"
    every { modelCatalogRepository.getModelById(modelId) } returns
      flowOf(
        DomainTestBuilders.buildModelPackage(
          modelId = modelId,
          checksumSha256 = checksum,
          installState = InstallState.DOWNLOADING,
        )
      )
    coEvery { downloadManager.getDownloadedChecksum(modelId) } returns checksum

    val result = useCase.verifyDownloadChecksum(modelId)

    val value = result.assertSuccess()
    assertThat(value).isTrue()
    coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.INSTALLED) }
  }

  @Test
  fun `verifyDownloadChecksum marks error on mismatch`() = runTest {
    val modelId = "gemini-2.0-flash-lite"
    every { modelCatalogRepository.getModelById(modelId) } returns
      flowOf(
        DomainTestBuilders.buildModelPackage(
          modelId = modelId,
          checksumSha256 = "expected",
          installState = InstallState.DOWNLOADING,
        )
      )
    coEvery { downloadManager.getDownloadedChecksum(modelId) } returns "different"

    val result = useCase.verifyDownloadChecksum(modelId)

    val value = result.assertSuccess()
    assertThat(value).isFalse()
    coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR) }
  }

  @Test
  fun `verifyDownloadChecksum returns recoverable when repository fails`() = runTest {
    every { modelCatalogRepository.getModelById("error") } throws IllegalStateException("db")

    val result = useCase.verifyDownloadChecksum("error")

    result.assertRecoverableError()
  }

  @Test
  fun `verifyDownloadChecksum returns recoverable when checksum missing`() = runTest {
    val modelId = "gemini-2.0-flash-lite"
    every { modelCatalogRepository.getModelById(modelId) } returns
      flowOf(
        DomainTestBuilders.buildModelPackage(
          modelId = modelId,
          checksumSha256 = "expected",
          installState = InstallState.DOWNLOADING,
        )
      )
    coEvery { downloadManager.getDownloadedChecksum(modelId) } returns null

    val result = useCase.verifyDownloadChecksum(modelId)

    result.assertRecoverableError()
    coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR) }
  }

  @Test
  fun `deleteModel rejects when active`() = runTest {
    val modelId = "active"
    coEvery { modelCatalogRepository.isModelActiveInSession(modelId) } returns true

    val result = useCase.deleteModel(modelId)

    result.assertRecoverableError()
    coVerify(exactly = 0) { modelCatalogRepository.deleteModelFiles(modelId) }
  }

  @Test
  fun `deleteModel removes files when idle`() = runTest {
    val modelId = "inactive"
    coEvery { modelCatalogRepository.isModelActiveInSession(modelId) } returns false

    val result = useCase.deleteModel(modelId)

    result.assertIsSuccess()
    coVerify { modelCatalogRepository.deleteModelFiles(modelId) }
    coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.NOT_INSTALLED) }
    coVerify { modelCatalogRepository.updateDownloadTaskId(modelId, null) }
  }
}
