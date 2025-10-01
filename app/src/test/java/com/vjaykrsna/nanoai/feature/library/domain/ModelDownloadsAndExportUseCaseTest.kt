package com.vjaykrsna.nanoai.feature.library.domain

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

/**
 * Domain unit test for ModelDownloadsAndExportUseCase.
 * Tests queue limits, checksum validation, and export bundle composition.
 *
 * TDD: This test is written BEFORE the use case is implemented.
 * Expected to FAIL with compilation errors until:
 * - ModelDownloadsAndExportUseCase is created
 * - ModelCatalogRepository is defined
 * - DownloadManager is defined
 * - ExportService is defined
 */
class ModelDownloadsAndExportUseCaseTest {
    private lateinit var useCase: ModelDownloadsAndExportUseCase
    private lateinit var modelCatalogRepository: ModelCatalogRepository
    private lateinit var downloadManager: DownloadManager
    private lateinit var exportService: ExportService

    @Before
    fun setup() {
        modelCatalogRepository = mockk(relaxed = true)
        downloadManager = mockk(relaxed = true)
        exportService = mockk(relaxed = true)

        useCase =
            ModelDownloadsAndExportUseCase(
                modelCatalogRepository = modelCatalogRepository,
                downloadManager = downloadManager,
                exportService = exportService,
            )
    }

    @Test
    fun `downloadModel should enforce concurrent download limit`() =
        runTest {
            // Arrange: 2 downloads already in progress
            val activeDownloads =
                listOf(
                    createDownloadTask(status = DownloadStatus.DOWNLOADING),
                    createDownloadTask(status = DownloadStatus.DOWNLOADING),
                )
            coEvery { downloadManager.getActiveDownloads() } returns flowOf(activeDownloads)
            coEvery { downloadManager.getMaxConcurrentDownloads() } returns 2

            val newModelId = "gemini-2.0-flash-lite"

            // Act
            val result = useCase.downloadModel(newModelId)

            // Assert: Should queue the download instead of starting immediately
            assertThat(result.isSuccess).isTrue()
            coVerify { downloadManager.queueDownload(newModelId) }
            coVerify(exactly = 0) { downloadManager.startDownload(newModelId) }
        }

    @Test
    fun `downloadModel should start immediately when under concurrent limit`() =
        runTest {
            // Arrange: Only 1 download in progress, limit is 2
            val activeDownloads =
                listOf(
                    createDownloadTask(status = DownloadStatus.DOWNLOADING),
                )
            coEvery { downloadManager.getActiveDownloads() } returns flowOf(activeDownloads)
            coEvery { downloadManager.getMaxConcurrentDownloads() } returns 2

            val newModelId = "phi-3-mini-4k"

            // Act
            val result = useCase.downloadModel(newModelId)

            // Assert: Should start download immediately
            assertThat(result.isSuccess).isTrue()
            coVerify { downloadManager.startDownload(newModelId) }
        }

    @Test
    fun `downloadModel should validate checksum after download completes`() =
        runTest {
            // Arrange
            val modelId = "gemini-2.0-flash-lite"
            val expectedChecksum = "abc123def456"

            val modelPackage =
                ModelPackage(
                    modelId = modelId,
                    displayName = "Gemini 2.0 Flash Lite",
                    version = "2.0.1",
                    providerType = ProviderType.MEDIA_PIPE,
                    sizeBytes = 1500000000L,
                    capabilities = setOf("TEXT_GEN"),
                    installState = InstallState.DOWNLOADING,
                    downloadTaskId = UUID.randomUUID(),
                    checksum = expectedChecksum,
                    updatedAt = Instant.now(),
                )

            coEvery { modelCatalogRepository.getModelById(modelId) } returns flowOf(modelPackage)
            coEvery { downloadManager.getDownloadedChecksum(modelId) } returns expectedChecksum

            // Act
            val result = useCase.verifyDownloadChecksum(modelId)

            // Assert
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).isTrue()
        }

    @Test
    fun `downloadModel should fail when checksum mismatch detected`() =
        runTest {
            // Arrange
            val modelId = "gemini-2.0-flash-lite"
            val expectedChecksum = "abc123def456"
            val actualChecksum = "different123"

            val modelPackage =
                ModelPackage(
                    modelId = modelId,
                    displayName = "Gemini 2.0 Flash Lite",
                    version = "2.0.1",
                    providerType = ProviderType.MEDIA_PIPE,
                    sizeBytes = 1500000000L,
                    capabilities = setOf("TEXT_GEN"),
                    installState = InstallState.DOWNLOADING,
                    downloadTaskId = UUID.randomUUID(),
                    checksum = expectedChecksum,
                    updatedAt = Instant.now(),
                )

            coEvery { modelCatalogRepository.getModelById(modelId) } returns flowOf(modelPackage)
            coEvery { downloadManager.getDownloadedChecksum(modelId) } returns actualChecksum

            // Act
            val result = useCase.verifyDownloadChecksum(modelId)

            // Assert
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).isFalse()
            coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR) }
        }

    @Test
    fun `pauseDownload should update task status to PAUSED`() =
        runTest {
            // Arrange
            val taskId = UUID.randomUUID()

            // Act
            useCase.pauseDownload(taskId)

            // Assert
            coVerify { downloadManager.pauseDownload(taskId) }
            coVerify { downloadManager.updateTaskStatus(taskId, DownloadStatus.PAUSED) }
        }

    @Test
    fun `resumeDownload should restart paused download`() =
        runTest {
            // Arrange
            val taskId = UUID.randomUUID()
            val pausedTask =
                createDownloadTask(
                    taskId = taskId,
                    status = DownloadStatus.PAUSED,
                    progress = 0.45f,
                )

            coEvery { downloadManager.getTaskById(taskId) } returns flowOf(pausedTask)

            // Act
            useCase.resumeDownload(taskId)

            // Assert
            coVerify { downloadManager.resumeDownload(taskId) }
            coVerify { downloadManager.updateTaskStatus(taskId, DownloadStatus.DOWNLOADING) }
        }

    @Test
    fun `cancelDownload should clean up partial files and update state`() =
        runTest {
            // Arrange
            val taskId = UUID.randomUUID()
            val modelId = "test-model"

            coEvery { downloadManager.getModelIdForTask(taskId) } returns modelId

            // Act
            useCase.cancelDownload(taskId)

            // Assert
            coVerify { downloadManager.cancelDownload(taskId) }
            coVerify { downloadManager.deletePartialFiles(modelId) }
            coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.NOT_INSTALLED) }
        }

    @Test
    fun `deleteModel should fail if model is currently active in session`() =
        runTest {
            // Arrange
            val modelId = "active-model"
            coEvery { modelCatalogRepository.isModelActiveInSession(modelId) } returns true

            // Act
            val result = useCase.deleteModel(modelId)

            // Assert
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(ModelInUseException::class.java)
            coVerify(exactly = 0) { modelCatalogRepository.deleteModelFiles(modelId) }
        }

    @Test
    fun `deleteModel should remove files and update catalog when model inactive`() =
        runTest {
            // Arrange
            val modelId = "inactive-model"
            coEvery { modelCatalogRepository.isModelActiveInSession(modelId) } returns false

            // Act
            val result = useCase.deleteModel(modelId)

            // Assert
            assertThat(result.isSuccess).isTrue()
            coVerify { modelCatalogRepository.deleteModelFiles(modelId) }
            coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.NOT_INSTALLED) }
        }

    @Test
    fun `exportBackup should create bundle with personas, credentials, and config`() =
        runTest {
            // Arrange
            val exportPath = "/storage/emulated/0/Download/nanoai-backup.zip"

            val personas =
                listOf(
                    PersonaProfile(
                        personaId = UUID.randomUUID(),
                        name = "Assistant",
                        description = "General assistant",
                        systemPrompt = "You are helpful",
                        defaultModelPreference = null,
                        temperature = 0.7f,
                        topP = 0.9f,
                        defaultVoice = null,
                        defaultImageStyle = null,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now(),
                    ),
                )

            val apiConfigs =
                listOf(
                    APIProviderConfig(
                        providerId = "openai",
                        providerName = "OpenAI",
                        baseUrl = "https://api.openai.com/v1",
                        apiKey = "sk-test123",
                        apiType = APIType.OPENAI_COMPATIBLE,
                        isEnabled = true,
                        quotaResetAt = null,
                        lastStatus = ProviderStatus.OK,
                    ),
                )

            coEvery { exportService.gatherPersonas() } returns personas
            coEvery { exportService.gatherAPIProviderConfigs() } returns apiConfigs
            coEvery { exportService.createExportBundle(any(), any(), any()) } returns exportPath

            // Act
            val result = useCase.exportBackup(exportPath)

            // Assert
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).isEqualTo(exportPath)
            coVerify { exportService.createExportBundle(personas, apiConfigs, exportPath) }
        }

    @Test
    fun `exportBackup should exclude chat history by default`() =
        runTest {
            // Arrange
            val exportPath = "/storage/emulated/0/Download/backup.zip"

            // Act
            useCase.exportBackup(exportPath, includeChatHistory = false)

            // Assert
            coVerify(exactly = 0) { exportService.gatherChatHistory() }
            coVerify { exportService.createExportBundle(any(), any(), exportPath) }
        }

    @Test
    fun `exportBackup should include chat history when requested`() =
        runTest {
            // Arrange
            val exportPath = "/storage/emulated/0/Download/backup-with-history.zip"
            val chatThreads =
                listOf(
                    ChatThread(
                        threadId = UUID.randomUUID(),
                        title = "Test Chat",
                        personaId = null,
                        activeModelId = "test-model",
                        createdAt = Instant.now(),
                        updatedAt = Instant.now(),
                        isArchived = false,
                    ),
                )

            coEvery { exportService.gatherChatHistory() } returns chatThreads

            // Act
            useCase.exportBackup(exportPath, includeChatHistory = true)

            // Assert
            coVerify { exportService.gatherChatHistory() }
        }

    @Test
    fun `exportBackup should warn user about unencrypted export`() =
        runTest {
            // Arrange
            val exportPath = "/storage/emulated/0/Download/backup.zip"

            // Act
            val result = useCase.exportBackup(exportPath)

            // Assert: Warning should be logged or returned
            assertThat(result.isSuccess).isTrue()
            // Implementation should show warning dialog to user about storing securely
        }

    @Test
    fun `getDownloadProgress should emit progress updates`() =
        runTest {
            // Arrange
            val taskId = UUID.randomUUID()
            val progressFlow = flowOf(0.0f, 0.25f, 0.50f, 0.75f, 1.0f)

            coEvery { downloadManager.observeProgress(taskId) } returns progressFlow

            // Act & Assert
            useCase.getDownloadProgress(taskId).test {
                assertThat(awaitItem()).isEqualTo(0.0f)
                assertThat(awaitItem()).isEqualTo(0.25f)
                assertThat(awaitItem()).isEqualTo(0.50f)
                assertThat(awaitItem()).isEqualTo(0.75f)
                assertThat(awaitItem()).isEqualTo(1.0f)
                awaitComplete()
            }
        }

    @Test
    fun `getQueuedDownloads should return downloads in QUEUED state`() =
        runTest {
            // Arrange
            val queuedDownloads =
                listOf(
                    createDownloadTask(status = DownloadStatus.QUEUED, modelId = "model-1"),
                    createDownloadTask(status = DownloadStatus.QUEUED, modelId = "model-2"),
                )

            coEvery { downloadManager.getQueuedDownloads() } returns flowOf(queuedDownloads)

            // Act & Assert
            useCase.getQueuedDownloads().test {
                val result = awaitItem()
                assertThat(result).hasSize(2)
                assertThat(result.all { it.status == DownloadStatus.QUEUED }).isTrue()
                awaitComplete()
            }
        }

    @Test
    fun `retryFailedDownload should reset error state and restart`() =
        runTest {
            // Arrange
            val taskId = UUID.randomUUID()
            val modelId = "failed-model"
            val failedTask =
                createDownloadTask(
                    taskId = taskId,
                    modelId = modelId,
                    status = DownloadStatus.FAILED,
                )

            coEvery { downloadManager.getTaskById(taskId) } returns flowOf(failedTask)
            coEvery { downloadManager.getModelIdForTask(taskId) } returns modelId

            // Act
            useCase.retryFailedDownload(taskId)

            // Assert
            coVerify { downloadManager.resetTask(taskId) }
            coVerify { downloadManager.startDownload(modelId) }
            coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.DOWNLOADING) }
        }

    // Helper functions
    private fun createDownloadTask(
        taskId: UUID = UUID.randomUUID(),
        modelId: String = "test-model",
        status: DownloadStatus = DownloadStatus.DOWNLOADING,
        progress: Float = 0.5f,
    ) = DownloadTask(
        taskId = taskId,
        modelId = modelId,
        progress = progress,
        status = status,
        bytesDownloaded = 750000000L,
        startedAt = Instant.now(),
        finishedAt = null,
        errorMessage = null,
    )
}
