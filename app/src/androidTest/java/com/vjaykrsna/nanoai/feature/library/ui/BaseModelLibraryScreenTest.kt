package com.vjaykrsna.nanoai.feature.library.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.vjaykrsna.nanoai.core.device.ConnectivityObserver
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelCompatibilityChecker
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceToModelPackageConverter
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.library.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import com.vjaykrsna.nanoai.feature.library.domain.QueueModelDownloadUseCase
import com.vjaykrsna.nanoai.feature.library.presentation.DownloadUiCoordinator
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryViewModel
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import com.vjaykrsna.nanoai.shared.testing.FakeModelCatalogRepository
import com.vjaykrsna.nanoai.shared.testing.TestingTheme
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
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
  protected lateinit var downloadCoordinator: DownloadUiCoordinator
  protected lateinit var queueModelDownloadUseCase: QueueModelDownloadUseCase
  protected lateinit var hfToModelConverter: HuggingFaceToModelPackageConverter
  protected lateinit var compatibilityChecker: HuggingFaceModelCompatibilityChecker
  protected lateinit var connectivityObserver: ConnectivityObserver
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
    downloadCoordinator = mockk(relaxed = true)
    queueModelDownloadUseCase = mockk(relaxed = true)
    hfToModelConverter = mockk(relaxed = true)
    compatibilityChecker = mockk(relaxed = true)
    connectivityObserver = mockk(relaxed = true)

    coEvery { refreshUseCase.invoke() } returns NanoAIResult.success(Unit)
    coEvery { queueModelDownloadUseCase.invoke(any()) } returns
      NanoAIResult.success(UUID.randomUUID())
    coEvery { huggingFaceCatalogUseCase.listModels(any()) } returns
      NanoAIResult.success(emptyList())
    every { hfToModelConverter.convertIfCompatible(any()) } returns null
    every { modelCatalogUseCase.observeAllModels() } returns catalogRepository.observeAllModels()
    every { modelCatalogUseCase.observeInstalledModels() } returns
      catalogRepository.observeInstalledModels()
    every { downloadCoordinator.isLoading } returns downloadLoadingFlow
    every { downloadCoordinator.observeDownloadTasks() } returns downloadTasksFlow
    every { downloadCoordinator.errorEvents } returns downloadErrorFlow
    every { connectivityObserver.status } returns MutableStateFlow(ConnectivityStatus.ONLINE)

    viewModel =
      ModelLibraryViewModel(
        modelCatalogUseCase = modelCatalogUseCase,
        refreshModelCatalogUseCase = refreshUseCase,
        downloadCoordinator = downloadCoordinator,
        queueModelDownloadUseCase = queueModelDownloadUseCase,
        hfToModelConverter = hfToModelConverter,
        huggingFaceCatalogUseCase = huggingFaceCatalogUseCase,
        compatibilityChecker = compatibilityChecker,
        connectivityObserver = connectivityObserver,
        mainDispatcher = testDispatcher,
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
