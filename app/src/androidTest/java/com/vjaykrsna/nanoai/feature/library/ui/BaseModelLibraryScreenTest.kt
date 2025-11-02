package com.vjaykrsna.nanoai.feature.library.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.domain.DownloadModelUseCase
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceModelCompatibilityChecker
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceToModelPackageConverter
import com.vjaykrsna.nanoai.feature.library.domain.ModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.domain.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.presentation.DownloadManager
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryViewModel
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import com.vjaykrsna.nanoai.shared.testing.FakeModelCatalogRepository
import com.vjaykrsna.nanoai.shared.testing.TestingTheme
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseModelLibraryScreenTest {
  @get:Rule
  val composeTestRule: ComposeContentTestRule = createAndroidComposeRule<ComponentActivity>()

  protected lateinit var catalogRepository: FakeModelCatalogRepository
  protected lateinit var modelCatalogUseCase: ModelCatalogUseCase
  protected lateinit var refreshUseCase: RefreshModelCatalogUseCase
  protected lateinit var huggingFaceCatalogUseCase: HuggingFaceCatalogUseCase
  protected lateinit var viewModel: ModelLibraryViewModel
  protected lateinit var downloadManager: DownloadManager
  protected lateinit var downloadModelUseCase: DownloadModelUseCase
  protected lateinit var hfToModelConverter: HuggingFaceToModelPackageConverter
  protected lateinit var compatibilityChecker: HuggingFaceModelCompatibilityChecker
  protected val testDispatcher = UnconfinedTestDispatcher()
  private val downloadLoadingFlow = MutableStateFlow(false)
  private val downloadTasksFlow = MutableStateFlow<List<DownloadTask>>(emptyList())
  private val downloadErrorFlow = MutableSharedFlow<LibraryError>(extraBufferCapacity = 1)

  @Before
  fun setUpBase() {
    catalogRepository = FakeModelCatalogRepository()
    modelCatalogUseCase = mockk(relaxed = true)
    refreshUseCase = mockk(relaxed = true)
    huggingFaceCatalogUseCase = mockk(relaxed = true)
    downloadManager = mockk(relaxed = true)
    downloadModelUseCase = mockk(relaxed = true)
    hfToModelConverter = mockk(relaxed = true)
    compatibilityChecker = mockk(relaxed = true)

    coEvery { refreshUseCase.invoke() } returns NanoAIResult.success(Unit)
    coEvery { huggingFaceCatalogUseCase.listModels(any()) } returns
      NanoAIResult.success(emptyList())
    every { hfToModelConverter.convertIfCompatible(any()) } returns null
    every { modelCatalogUseCase.observeAllModels() } returns catalogRepository.observeAllModels()
    every { modelCatalogUseCase.observeInstalledModels() } returns
      catalogRepository.observeInstalledModels()
    every { downloadManager.isLoading } returns downloadLoadingFlow
    every { downloadManager.observeDownloadTasks() } returns downloadTasksFlow
    every { downloadManager.errorEvents } returns downloadErrorFlow

    viewModel =
      ModelLibraryViewModel(
        modelCatalogUseCase = modelCatalogUseCase,
        refreshModelCatalogUseCase = refreshUseCase,
        downloadManager = downloadManager,
        downloadModelUseCase = downloadModelUseCase,
        hfToModelConverter = hfToModelConverter,
        huggingFaceCatalogUseCase = huggingFaceCatalogUseCase,
        compatibilityChecker = compatibilityChecker,
        dispatcher = testDispatcher,
      )
  }

  protected fun renderModelLibraryScreen() {
    composeTestRule.setContent { TestingTheme { ModelLibraryScreen(viewModel = viewModel) } }
    drainPendingCoroutines()
  }

  protected fun replaceCatalog(models: List<ModelPackage>) {
    runBlocking { catalogRepository.replaceCatalog(models) }
    drainPendingCoroutines()
  }

  protected fun setDownloadLoading(isLoading: Boolean) {
    downloadLoadingFlow.value = isLoading
    drainPendingCoroutines()
  }

  protected fun updateDownloadTasks(tasks: List<DownloadTask>) {
    downloadTasksFlow.value = tasks
    drainPendingCoroutines()
  }

  protected fun drainPendingCoroutines() {
    testDispatcher.scheduler.advanceUntilIdle()
    composeTestRule.waitForIdle()
  }
}
