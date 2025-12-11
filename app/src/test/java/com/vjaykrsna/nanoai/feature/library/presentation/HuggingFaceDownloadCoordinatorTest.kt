package com.vjaykrsna.nanoai.feature.library.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.library.DownloadModelUseCase
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceToModelPackageConverter
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HuggingFaceDownloadCoordinatorTest {

  private lateinit var converter: HuggingFaceToModelPackageConverter
  private lateinit var modelCatalogUseCase: ModelCatalogUseCase
  private lateinit var downloadModelUseCase: DownloadModelUseCase
  private lateinit var errors: MutableList<LibraryError>
  private lateinit var coordinator: HuggingFaceDownloadCoordinator

  @BeforeEach
  fun setUp() {
    converter = mockk()
    modelCatalogUseCase = mockk()
    downloadModelUseCase = mockk()
    errors = mutableListOf()
    coordinator =
      HuggingFaceDownloadCoordinator(
        converter = converter,
        modelCatalogUseCase = modelCatalogUseCase,
        downloadModelUseCase = downloadModelUseCase,
        emitError = { error -> errors.add(error) },
      )
  }

  @Test
  fun `process emits error when model is not compatible`() = runTest {
    val summary = createSummary("model-123")
    coEvery { converter.convertIfCompatible(summary) } returns null

    coordinator.process(summary)

    assertThat(errors).hasSize(1)
    val error = errors.single() as LibraryError.DownloadFailed
    assertThat(error.modelId).isEqualTo("model-123")
    assertThat(error.message).contains("not compatible")
  }

  @Test
  fun `process emits error when model already exists`() = runTest {
    val summary = createSummary("model-123")
    val modelPackage = createModelPackage("model-123")
    coEvery { converter.convertIfCompatible(summary) } returns modelPackage
    coEvery { modelCatalogUseCase.getModel("model-123") } returns NanoAIResult.success(modelPackage)

    coordinator.process(summary)

    assertThat(errors).hasSize(1)
    val error = errors.single() as LibraryError.DownloadFailed
    assertThat(error.modelId).isEqualTo("model-123")
    assertThat(error.message).contains("already exists")
  }

  @Test
  fun `process emits error when adding to catalog fails`() = runTest {
    val summary = createSummary("model-123")
    val modelPackage = createModelPackage("model-123")
    coEvery { converter.convertIfCompatible(summary) } returns modelPackage
    coEvery { modelCatalogUseCase.getModel("model-123") } returns
      NanoAIResult.recoverable(message = "not found")
    coEvery { modelCatalogUseCase.upsertModel(modelPackage) } returns
      NanoAIResult.recoverable(message = "db error")

    coordinator.process(summary)

    assertThat(errors).hasSize(1)
    val error = errors.single() as LibraryError.DownloadFailed
    assertThat(error.modelId).isEqualTo("model-123")
    assertThat(error.message).contains("Failed to add model to catalog")
  }

  @Test
  fun `process emits error when download fails`() = runTest {
    val summary = createSummary("model-123")
    val modelPackage = createModelPackage("model-123")
    coEvery { converter.convertIfCompatible(summary) } returns modelPackage
    coEvery { modelCatalogUseCase.getModel("model-123") } returns
      NanoAIResult.recoverable(message = "not found")
    coEvery { modelCatalogUseCase.upsertModel(modelPackage) } returns NanoAIResult.success(Unit)
    coEvery { downloadModelUseCase.downloadModel("model-123") } returns
      NanoAIResult.recoverable(message = "download failed")

    coordinator.process(summary)

    assertThat(errors).hasSize(1)
    val error = errors.single() as LibraryError.DownloadFailed
    assertThat(error.modelId).isEqualTo("model-123")
    assertThat(error.message).contains("download failed")
  }

  @Test
  fun `process starts download on success`() = runTest {
    val summary = createSummary("model-123")
    val modelPackage = createModelPackage("model-123")
    val taskId = UUID.randomUUID()
    coEvery { converter.convertIfCompatible(summary) } returns modelPackage
    coEvery { modelCatalogUseCase.getModel("model-123") } returns
      NanoAIResult.recoverable(message = "not found")
    coEvery { modelCatalogUseCase.upsertModel(modelPackage) } returns NanoAIResult.success(Unit)
    coEvery { downloadModelUseCase.downloadModel("model-123") } returns NanoAIResult.success(taskId)

    coordinator.process(summary)

    assertThat(errors).isEmpty()
    coVerify { downloadModelUseCase.downloadModel("model-123") }
  }

  @Test
  fun `process handles IOException gracefully`() = runTest {
    val summary = createSummary("model-123")
    coEvery { converter.convertIfCompatible(summary) } throws IOException("Network unavailable")

    coordinator.process(summary)

    assertThat(errors).hasSize(1)
    val error = errors.single() as LibraryError.DownloadFailed
    assertThat(error.modelId).isEqualTo("model-123")
    assertThat(error.message).contains("Network error")
  }

  @Test
  fun `process handles IllegalStateException gracefully`() = runTest {
    val summary = createSummary("model-123")
    coEvery { converter.convertIfCompatible(summary) } throws IllegalStateException("Invalid state")

    coordinator.process(summary)

    assertThat(errors).hasSize(1)
    val error = errors.single() as LibraryError.DownloadFailed
    assertThat(error.modelId).isEqualTo("model-123")
    assertThat(error.message).contains("Failed to process")
  }

  @Test
  fun `process handles IllegalArgumentException gracefully`() = runTest {
    val summary = createSummary("model-123")
    coEvery { converter.convertIfCompatible(summary) } throws
      IllegalArgumentException("Invalid metadata")

    coordinator.process(summary)

    assertThat(errors).hasSize(1)
    val error = errors.single() as LibraryError.DownloadFailed
    assertThat(error.modelId).isEqualTo("model-123")
    assertThat(error.message).contains("Invalid Hugging Face model metadata")
  }

  private fun createSummary(modelId: String): HuggingFaceModelSummary {
    return HuggingFaceModelSummary(
      modelId = modelId,
      displayName = "Test Model",
      author = "test-author",
      downloads = 1000,
      likes = 100,
      pipelineTag = "text-generation",
      libraryName = null,
      tags = emptyList(),
      trendingScore = null,
      createdAt = null,
      lastModified = null,
      isPrivate = false,
    )
  }

  private fun createModelPackage(modelId: String): ModelPackage {
    val now = Clock.System.now()
    return ModelPackage(
      modelId = modelId,
      displayName = "Test Model",
      version = "1.0.0",
      providerType = ProviderType.MEDIA_PIPE,
      deliveryType = DeliveryType.LOCAL_ARCHIVE,
      minAppVersion = 1,
      sizeBytes = 1024L,
      capabilities = emptySet(),
      installState = InstallState.NOT_INSTALLED,
      manifestUrl = "https://example.com/manifest.json",
      createdAt = now,
      updatedAt = now,
    )
  }
}
