package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.data.DownloadManager
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.data.catalog.DeliveryType
import com.vjaykrsna.nanoai.testing.assertRecoverableError
import com.vjaykrsna.nanoai.testing.assertSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class VerifyDownloadUseCaseTest {
  private lateinit var useCase: VerifyDownloadUseCase
  private lateinit var modelCatalogRepository: ModelCatalogRepository
  private lateinit var downloadManager: DownloadManager

  private val modelId = "test-model"
  private val checksum = "abc123"

  @Before
  fun setup() {
    modelCatalogRepository = mockk(relaxed = true)
    downloadManager = mockk(relaxed = true)

    useCase =
      VerifyDownloadUseCase(
        modelCatalogRepository = modelCatalogRepository,
        downloadManager = downloadManager,
      )
  }

  @Test
  fun `invoke returns true when checksums match`() = runTest {
    val model = createModelPackage(checksum)
    coEvery { modelCatalogRepository.getModelById(modelId) } returns flowOf(model)
    coEvery { downloadManager.getDownloadedChecksum(modelId) } returns checksum

    val result = useCase.invoke(modelId)

    val matches = result.assertSuccess()
    assert(matches)
    coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.INSTALLED) }
  }

  @Test
  fun `invoke returns false when checksums do not match`() = runTest {
    val model = createModelPackage(checksum)
    coEvery { modelCatalogRepository.getModelById(modelId) } returns flowOf(model)
    coEvery { downloadManager.getDownloadedChecksum(modelId) } returns "different-checksum"

    val result = useCase.invoke(modelId)

    val matches = result.assertSuccess()
    assert(!matches)
    coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR) }
  }

  @Test
  fun `invoke returns error when model not found`() = runTest {
    coEvery { modelCatalogRepository.getModelById(modelId) } returns flowOf(null)

    val result = useCase.invoke(modelId)

    result.assertRecoverableError()
  }

  @Test
  fun `invoke returns error when checksum not available in model`() = runTest {
    val model = createModelPackage(null)
    coEvery { modelCatalogRepository.getModelById(modelId) } returns flowOf(model)

    val result = useCase.invoke(modelId)

    result.assertRecoverableError()
    coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR) }
  }

  @Test
  fun `invoke returns error when downloaded checksum not available`() = runTest {
    val model = createModelPackage(checksum)
    coEvery { modelCatalogRepository.getModelById(modelId) } returns flowOf(model)
    coEvery { downloadManager.getDownloadedChecksum(modelId) } returns null

    val result = useCase.invoke(modelId)

    result.assertRecoverableError()
    coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR) }
  }

  private fun createModelPackage(checksum: String?) =
    ModelPackage(
      modelId = modelId,
      displayName = "Test Model",
      version = "1.0",
      providerType = ProviderType.MEDIA_PIPE,
      deliveryType = DeliveryType.LOCAL_ARCHIVE,
      minAppVersion = 1,
      sizeBytes = 1000,
      capabilities = setOf("text-gen"),
      installState = InstallState.DOWNLOADING,
      downloadTaskId = null,
      manifestUrl = "http://example.com/manifest",
      checksumSha256 = checksum,
      signature = null,
      createdAt = kotlinx.datetime.Clock.System.now(),
      updatedAt = kotlinx.datetime.Clock.System.now(),
    )
}
