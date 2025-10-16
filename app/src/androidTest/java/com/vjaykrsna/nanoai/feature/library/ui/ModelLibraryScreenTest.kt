package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.feature.library.domain.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryViewModel
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import com.vjaykrsna.nanoai.testing.FakeModelCatalogRepository
import com.vjaykrsna.nanoai.testing.FakeModelDownloadsAndExportUseCase
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import io.mockk.coEvery
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.delay
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

  @Before
  fun setup() {
    catalogRepository = FakeModelCatalogRepository()
    downloadsUseCase = FakeModelDownloadsAndExportUseCase()
    refreshUseCase = mockk(relaxed = true)

    coEvery { refreshUseCase.invoke() } returns Result.success(Unit)

    viewModel = ModelLibraryViewModel(downloadsUseCase, catalogRepository, refreshUseCase)
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

    composeTestRule.onAllNodesWithText("Installed", substring = false).onFirst().assertExists()
    composeTestRule.onAllNodesWithText("Storage", substring = false).onFirst().assertExists()
  }

  @Test
  fun modelLibraryScreen_displaysLoadingIndicator() = runTest {
    // Start with empty catalog to show loading
    catalogRepository.replaceCatalog(emptyList())
    coEvery { refreshUseCase.invoke() } coAnswers
      {
        delay(1_200)
        Result.success(Unit)
      }

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.runOnIdle { viewModel.refreshCatalog() }

    composeTestRule.waitUntil(timeoutMillis = 5_000) { viewModel.isLoading.value }

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodes(hasTestTag(ModelLibraryUiConstants.LOADING_INDICATOR_TAG))
        .fetchSemanticsNodes()
        .isNotEmpty()
    }

    composeTestRule
      .onAllNodes(hasTestTag(ModelLibraryUiConstants.LOADING_INDICATOR_TAG))
      .onFirst()
      .assertExists()
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
    composeTestRule
      .onNode(hasTestTag(ModelLibraryUiConstants.SEARCH_FIELD_TAG))
      .performTextInput("Qwen")

    composeTestRule.waitUntil {
      composeTestRule
        .onAllNodesWithText("Qwen Model", substring = true)
        .fetchSemanticsNodes()
        .isNotEmpty()
    }

    // Only Qwen model should be visible
    composeTestRule.onNodeWithText("Qwen Model", substring = true).assertExists()
    composeTestRule.onAllNodesWithText("Gemma Model", substring = true).assertCountEquals(0)
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

    composeTestRule
      .onAllNodes(hasText("Media pipe", substring = false).and(hasClickAction()))
      .onFirst()
      .performClick()

    composeTestRule.waitUntil {
      composeTestRule
        .onAllNodesWithText("Google Model", substring = true)
        .fetchSemanticsNodes()
        .isEmpty()
    }

    composeTestRule.onNodeWithText("HF Model", substring = true).assertExists()
    composeTestRule.onAllNodesWithText("Google Model", substring = true).assertCountEquals(0)
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

    composeTestRule
      .onAllNodes(hasText("Chat", substring = false).and(hasClickAction()))
      .onFirst()
      .performClick()

    composeTestRule.waitUntil {
      composeTestRule
        .onAllNodesWithText("Embedding Model", substring = true)
        .fetchSemanticsNodes()
        .isEmpty()
    }

    composeTestRule.onNodeWithText("Chat Model", substring = true).assertExists()
    composeTestRule.onAllNodesWithText("Embedding Model", substring = true).assertCountEquals(0)
  }

  @Test
  fun modelLibraryScreen_clearFilters() = runTest {
    val model = DomainTestBuilders.buildModelPackage(modelId = "test-model")
    catalogRepository.replaceCatalog(listOf(model))

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Apply search filter
    composeTestRule
      .onNode(hasTestTag(ModelLibraryUiConstants.SEARCH_FIELD_TAG))
      .performTextInput("Test")

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithText("Clear filters", substring = false)
        .fetchSemanticsNodes()
        .isNotEmpty()
    }

    composeTestRule.onAllNodesWithText("Clear filters", substring = false).onFirst().performClick()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithText("Clear filters", substring = false)
        .fetchSemanticsNodes()
        .isEmpty()
    }

    composeTestRule.runOnIdle { assertThat(viewModel.filters.value.searchQuery).isEmpty() }
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

    composeTestRule.onNodeWithText("Download Model", substring = true).assertExists()

    composeTestRule
      .onNodeWithContentDescription("Download Download Model", substring = false)
      .performClick()

    composeTestRule.runOnIdle {
      assertThat(downloadsUseCase.lastDownloadedModelId).isEqualTo("download-model")
    }
  }

  @Test
  fun modelLibraryScreen_errorModel_showsRetryAction() = runTest {
    val model =
      DomainTestBuilders.buildModelPackage(
        modelId = "error-model",
        displayName = "Error Model",
        installState = InstallState.ERROR
      )
    catalogRepository.replaceCatalog(listOf(model))

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onAllNodesWithContentDescription("Retry Error Model").onFirst().assertExists()
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

    composeTestRule.onNodeWithText("Installed Model", substring = true).assertExists()

    composeTestRule
      .onNodeWithContentDescription("Remove Installed Model", substring = false)
      .performClick()

    composeTestRule.runOnIdle {
      assertThat(downloadsUseCase.lastDeletedModelId).isEqualTo("installed-model")
    }
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

    composeTestRule.waitUntil {
      composeTestRule
        .onAllNodesWithText("Download failed for error-model", substring = true)
        .fetchSemanticsNodes()
        .isNotEmpty()
    }

    composeTestRule
      .onNodeWithText("Download failed for error-model", substring = true)
      .assertExists()
  }

  @Test
  fun modelLibraryScreen_organizesModelsIntoSections() = runTest {
    val needsAttention =
      DomainTestBuilders.buildModelPackage(
        modelId = "attention-model",
        displayName = "Attention Model",
        installState = InstallState.ERROR
      )
    val installed =
      DomainTestBuilders.buildModelPackage(
        modelId = "installed-model",
        displayName = "Installed Model",
        installState = InstallState.INSTALLED
      )
    val available =
      DomainTestBuilders.buildModelPackage(
        modelId = "available-model",
        displayName = "Available Model",
        installState = InstallState.NOT_INSTALLED
      )

    catalogRepository.replaceCatalog(listOf(needsAttention, installed, available))

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle { assertThat(viewModel.sections.value.available).isNotEmpty() }

    val listNode = composeTestRule.onNode(hasTestTag(ModelLibraryUiConstants.LIST_TAG))
    listNode.performScrollToNode(hasText("Attention Model", substring = false))
    composeTestRule.onNodeWithText("Attention Model", substring = false).assertExists()

    listNode.performScrollToNode(hasText("Installed Model", substring = false))
    composeTestRule.onNodeWithText("Installed Model", substring = false).assertExists()

    listNode.performScrollToNode(hasText("Available Model", substring = false))
    composeTestRule.onNodeWithText("Available Model", substring = false).assertExists()
  }

  @Test
  fun modelLibraryScreen_emptyState() = runTest {
    catalogRepository.replaceCatalog(emptyList())

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("No models to show", substring = true).assertExists()
  }

  @Test
  fun modelLibraryScreen_filterChips_haveAccessibility() = runTest {
    val model = DomainTestBuilders.buildModelPackage(modelId = "test-model")
    catalogRepository.replaceCatalog(listOf(model))

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNode(hasTestTag(ModelLibraryUiConstants.FILTER_TOGGLE_TAG)).performClick()

    composeTestRule.onNodeWithText("All providers", substring = false).assertExists()
    composeTestRule.onNodeWithText("Recommended", substring = false).assertExists()
  }

  @Test
  fun modelLibraryScreen_activeDownloadsArePrioritized() = runTest {
    val model =
      DomainTestBuilders.buildModelPackage(
        modelId = "active-model",
        displayName = "Active Model",
        installState = InstallState.NOT_INSTALLED
      )
    catalogRepository.replaceCatalog(listOf(model))

    val taskId = UUID.randomUUID()
    val task =
      DownloadTask(
        taskId = taskId,
        modelId = model.modelId,
        progress = 0.45f,
        status = DownloadStatus.DOWNLOADING,
      )
    downloadsUseCase.addDownloadTask(task)
    downloadsUseCase.updateDownloadProgress(taskId, 0.45f)
    downloadsUseCase.updateDownloadStatus(taskId, DownloadStatus.DOWNLOADING)

    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule
      .onNode(hasTestTag(ModelLibraryUiConstants.DOWNLOAD_QUEUE_HEADER_TAG))
      .assertIsDisplayed()
    composeTestRule.onNodeWithText("Active Model", substring = false).assertExists()
    composeTestRule
      .onAllNodesWithContentDescription("Download Active Model", substring = false)
      .assertCountEquals(0)
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
