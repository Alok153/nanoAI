package com.vjaykrsna.nanoai.feature.library.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.library.DownloadModelUseCase
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceToModelPackageConverter
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.library.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryUiEvent
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModelLibraryEventDelegateTest {
  private lateinit var modelCatalogUseCase: ModelCatalogUseCase
  private lateinit var refreshModelCatalogUseCase: RefreshModelCatalogUseCase
  private lateinit var downloadModelUseCase: DownloadModelUseCase
  private lateinit var hfToModelConverter: HuggingFaceToModelPackageConverter
  private lateinit var huggingFaceViewModel: HuggingFaceLibraryViewModel
  private lateinit var downloadManager: DownloadManager
  private lateinit var stateStore: ModelLibraryStateStore

  private lateinit var downloadRequests: MutableSharedFlow<HuggingFaceModelSummary>
  private lateinit var downloadManagerErrors: MutableSharedFlow<LibraryError>
  private lateinit var huggingFaceErrors: MutableSharedFlow<LibraryError>

  @BeforeEach
  fun setup() {
    modelCatalogUseCase = mockk(relaxed = true)
    refreshModelCatalogUseCase = mockk()
    downloadModelUseCase = mockk()
    hfToModelConverter = mockk()
    huggingFaceViewModel = mockk()
    downloadManager = mockk()
    stateStore = mockk(relaxed = true)

    downloadRequests = MutableSharedFlow()
    downloadManagerErrors = MutableSharedFlow()
    huggingFaceErrors = MutableSharedFlow()

    every { stateStore.beginRefresh() } returns true
    justRun { stateStore.completeRefresh() }

    every { huggingFaceViewModel.downloadRequests } returns downloadRequests
    every { huggingFaceViewModel.errorEvents } returns huggingFaceErrors
    justRun { huggingFaceViewModel.requestDownload(any()) }

    every { downloadManager.errorEvents } returns downloadManagerErrors
    justRun { downloadManager.deleteModel(any()) }

    coEvery { refreshModelCatalogUseCase() } returns NanoAIResult.success(Unit)
    coEvery { modelCatalogUseCase.upsertModel(any()) } returns NanoAIResult.success(Unit)
    coEvery { modelCatalogUseCase.getModel(any()) } returns NanoAIResult.success(null)
    coEvery { modelCatalogUseCase.getAllModels() } returns NanoAIResult.success(emptyList())
    coEvery { modelCatalogUseCase.recordOfflineFallback(any(), any(), any()) } returns
      NanoAIResult.success(Unit)
    coEvery { downloadModelUseCase.downloadModel(any()) } returns
      NanoAIResult.success(UUID.randomUUID())
  }

  @Test
  fun refreshCatalogInvokesUseCaseAndCompletes() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val scope = TestScope(dispatcher)
    val delegate = createDelegate(scope, dispatcher)

    delegate.refreshCatalog()
    scope.advanceUntilIdle()

    coVerify { refreshModelCatalogUseCase.invoke() }
    verify { stateStore.completeRefresh() }
  }

  @Test
  fun refreshCatalogSkipsWhenAlreadyRefreshing() = runTest {
    every { stateStore.beginRefresh() } returns false
    val dispatcher = StandardTestDispatcher(testScheduler)
    val scope = TestScope(dispatcher)
    val delegate = createDelegate(scope, dispatcher)

    delegate.refreshCatalog()
    scope.advanceUntilIdle()

    coVerify(exactly = 0) { refreshModelCatalogUseCase.invoke() }
  }

  @Test
  fun downloadModelEmitsErrorWhenUseCaseFails() = runTest {
    coEvery { downloadModelUseCase.downloadModel(any()) } returns
      NanoAIResult.recoverable(message = "failed")

    val dispatcher = StandardTestDispatcher(testScheduler)
    val scope = TestScope(dispatcher)
    val delegate = createDelegate(scope, dispatcher)

    delegate.errorEvents.test {
      delegate.downloadActions.downloadModel("model-1")
      scope.advanceUntilIdle()

      val error = awaitItem()
      assertThat(error).isInstanceOf(LibraryError.DownloadFailed::class.java)
    }
  }

  @Test
  fun requestLocalModelImportEmitsUiEvent() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val scope = TestScope(dispatcher)
    val delegate = createDelegate(scope, dispatcher)

    delegate.uiEvents.test {
      delegate.requestLocalModelImport()
      scope.advanceUntilIdle()
      assertThat(awaitItem()).isEqualTo(LibraryUiEvent.RequestLocalModelImport)
    }
  }

  @Test
  fun startPropagatesDownloadManagerErrors() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val scope = TestScope(dispatcher)
    val delegate = createDelegate(scope, dispatcher)

    delegate.start()
    scope.advanceUntilIdle()

    delegate.errorEvents.test {
      val error = LibraryError.UnexpectedError("boom")
      downloadManagerErrors.emit(error)
      scope.advanceUntilIdle()
      assertThat(awaitItem()).isEqualTo(error)
    }
  }

  @Test
  fun startProcessesHuggingFaceDownloadWhenCompatible() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val scope = TestScope(dispatcher)
    val delegate = createDelegate(scope, dispatcher)

    val summary = huggingFaceSummary(modelId = "hf-model")
    val modelPackage = DomainTestBuilders.buildModelPackage(modelId = "hf-model")
    coEvery { hfToModelConverter.convertIfCompatible(summary) } returns modelPackage

    delegate.start()
    scope.advanceUntilIdle()

    delegate.errorEvents.test {
      downloadRequests.emit(summary)
      scope.advanceUntilIdle()
      expectNoEvents()
    }

    coVerify { modelCatalogUseCase.upsertModel(modelPackage) }
    coVerify { downloadModelUseCase.downloadModel("hf-model") }
  }

  @Test
  fun startEmitsErrorWhenHuggingFaceModelAlreadyExists() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val scope = TestScope(dispatcher)
    val delegate = createDelegate(scope, dispatcher)

    val summary = huggingFaceSummary(modelId = "existing")
    val existingPackage = DomainTestBuilders.buildModelPackage(modelId = "existing")
    coEvery { hfToModelConverter.convertIfCompatible(summary) } returns existingPackage
    coEvery { modelCatalogUseCase.getModel("existing") } returns
      NanoAIResult.success(existingPackage)

    delegate.start()
    scope.advanceUntilIdle()

    delegate.errorEvents.test {
      downloadRequests.emit(summary)
      scope.advanceUntilIdle()

      val error = awaitItem()
      assertThat(error).isInstanceOf(LibraryError.DownloadFailed::class.java)
    }

    coVerify(exactly = 0) { downloadModelUseCase.downloadModel(any()) }
  }

  private fun createDelegate(
    scope: TestScope,
    dispatcher: TestDispatcher,
  ): ModelLibraryEventDelegate =
    ModelLibraryEventDelegate(
      modelCatalogUseCase = modelCatalogUseCase,
      refreshModelCatalogUseCase = refreshModelCatalogUseCase,
      downloadModelUseCase = downloadModelUseCase,
      hfToModelConverter = hfToModelConverter,
      huggingFaceLibraryViewModel = huggingFaceViewModel,
      downloadManager = downloadManager,
      stateStore = stateStore,
      dispatcher = dispatcher,
      scope = scope,
    )

  private fun huggingFaceSummary(modelId: String): HuggingFaceModelSummary =
    HuggingFaceModelSummary(
      modelId = modelId,
      displayName = "Model",
      author = "author",
      pipelineTag = "text-generation",
      libraryName = "transformers",
      tags = emptyList(),
      likes = 1,
      downloads = 10,
      license = null,
      languages = emptyList(),
      baseModel = null,
      datasets = emptyList(),
      architectures = emptyList(),
      modelType = null,
      baseModelRelations = emptyList(),
      hasGatedAccess = false,
      isDisabled = false,
      totalSizeBytes = null,
      summary = null,
      description = null,
      trendingScore = null,
      createdAt = null,
      lastModified = null,
      isPrivate = false,
    )
}
