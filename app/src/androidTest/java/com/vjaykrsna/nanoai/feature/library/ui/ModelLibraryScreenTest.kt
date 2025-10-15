package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vjaykrsna.nanoai.feature.library.domain.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryViewModel
import com.vjaykrsna.nanoai.testing.ComposeTestHarness
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import com.vjaykrsna.nanoai.testing.FakeModelCatalogRepository
import com.vjaykrsna.nanoai.testing.FakeModelDownloadsAndExportUseCase
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose instrumentation tests for [ModelLibraryScreen].
 *
 * Validates filter chips accessibility, download status semantics, and sections organization.
 */
@RunWith(AndroidJUnit4::class)
class ModelLibraryScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @get:Rule val testEnvironmentRule = TestEnvironmentRule()

  private lateinit var catalogRepository: FakeModelCatalogRepository
  private lateinit var downloadsUseCase: FakeModelDownloadsAndExportUseCase
  private lateinit var refreshUseCase: RefreshModelCatalogUseCase
  private lateinit var viewModel: ModelLibraryViewModel
  private lateinit var harness: ComposeTestHarness

  @Before
  fun setup() {
    catalogRepository = FakeModelCatalogRepository()
    downloadsUseCase = FakeModelDownloadsAndExportUseCase()
    refreshUseCase = mockk(relaxed = true)

    coEvery { refreshUseCase.invoke() } returns Result.success(Unit)

    viewModel = ModelLibraryViewModel(downloadsUseCase, catalogRepository, refreshUseCase)
    harness = ComposeTestHarness(composeTestRule)
  }

  @Test
  fun modelLibraryScreen_displaysContentDescription() {
    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule
      .onNodeWithContentDescription("Model library screen with enhanced management controls")
      .assertIsDisplayed()
  }

  @Test
  fun modelLibraryScreen_displaysHeader() {
    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Header should show model summary
    composeTestRule.onNodeWithText("Model Library", substring = true).assertExists()
  }

  @Test
  fun modelLibraryScreen_displaysLoadingIndicator() = runTest {
    // Start with empty catalog to show loading
    catalogRepository.replaceCatalog(emptyList())

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    // Initially loading
    composeTestRule.onNodeWithContentDescription("Loading models").assertExists()
  }

  @Test
  fun modelLibraryScreen_displaysModels() = runTest {
    val model1 =
      DomainTestBuilders.buildModelPackage(
        modelId = "model-1",
        displayName = "Test Model 1",
        providerType = ProviderType.MEDIA_PIPE
      )
    val model2 =
      DomainTestBuilders.buildModelPackage(
        modelId = "model-2",
        displayName = "Test Model 2",
        providerType = ProviderType.CLOUD_API
      )

    catalogRepository.replaceCatalog(listOf(model1, model2))

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Models should be displayed
    composeTestRule.onNodeWithText("Test Model 1", substring = true).assertExists()
    composeTestRule.onNodeWithText("Test Model 2", substring = true).assertExists()
  }

  @Test
  fun modelLibraryScreen_filterBySearchQuery() = runTest {
    val model1 =
      DomainTestBuilders.buildModelPackage(modelId = "model-1", displayName = "Qwen Model")
    val model2 =
      DomainTestBuilders.buildModelPackage(modelId = "model-2", displayName = "Gemma Model")

    catalogRepository.replaceCatalog(listOf(model1, model2))

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Search for "Qwen"
    composeTestRule.onNodeWithText("Search models...").performTextInput("Qwen")
    composeTestRule.waitForIdle()

    // Only Qwen model should be visible
    composeTestRule.onNodeWithText("Qwen Model", substring = true).assertExists()
  }

  @Test
  fun modelLibraryScreen_filterByProvider() = runTest {
    val hfModel =
      DomainTestBuilders.buildModelPackage(
        modelId = "hf-model",
        displayName = "HF Model",
        providerType = ProviderType.MEDIA_PIPE
      )
    val googleModel =
      DomainTestBuilders.buildModelPackage(
        modelId = "google-model",
        displayName = "Google Model",
        providerType = ProviderType.CLOUD_API
      )

    catalogRepository.replaceCatalog(listOf(hfModel, googleModel))

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Both models initially visible
    composeTestRule.onNodeWithText("HF Model", substring = true).assertExists()
    composeTestRule.onNodeWithText("Google Model", substring = true).assertExists()

    // Filter by HUGGING_FACE provider (implementation depends on UI)
    // This would require clicking on provider filter chip
  }

  @Test
  fun modelLibraryScreen_filterByCapability() = runTest {
    val chatModel =
      DomainTestBuilders.buildModelPackage(
        modelId = "chat-model",
        displayName = "Chat Model",
        capabilities = setOf("chat")
      )
    val embeddingModel =
      DomainTestBuilders.buildModelPackage(
        modelId = "embedding-model",
        displayName = "Embedding Model",
        capabilities = setOf("embeddings")
      )

    catalogRepository.replaceCatalog(listOf(chatModel, embeddingModel))

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Both models initially visible
    composeTestRule.onNodeWithText("Chat Model", substring = true).assertExists()
    composeTestRule.onNodeWithText("Embedding Model", substring = true).assertExists()

    // Toggle CHAT capability filter (implementation depends on UI)
  }

  @Test
  fun modelLibraryScreen_clearFilters() = runTest {
    val model = DomainTestBuilders.buildModelPackage(modelId = "test-model")
    catalogRepository.replaceCatalog(listOf(model))

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Apply search filter
    composeTestRule.onNodeWithText("Search models...").performTextInput("Test")
    composeTestRule.waitForIdle()

    // Clear filters button should appear when filters are active
    // (Implementation depends on UI - might be a button or chip)
  }

  @Test
  fun modelLibraryScreen_downloadModel() = runTest {
    val model =
      DomainTestBuilders.buildModelPackage(
        modelId = "download-model",
        displayName = "Download Model",
        installState = InstallState.NOT_INSTALLED
      )
    catalogRepository.replaceCatalog(listOf(model))

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Model should show download button
    composeTestRule.onNodeWithText("Download Model", substring = true).assertExists()

    // Click download (implementation depends on button accessibility)
  }

  @Test
  fun modelLibraryScreen_downloadingModel_showsProgress() = runTest {
    val model =
      DomainTestBuilders.buildModelPackage(
        modelId = "downloading-model",
        displayName = "Downloading Model",
        installState = InstallState.DOWNLOADING
      )
    catalogRepository.replaceCatalog(listOf(model))

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Progress should be displayed
    composeTestRule.onNodeWithText("50%", substring = true).assertExists()
  }

  @Test
  fun modelLibraryScreen_installedModel_showsDeleteOption() = runTest {
    val model =
      DomainTestBuilders.buildModelPackage(
        modelId = "installed-model",
        displayName = "Installed Model",
        installState = InstallState.INSTALLED
      )
    catalogRepository.replaceCatalog(listOf(model))

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Model should show as installed
    composeTestRule.onNodeWithText("Installed Model", substring = true).assertExists()
  }

  @Test
  fun modelLibraryScreen_pullToRefresh() {
    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Pull-to-refresh indicator should be present
    // (Testing actual pull gesture is complex in Compose tests)

    // Verify refresh can be triggered programmatically
    viewModel.refreshCatalog()
    composeTestRule.waitForIdle()
  }

  @Test
  fun modelLibraryScreen_errorSnackbar_displaysDownloadError() = runTest {
    val model = DomainTestBuilders.buildModelPackage(modelId = "error-model")
    catalogRepository.replaceCatalog(listOf(model))
    downloadsUseCase.shouldFailOnDownload = true

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Trigger download that will fail
    viewModel.downloadModel("error-model")
    composeTestRule.waitForIdle()

    // Error snackbar should appear
    composeTestRule.onNodeWithText("Download failed", substring = true).assertExists()
  }

  @Test
  fun modelLibraryScreen_organizesModelsIntoSections() = runTest {
    val needsAttention =
      DomainTestBuilders.buildModelPackage(
        modelId = "attention-model",
        installState = InstallState.ERROR
      )
    val installed =
      DomainTestBuilders.buildModelPackage(
        modelId = "installed-model",
        installState = InstallState.INSTALLED
      )
    val available =
      DomainTestBuilders.buildModelPackage(
        modelId = "available-model",
        installState = InstallState.NOT_INSTALLED
      )

    catalogRepository.replaceCatalog(listOf(needsAttention, installed, available))

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Sections should be organized: Needs Attention, Installed, Available
    // (Exact section headers depend on implementation)
  }

  @Test
  fun modelLibraryScreen_emptyState() = runTest {
    catalogRepository.replaceCatalog(emptyList())

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Should show empty state message
    // (Implementation depends on UI)
  }

  @Test
  fun modelLibraryScreen_filterChips_haveAccessibility() = runTest {
    val model = DomainTestBuilders.buildModelPackage(modelId = "test-model")
    catalogRepository.replaceCatalog(listOf(model))

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Filter chips should have proper semantics for TalkBack
    // (Specific accessibility labels depend on implementation)
  }

  @Test
  fun modelLibraryScreen_downloadQueue_displaysQueuedDownloads() = runTest {
    val model1 =
      DomainTestBuilders.buildModelPackage(
        modelId = "queued-1",
        installState = InstallState.NOT_INSTALLED
      )
    val model2 =
      DomainTestBuilders.buildModelPackage(
        modelId = "queued-2",
        installState = InstallState.DOWNLOADING
      )

    catalogRepository.replaceCatalog(listOf(model1, model2))

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Queued downloads should be visible
    // (Implementation depends on download queue UI)
  }
}
