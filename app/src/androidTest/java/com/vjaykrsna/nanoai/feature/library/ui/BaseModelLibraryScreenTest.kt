package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.vjaykrsna.nanoai.feature.library.domain.ListHuggingFaceModelsUseCase
import com.vjaykrsna.nanoai.feature.library.domain.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryViewModel
import com.vjaykrsna.nanoai.testing.FakeModelCatalogRepository
import com.vjaykrsna.nanoai.testing.FakeModelDownloadsAndExportUseCase
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule

abstract class BaseModelLibraryScreenTest {

  @get:Rule val composeTestRule: ComposeContentTestRule = createComposeRule()
  @get:Rule val testEnvironmentRule = TestEnvironmentRule()

  protected lateinit var catalogRepository: FakeModelCatalogRepository
  protected lateinit var downloadsUseCase: FakeModelDownloadsAndExportUseCase
  protected lateinit var refreshUseCase: RefreshModelCatalogUseCase
  protected lateinit var listHuggingFaceModelsUseCase: ListHuggingFaceModelsUseCase
  protected lateinit var viewModel: ModelLibraryViewModel

  @Before
  fun setUpBase() {
    catalogRepository = FakeModelCatalogRepository()
    downloadsUseCase = FakeModelDownloadsAndExportUseCase()
    refreshUseCase = mockk(relaxed = true)
    listHuggingFaceModelsUseCase = mockk(relaxed = true)

    coEvery { refreshUseCase.invoke() } returns Result.success(Unit)

    viewModel =
      ModelLibraryViewModel(
        downloadsUseCase,
        catalogRepository,
        refreshUseCase,
        listHuggingFaceModelsUseCase,
      )
  }

  protected fun renderModelLibraryScreen() {
    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }
    composeTestRule.waitForIdle()
  }
}
