package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.shared.model.catalog.DeliveryType
import com.vjaykrsna.nanoai.testing.assertRecoverableError
import com.vjaykrsna.nanoai.testing.assertSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ModelCatalogUseCaseTest {
  private lateinit var useCase: ModelCatalogUseCase
  private lateinit var modelCatalogRepository: ModelCatalogRepository

  private val modelId = "test-model"

  @Before
  fun setup() {
    modelCatalogRepository = mockk(relaxed = true)

    useCase = ModelCatalogUseCase(modelCatalogRepository)
  }

  @Test
  fun `getAllModels returns success with model list`() = runTest {
    val models = listOf(createModelPackage("model1"), createModelPackage("model2"))
    coEvery { modelCatalogRepository.getAllModels() } returns models

    val result = useCase.getAllModels()

    val returnedModels = result.assertSuccess()
    assert(returnedModels == models)
  }

  @Test
  fun `getAllModels returns recoverable error when repository fails`() = runTest {
    val exception = RuntimeException("Repository error")
    coEvery { modelCatalogRepository.getAllModels() } throws exception

    val result = useCase.getAllModels()

    result.assertRecoverableError()
  }

  @Test
  fun `getModel returns success with found model`() = runTest {
    val model = createModelPackage(modelId)
    coEvery { modelCatalogRepository.getModel(modelId) } returns model

    val result = useCase.getModel(modelId)

    val returnedModel = result.assertSuccess()
    assert(returnedModel == model)
  }

  @Test
  fun `getModel returns success with null when model not found`() = runTest {
    coEvery { modelCatalogRepository.getModel(modelId) } returns null

    val result = useCase.getModel(modelId)

    val returnedModel = result.assertSuccess()
    assert(returnedModel == null)
  }

  @Test
  fun `getModel returns recoverable error when repository fails`() = runTest {
    val exception = RuntimeException("Repository error")
    coEvery { modelCatalogRepository.getModel(modelId) } throws exception

    val result = useCase.getModel(modelId)

    result.assertRecoverableError()
  }

  @Test
  fun `upsertModel succeeds and calls repository`() = runTest {
    val model = createModelPackage(modelId)

    val result = useCase.upsertModel(model)

    result.assertSuccess()
    coVerify { modelCatalogRepository.upsertModel(model) }
  }

  @Test
  fun `upsertModel returns recoverable error when repository fails`() = runTest {
    val model = createModelPackage(modelId)
    val exception = RuntimeException("Repository error")
    coEvery { modelCatalogRepository.upsertModel(model) } throws exception

    val result = useCase.upsertModel(model)

    result.assertRecoverableError()
  }

  @Test
  fun `recordOfflineFallback succeeds and calls repository`() = runTest {
    val reason = "network_unavailable"
    val cachedCount = 5
    val message = "Offline mode activated"

    val result = useCase.recordOfflineFallback(reason, cachedCount, message)

    result.assertSuccess()
    coVerify { modelCatalogRepository.recordOfflineFallback(reason, cachedCount, message) }
  }

  @Test
  fun `recordOfflineFallback returns recoverable error when repository fails`() = runTest {
    val reason = "network_unavailable"
    val cachedCount = 5
    val exception = RuntimeException("Repository error")
    coEvery { modelCatalogRepository.recordOfflineFallback(any(), any(), any()) } throws exception

    val result = useCase.recordOfflineFallback(reason, cachedCount)

    result.assertRecoverableError()
  }

  private fun createModelPackage(id: String) =
    ModelPackage(
      modelId = id,
      displayName = "Test Model",
      version = "1.0",
      providerType = ProviderType.MEDIA_PIPE,
      deliveryType = DeliveryType.LOCAL_ARCHIVE,
      minAppVersion = 1,
      sizeBytes = 1000,
      capabilities = setOf("text-gen"),
      installState = InstallState.NOT_INSTALLED,
      downloadTaskId = null,
      manifestUrl = "http://example.com/manifest",
      checksumSha256 = null,
      signature = null,
      createdAt = kotlinx.datetime.Clock.System.now(),
      updatedAt = kotlinx.datetime.Clock.System.now(),
    )
}
