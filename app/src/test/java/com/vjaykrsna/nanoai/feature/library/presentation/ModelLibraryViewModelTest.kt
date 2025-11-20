package com.vjaykrsna.nanoai.feature.library.presentation

import app.cash.turbine.TurbineTestContext
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.library.DownloadModelUseCase
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelCompatibilityChecker
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceToModelPackageConverter
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.library.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibraryUiEvent
import com.vjaykrsna.nanoai.feature.library.presentation.state.ModelLibraryUiState
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHostTestHarness
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModelLibraryViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  private lateinit var modelCatalogUseCase: ModelCatalogUseCase
  private lateinit var refreshModelCatalogUseCase: RefreshModelCatalogUseCase
  private lateinit var downloadManager: DownloadManager
  private lateinit var downloadModelUseCase: DownloadModelUseCase
  private lateinit var converter: HuggingFaceToModelPackageConverter
  private lateinit var huggingFaceCatalogUseCase: HuggingFaceCatalogUseCase
  private lateinit var compatibilityChecker: HuggingFaceModelCompatibilityChecker

  private lateinit var allModelsFlow: MutableStateFlow<List<ModelPackage>>
  private lateinit var installedModelsFlow: MutableStateFlow<List<ModelPackage>>
  private lateinit var downloadTasksFlow: MutableStateFlow<List<DownloadTask>>
  private lateinit var downloadManagerLoading: MutableStateFlow<Boolean>
  private lateinit var downloadManagerErrors: MutableSharedFlow<LibraryError>

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(dispatcher)

    modelCatalogUseCase = mockk()
    refreshModelCatalogUseCase = mockk()
    downloadManager = mockk(relaxed = true)
    downloadModelUseCase = mockk()
    converter = mockk(relaxed = true)
    huggingFaceCatalogUseCase = mockk()
    compatibilityChecker = mockk()

    allModelsFlow = MutableStateFlow(emptyList())
    installedModelsFlow = MutableStateFlow(emptyList())
    downloadTasksFlow = MutableStateFlow(emptyList())
    downloadManagerLoading = MutableStateFlow(false)
    downloadManagerErrors = MutableSharedFlow(extraBufferCapacity = 1)

    every { modelCatalogUseCase.observeAllModels() } returns allModelsFlow
    every { modelCatalogUseCase.observeInstalledModels() } returns installedModelsFlow
    coEvery { modelCatalogUseCase.getAllModels() } returns NanoAIResult.success(emptyList())
    coEvery { modelCatalogUseCase.recordOfflineFallback(any(), any(), any()) } returns
      NanoAIResult.success(Unit)

    every { downloadManager.observeDownloadTasks() } returns downloadTasksFlow
    every { downloadManager.isLoading } returns downloadManagerLoading
    every { downloadManager.errorEvents } returns downloadManagerErrors
    justRun { downloadManager.downloadModel(any()) }
    justRun { downloadManager.pauseDownload(any()) }
    justRun { downloadManager.resumeDownload(any()) }
    justRun { downloadManager.cancelDownload(any()) }
    justRun { downloadManager.retryDownload(any()) }
    justRun { downloadManager.deleteModel(any()) }

    coEvery { downloadModelUseCase.downloadModel(any()) } returns
      NanoAIResult.success(UUID.randomUUID())
    coEvery { downloadModelUseCase.pauseDownload(any()) } returns NanoAIResult.success(Unit)
    coEvery { downloadModelUseCase.resumeDownload(any()) } returns NanoAIResult.success(Unit)
    coEvery { downloadModelUseCase.cancelDownload(any()) } returns NanoAIResult.success(Unit)
    coEvery { downloadModelUseCase.retryFailedDownload(any()) } returns NanoAIResult.success(Unit)
    every { downloadModelUseCase.getDownloadProgress(any()) } returns MutableStateFlow(0f)
    every { downloadModelUseCase.observeDownloadTasks() } returns MutableStateFlow(emptyList())
    coEvery { downloadModelUseCase.observeDownloadTask(any()) } returns
      MutableStateFlow<DownloadTask?>(null)

    every { converter.convertIfCompatible(any()) } returns DomainTestBuilders.buildModelPackage()

    coEvery { huggingFaceCatalogUseCase.listModels(any()) } returns
      NanoAIResult.success(emptyList())
    every { compatibilityChecker.checkCompatibility(any()) } returns ProviderType.MEDIA_PIPE

    coEvery { refreshModelCatalogUseCase.invoke() } returns NanoAIResult.success(Unit)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun refreshCatalogUpdatesSummaryAndSections() =
    runTest(dispatcher) {
      val installed =
        DomainTestBuilders.buildModelPackage(
          modelId = "installed",
          installState = InstallState.INSTALLED,
        )
      val available =
        DomainTestBuilders.buildModelPackage(
          modelId = "available",
          installState = InstallState.NOT_INSTALLED,
        )
      allModelsFlow.value = listOf(installed, available)
      installedModelsFlow.value = listOf(installed)

      val harness = createHarness()

      harness.viewModel.refreshCatalog()
      advanceUntilIdle()

      val state = harness.awaitState(predicate = { it.summary.total == 2 })
      assertThat(state.summary.installed).isEqualTo(1)
      assertThat(state.curatedSections.available.map { it.modelId })
        .containsExactly(available.modelId)
    }

  @Test
  fun updateSearchQueryUpdatesLocalFilters() =
    runTest(dispatcher) {
      val harness = createHarness()

      harness.viewModel.updateSearchQuery("vision")

      val state = harness.awaitState(predicate = { it.filters.localSearchQuery == "vision" })
      assertThat(state.filters.huggingFaceSearchQuery).isEmpty()
    }

  @Test
  fun updateSearchQueryUpdatesHuggingFaceFilters() =
    runTest(dispatcher) {
      val harness = createHarness()

      harness.viewModel.selectTab(ModelLibraryTab.HUGGING_FACE)
      advanceUntilIdle()
      harness.viewModel.updateSearchQuery("audio")

      val state = harness.awaitState(predicate = { it.filters.huggingFaceSearchQuery == "audio" })
      assertThat(state.filters.localSearchQuery).isEmpty()
    }

  @Test
  fun requestLocalModelImportEmitsEvent() =
    runTest(dispatcher) {
      val harness = createHarness()

      harness.testEvents {
        harness.viewModel.requestLocalModelImport()
        advanceUntilIdle()
        assertThat(awaitItem()).isEqualTo(ModelLibraryUiEvent.RequestLocalModelImport)
      }
    }

  @Test
  fun refreshCatalogFailureEmitsErrorEvent() =
    runTest(dispatcher) {
      coEvery { refreshModelCatalogUseCase.invoke() } returns
        NanoAIResult.recoverable(message = "offline", cause = IllegalStateException("offline"))
      val harness = createHarness()

      harness.testEvents {
        harness.viewModel.refreshCatalog()
        advanceUntilIdle()
        val event = awaitItem() as ModelLibraryUiEvent.ErrorRaised
        assertThat(event.envelope.userMessage).contains("Failed to refresh model catalog")
      }

      coVerify { modelCatalogUseCase.recordOfflineFallback(any(), any(), any()) }
    }

  @Test
  fun downloadModelDelegatesToUseCase() =
    runTest(dispatcher) {
      val harness = createHarness()

      harness.viewModel.downloadModel("model-123")
      advanceUntilIdle()

      coVerify { downloadModelUseCase.downloadModel("model-123") }
    }

  @Test
  fun deleteModelDelegatesToDownloadManager() =
    runTest(dispatcher) {
      val harness = createHarness()

      harness.viewModel.deleteModel("model-456")

      verify { downloadManager.deleteModel("model-456") }
    }

  private fun createHarness(): ModelLibraryHarness {
    val viewModel =
      ModelLibraryViewModel(
        modelCatalogUseCase = modelCatalogUseCase,
        refreshModelCatalogUseCase = refreshModelCatalogUseCase,
        downloadManager = downloadManager,
        downloadModelUseCase = downloadModelUseCase,
        hfToModelConverter = converter,
        huggingFaceCatalogUseCase = huggingFaceCatalogUseCase,
        compatibilityChecker = compatibilityChecker,
        mainDispatcher = dispatcher,
      )
    val harness =
      ViewModelStateHostTestHarness<ModelLibraryUiState, ModelLibraryUiEvent>(
        viewModel,
        defaultTimeout = 5.seconds,
      )
    return ModelLibraryHarness(viewModel, harness)
  }

  private data class ModelLibraryHarness(
    val viewModel: ModelLibraryViewModel,
    val stateHarness: ViewModelStateHostTestHarness<ModelLibraryUiState, ModelLibraryUiEvent>,
  ) {
    suspend fun awaitState(predicate: (ModelLibraryUiState) -> Boolean): ModelLibraryUiState =
      stateHarness.awaitState(predicate)

    suspend fun testEvents(block: suspend TurbineTestContext<ModelLibraryUiEvent>.() -> Unit) {
      stateHarness.testEvents(block = block)
    }
  }
}
