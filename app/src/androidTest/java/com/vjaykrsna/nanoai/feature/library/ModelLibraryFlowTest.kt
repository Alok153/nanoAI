package com.vjaykrsna.nanoai.feature.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Compose UI instrumentation test for Model Library feature.
 * Tests download flow, pause/resume, queued downloads, and failure recovery.
 *
 * TDD: This test is written BEFORE the UI is implemented.
 * Expected to FAIL with compilation errors until:
 * - ModelLibraryScreen composable is created
 * - ModelLibraryViewModel is defined
 * - UI components (download buttons, progress indicators) exist
 */
@RunWith(AndroidJUnit4::class)
class ModelLibraryFlowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: ModelLibraryViewModel
    private lateinit var allModelsFlow: MutableStateFlow<List<ModelPackage>>
    private lateinit var installedModelsFlow: MutableStateFlow<List<ModelPackage>>
    private lateinit var filteredModelsFlow: MutableStateFlow<List<ModelPackage>>
    private lateinit var queuedDownloadsFlow: MutableStateFlow<List<DownloadTask>>
    private lateinit var isLoadingFlow: MutableStateFlow<Boolean>

    @Before
    fun setup() {
        viewModel = mockk(relaxed = true)
        allModelsFlow = MutableStateFlow(emptyList())
        installedModelsFlow = MutableStateFlow(emptyList())
        filteredModelsFlow = MutableStateFlow(emptyList())
        queuedDownloadsFlow = MutableStateFlow(emptyList())
        isLoadingFlow = MutableStateFlow(false)

        coEvery { viewModel.allModels } returns allModelsFlow
        coEvery { viewModel.installedModels } returns installedModelsFlow
        coEvery { viewModel.filteredModels } returns filteredModelsFlow
        coEvery { viewModel.queuedDownloads } returns queuedDownloadsFlow
        coEvery { viewModel.isLoading } returns isLoadingFlow
    }

    @Test
    fun modelLibraryScreen_shouldDisplayAvailableModels() {
        // Arrange
        val availableModels =
            listOf(
                createModelPackage(
                    modelId = "gemini-2.0-flash-lite",
                    displayName = "Gemini 2.0 Flash Lite",
                    installState = InstallState.NOT_INSTALLED,
                ),
                createModelPackage(
                    modelId = "phi-3-mini-4k",
                    displayName = "Phi-3 Mini 4K",
                    installState = InstallState.NOT_INSTALLED,
                ),
            )

        allModelsFlow.value = availableModels
        filteredModelsFlow.value = availableModels

        // Act
        composeTestRule.setContent {
            ModelLibraryScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithText("Gemini 2.0 Flash Lite").assertIsDisplayed()
        composeTestRule.onNodeWithText("Phi-3 Mini 4K").assertIsDisplayed()
    }

    @Test
    fun downloadButton_whenClicked_shouldStartDownload() {
        // Arrange
        val model =
            createModelPackage(
                modelId = "gemini-2.0-flash-lite",
                displayName = "Gemini 2.0 Flash Lite",
                installState = InstallState.NOT_INSTALLED,
            )

        uiState.value = uiState.value.copy(availableModels = listOf(model))

        composeTestRule.setContent {
            ModelLibraryScreen(viewModel = viewModel)
        }

        // Act
        composeTestRule
            .onNodeWithContentDescription("Download Gemini 2.0 Flash Lite")
            .performClick()

        // Assert
        coVerify { viewModel.downloadModel("gemini-2.0-flash-lite") }
    }

    @Test
    fun downloadProgress_shouldDisplayProgressBar() {
        // Arrange
        val downloadingModel =
            createModelPackage(
                modelId = "gemini-2.0-flash-lite",
                displayName = "Gemini 2.0 Flash Lite",
                installState = InstallState.DOWNLOADING,
            )

        val downloadTask =
            DownloadTask(
                taskId = UUID.randomUUID(),
                modelId = "gemini-2.0-flash-lite",
                progress = 0.65f,
                status = DownloadStatus.DOWNLOADING,
                bytesDownloaded = 975000000L,
                startedAt = Instant.now(),
                finishedAt = null,
                errorMessage = null,
            )

        uiState.value =
            uiState.value.copy(
                availableModels = listOf(downloadingModel),
                downloadingModels = listOf(downloadTask),
            )

        // Act
        composeTestRule.setContent {
            ModelLibraryScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithText("65%").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Downloading Gemini 2.0 Flash Lite")
            .assertIsDisplayed()
    }

    @Test
    fun pauseButton_whenClicked_shouldPauseDownload() {
        // Arrange
        val downloadTask =
            DownloadTask(
                taskId = UUID.randomUUID(),
                modelId = "gemini-2.0-flash-lite",
                progress = 0.45f,
                status = DownloadStatus.DOWNLOADING,
                bytesDownloaded = 675000000L,
                startedAt = Instant.now(),
                finishedAt = null,
                errorMessage = null,
            )

        uiState.value = uiState.value.copy(downloadingModels = listOf(downloadTask))

        composeTestRule.setContent {
            ModelLibraryScreen(viewModel = viewModel)
        }

        // Act
        composeTestRule
            .onNodeWithContentDescription("Pause download")
            .performClick()

        // Assert
        coVerify { viewModel.pauseDownload(downloadTask.taskId) }
    }

    @Test
    fun resumeButton_whenClicked_shouldResumeDownload() {
        // Arrange
        val pausedTask =
            DownloadTask(
                taskId = UUID.randomUUID(),
                modelId = "gemini-2.0-flash-lite",
                progress = 0.45f,
                status = DownloadStatus.PAUSED,
                bytesDownloaded = 675000000L,
                startedAt = Instant.now(),
                finishedAt = null,
                errorMessage = null,
            )

        uiState.value = uiState.value.copy(downloadingModels = listOf(pausedTask))

        composeTestRule.setContent {
            ModelLibraryScreen(viewModel = viewModel)
        }

        // Act
        composeTestRule
            .onNodeWithContentDescription("Resume download")
            .performClick()

        // Assert
        coVerify { viewModel.resumeDownload(pausedTask.taskId) }
    }

    @Test
    fun queuedDownloads_shouldDisplayQueuePosition() {
        // Arrange
        val queuedDownloads =
            listOf(
                createDownloadTask(
                    modelId = "model-1",
                    status = DownloadStatus.QUEUED,
                ),
                createDownloadTask(
                    modelId = "model-2",
                    status = DownloadStatus.QUEUED,
                ),
                createDownloadTask(
                    modelId = "model-3",
                    status = DownloadStatus.QUEUED,
                ),
            )

        uiState.value = uiState.value.copy(queuedDownloads = queuedDownloads)

        // Act
        composeTestRule.setContent {
            ModelLibraryScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithText("Queue position: 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Queue position: 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Queue position: 3").assertIsDisplayed()
    }

    @Test
    fun failedDownload_shouldDisplayRetryButton() {
        // Arrange
        val failedTask =
            DownloadTask(
                taskId = UUID.randomUUID(),
                modelId = "gemini-2.0-flash-lite",
                progress = 0.78f,
                status = DownloadStatus.FAILED,
                bytesDownloaded = 1170000000L,
                startedAt = Instant.now(),
                finishedAt = Instant.now(),
                errorMessage = "Network connection lost",
            )

        uiState.value =
            uiState.value.copy(
                downloadingModels = listOf(failedTask),
                errorMessage = "Download failed: Network connection lost",
            )

        // Act
        composeTestRule.setContent {
            ModelLibraryScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithText("Network connection lost").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Retry download")
            .assertIsDisplayed()
    }

    @Test
    fun retryButton_whenClicked_shouldRetryFailedDownload() {
        // Arrange
        val failedTask =
            DownloadTask(
                taskId = UUID.randomUUID(),
                modelId = "gemini-2.0-flash-lite",
                progress = 0.60f,
                status = DownloadStatus.FAILED,
                bytesDownloaded = 900000000L,
                startedAt = Instant.now(),
                finishedAt = Instant.now(),
                errorMessage = "Checksum mismatch",
            )

        uiState.value = uiState.value.copy(downloadingModels = listOf(failedTask))

        composeTestRule.setContent {
            ModelLibraryScreen(viewModel = viewModel)
        }

        // Act
        composeTestRule
            .onNodeWithContentDescription("Retry download")
            .performClick()

        // Assert
        coVerify { viewModel.retryDownload(failedTask.taskId) }
    }

    @Test
    fun installedModel_shouldDisplayDeleteButton() {
        // Arrange
        val installedModel =
            createModelPackage(
                modelId = "gemini-2.0-flash-lite",
                displayName = "Gemini 2.0 Flash Lite",
                installState = InstallState.INSTALLED,
            )

        uiState.value = uiState.value.copy(installedModels = listOf(installedModel))

        // Act
        composeTestRule.setContent {
            ModelLibraryScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule
            .onNodeWithContentDescription("Delete Gemini 2.0 Flash Lite")
            .assertIsDisplayed()
    }

    @Test
    fun deleteButton_whenClicked_shouldShowConfirmationDialog() {
        // Arrange
        val installedModel =
            createModelPackage(
                modelId = "gemini-2.0-flash-lite",
                displayName = "Gemini 2.0 Flash Lite",
                installState = InstallState.INSTALLED,
            )

        uiState.value = uiState.value.copy(installedModels = listOf(installedModel))

        composeTestRule.setContent {
            ModelLibraryScreen(viewModel = viewModel)
        }

        // Act
        composeTestRule
            .onNodeWithContentDescription("Delete Gemini 2.0 Flash Lite")
            .performClick()

        // Assert
        composeTestRule.onNodeWithText("Delete Model?").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("This will remove the model from your device.")
            .assertIsDisplayed()
    }

    @Test
    fun modelSize_shouldDisplayInGigabytes() {
        // Arrange
        val model =
            createModelPackage(
                modelId = "gemini-2.0-flash-lite",
                displayName = "Gemini 2.0 Flash Lite",
                sizeBytes = 1500000000L, // 1.5 GB
            )

        uiState.value = uiState.value.copy(availableModels = listOf(model))

        // Act
        composeTestRule.setContent {
            ModelLibraryScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithText("1.4 GB").assertIsDisplayed() // 1.5 GB ≈ 1.4 GiB
    }

    @Test
    fun modelCapabilities_shouldDisplayChips() {
        // Arrange
        val model =
            createModelPackage(
                modelId = "gemini-2.0-flash-lite",
                displayName = "Gemini 2.0 Flash Lite",
                capabilities = setOf("TEXT_GEN", "CODE_GEN", "AUDIO_IN"),
            )

        uiState.value = uiState.value.copy(availableModels = listOf(model))

        // Act
        composeTestRule.setContent {
            ModelLibraryScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithText("Text Generation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Code Generation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Audio Input").assertIsDisplayed()
    }

    @Test
    fun searchBar_shouldFilterModels() {
        // Arrange
        val models =
            listOf(
                createModelPackage(modelId = "gemini-2.0-flash-lite", displayName = "Gemini 2.0 Flash Lite"),
                createModelPackage(modelId = "phi-3-mini-4k", displayName = "Phi-3 Mini 4K"),
                createModelPackage(modelId = "llama-3-8b", displayName = "Llama 3 8B"),
            )

        uiState.value = uiState.value.copy(availableModels = models)

        composeTestRule.setContent {
            ModelLibraryScreen(viewModel = viewModel)
        }

        // Act
        composeTestRule
            .onNodeWithContentDescription("Search models")
            .performClick()
        // Type "gemini" in search field (would need text input simulation)

        // Assert: ViewModel should filter
        coVerify { viewModel.filterModels("gemini") }
    }

    // Helper functions
    private fun createModelPackage(
        modelId: String,
        displayName: String,
        installState: InstallState = InstallState.NOT_INSTALLED,
        sizeBytes: Long = 1500000000L,
        capabilities: Set<String> = setOf("TEXT_GEN"),
    ) = ModelPackage(
        modelId = modelId,
        displayName = displayName,
        version = "1.0.0",
        providerType = ProviderType.MEDIA_PIPE,
        sizeBytes = sizeBytes,
        capabilities = capabilities,
        installState = installState,
        downloadTaskId = null,
        checksum = "abc123def456",
        updatedAt = Clock.System.now(),
    )

    private fun createDownloadTask(
        modelId: String,
        status: DownloadStatus,
    ) = DownloadTask(
        taskId = UUID.randomUUID(),
        modelId = modelId,
        progress = 0.0f,
        status = status,
        bytesDownloaded = 0L,
        startedAt = Clock.System.now(),
        finishedAt = null,
        errorMessage = null,
    )
}
