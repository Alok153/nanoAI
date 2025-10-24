package com.vjaykrsna.nanoai.feature.library.domain

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.core.model.ProviderStatus
import com.vjaykrsna.nanoai.feature.library.data.DownloadManager
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.data.catalog.DeliveryType
import com.vjaykrsna.nanoai.testing.assertIsSuccess
import com.vjaykrsna.nanoai.testing.assertRecoverableError
import com.vjaykrsna.nanoai.testing.assertSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Test

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
  fun `downloadModel enqueues when limit reached`() = runTest {
    val activeDownloads =
      listOf(
        createDownloadTask(status = DownloadStatus.DOWNLOADING),
        createDownloadTask(status = DownloadStatus.DOWNLOADING),
      )
    coEvery { downloadManager.getActiveDownloads() } returns flowOf(activeDownloads)
    coEvery { downloadManager.getMaxConcurrentDownloads() } returns 2

    val result = useCase.downloadModel("gemini-2.0-flash-lite")

    result.assertSuccess()
    coVerify { downloadManager.queueDownload("gemini-2.0-flash-lite") }
    coVerify(exactly = 0) { downloadManager.startDownload("gemini-2.0-flash-lite") }
    coVerify {
      modelCatalogRepository.updateInstallState("gemini-2.0-flash-lite", InstallState.DOWNLOADING)
    }
  }

  @Test
  fun `downloadModel starts immediately under limit`() = runTest {
    val activeDownloads = listOf(createDownloadTask(status = DownloadStatus.DOWNLOADING))
    coEvery { downloadManager.getActiveDownloads() } returns flowOf(activeDownloads)
    coEvery { downloadManager.getMaxConcurrentDownloads() } returns 3

    val result = useCase.downloadModel("phi-3-mini-4k")

    result.assertSuccess()
    coVerify { downloadManager.startDownload("phi-3-mini-4k") }
  }

  @Test
  fun `verifyDownloadChecksum succeeds when checksums match`() = runTest {
    val modelId = "gemini-2.0-flash-lite"
    val checksum = "abc123"
    every { modelCatalogRepository.getModelById(modelId) } returns
      flowOf(sampleModel(modelId, checksum))
    coEvery { downloadManager.getDownloadedChecksum(modelId) } returns checksum

    val result = useCase.verifyDownloadChecksum(modelId)

    val value = result.assertSuccess()
    assertThat(value).isTrue()
    coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.INSTALLED) }
  }

  @Test
  fun `verifyDownloadChecksum marks error on mismatch`() = runTest {
    val modelId = "gemini-2.0-flash-lite"
    every { modelCatalogRepository.getModelById(modelId) } returns
      flowOf(sampleModel(modelId, "expected"))
    coEvery { downloadManager.getDownloadedChecksum(modelId) } returns "different"

    val result = useCase.verifyDownloadChecksum(modelId)

    val value = result.assertSuccess()
    assertThat(value).isFalse()
    coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR) }
  }

  @Test
  fun `pauseDownload updates status`() = runTest {
    val taskId = UUID.randomUUID()

    useCase.pauseDownload(taskId)

    coVerify { downloadManager.pauseDownload(taskId) }
    coVerify { downloadManager.updateTaskStatus(taskId, DownloadStatus.PAUSED) }
  }

  @Test
  fun `resumeDownload restarts paused task`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadManager.getTaskById(taskId) } returns
      flowOf(createDownloadTask(taskId = taskId, status = DownloadStatus.PAUSED))

    useCase.resumeDownload(taskId)

    coVerify { downloadManager.resumeDownload(taskId) }
    coVerify { downloadManager.updateTaskStatus(taskId, DownloadStatus.DOWNLOADING) }
  }

  @Test
  fun `cancelDownload cleans up artifacts`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadManager.getModelIdForTask(taskId) } returns "test-model"

    useCase.cancelDownload(taskId)

    coVerify { downloadManager.cancelDownload(taskId) }
    coVerify { downloadManager.deletePartialFiles("test-model") }
    coVerify { modelCatalogRepository.updateInstallState("test-model", InstallState.NOT_INSTALLED) }
  }

  @Test
  fun `deleteModel rejects when active`() = runTest {
    val modelId = "active"
    coEvery { modelCatalogRepository.isModelActiveInSession(modelId) } returns true

    val result = useCase.deleteModel(modelId)

    result.assertRecoverableError()
    coVerify(exactly = 0) { modelCatalogRepository.deleteModelFiles(modelId) }
  }

  @Test
  fun `deleteModel removes files when idle`() = runTest {
    val modelId = "inactive"
    coEvery { modelCatalogRepository.isModelActiveInSession(modelId) } returns false

    val result = useCase.deleteModel(modelId)

    result.assertIsSuccess()
    coVerify { modelCatalogRepository.deleteModelFiles(modelId) }
    coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.NOT_INSTALLED) }
  }

  @Test
  fun `exportBackup generates bundle`() = runTest {
    val exportPath = "/tmp/nanoai-backup.zip"
    val personas = listOf(samplePersona())
    val providers = listOf(sampleProvider())

    coEvery { exportService.gatherPersonas() } returns personas
    coEvery { exportService.gatherAPIProviderConfigs() } returns providers
    coEvery {
      exportService.createExportBundle(personas, providers, exportPath, emptyList())
    } returns exportPath

    val result = useCase.exportBackup(exportPath)

    val exportedPath = result.assertSuccess()
    assertThat(exportedPath).isEqualTo(exportPath)
    coVerify { exportService.createExportBundle(personas, providers, exportPath, emptyList()) }
    coVerify { exportService.notifyUnencryptedExport(exportPath) }
  }

  @Test
  fun `exportBackup skips chat history by default`() = runTest {
    val exportPath = "/tmp/backup.zip"

    useCase.exportBackup(exportPath)

    coVerify(exactly = 0) { exportService.gatherChatHistory() }
  }

  @Test
  fun `exportBackup includes chat history on request`() = runTest {
    val exportPath = "/tmp/backup-with-history.zip"
    val chats = listOf(sampleThread())

    coEvery { exportService.gatherChatHistory() } returns chats
    coEvery { exportService.gatherPersonas() } returns emptyList()
    coEvery { exportService.gatherAPIProviderConfigs() } returns emptyList()
    coEvery {
      exportService.createExportBundle(emptyList(), emptyList(), exportPath, chats)
    } returns exportPath

    val result = useCase.exportBackup(exportPath, includeChatHistory = true)

    result.assertIsSuccess()
    coVerify { exportService.gatherChatHistory() }
    coVerify { exportService.createExportBundle(emptyList(), emptyList(), exportPath, chats) }
  }

  @Test
  fun `getDownloadProgress proxies flow`() = runTest {
    val taskId = UUID.randomUUID()
    val progressFlow = flowOf(0.0f, 0.25f, 1.0f)
    every { downloadManager.observeProgress(taskId) } returns progressFlow

    useCase.getDownloadProgress(taskId).test {
      assertThat(awaitItem()).isEqualTo(0.0f)
      assertThat(awaitItem()).isEqualTo(0.25f)
      assertThat(awaitItem()).isEqualTo(1.0f)
      awaitComplete()
    }
  }

  @Test
  fun `observeDownloadTasks emits managed list`() = runTest {
    val queue = listOf(createDownloadTask(status = DownloadStatus.QUEUED))
    every { downloadManager.observeManagedDownloads() } returns flowOf(queue)

    useCase.observeDownloadTasks().test {
      val emission = awaitItem()
      assertThat(emission).hasSize(1)
      assertThat(emission.first().status).isEqualTo(DownloadStatus.QUEUED)
      awaitComplete()
    }
  }

  @Test
  fun `retryFailedDownload resets and restarts`() = runTest {
    val taskId = UUID.randomUUID()
    val modelId = "failed-model"
    coEvery { downloadManager.getTaskById(taskId) } returns
      flowOf(createDownloadTask(taskId = taskId, modelId = modelId, status = DownloadStatus.FAILED))
    coEvery { downloadManager.getModelIdForTask(taskId) } returns modelId

    useCase.retryFailedDownload(taskId)

    coVerify { downloadManager.resetTask(taskId) }
    coVerify { downloadManager.startDownload(modelId) }
    coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.DOWNLOADING) }
  }

  private fun sampleModel(modelId: String, checksum: String?): ModelPackage =
    ModelPackage(
      modelId = modelId,
      displayName = "Model $modelId",
      version = "1.0.0",
      providerType = com.vjaykrsna.nanoai.feature.library.domain.ProviderType.MEDIA_PIPE,
      deliveryType = DeliveryType.LOCAL_ARCHIVE,
      minAppVersion = 1,
      sizeBytes = 1_000_000_000L,
      capabilities = setOf("TEXT_GEN"),
      installState = InstallState.DOWNLOADING,
      downloadTaskId = UUID.randomUUID(),
      manifestUrl = "https://cdn.nanoai.app/catalog/$modelId.json",
      checksumSha256 = checksum,
      signature = null,
      createdAt = Clock.System.now(),
      updatedAt = Clock.System.now(),
    )

  private fun samplePersona(): PersonaProfile =
    PersonaProfile(
      personaId = UUID.randomUUID(),
      name = "Assistant",
      description = "Helpful persona",
      systemPrompt = "Be helpful",
      defaultModelPreference = null,
      temperature = 0.7f,
      topP = 0.9f,
      defaultVoice = null,
      defaultImageStyle = null,
      createdAt = Clock.System.now(),
      updatedAt = Clock.System.now(),
    )

  private fun sampleProvider(): APIProviderConfig =
    APIProviderConfig(
      providerId = "openai",
      providerName = "OpenAI",
      baseUrl = "https://api.openai.com/v1",
      apiKey = "sk-test",
      apiType = APIType.OPENAI_COMPATIBLE,
      isEnabled = true,
      quotaResetAt = null,
      lastStatus = ProviderStatus.OK,
    )

  private fun sampleThread(): ChatThread =
    ChatThread(
      threadId = UUID.randomUUID(),
      title = "Test",
      personaId = null,
      activeModelId = "model",
      createdAt = Clock.System.now(),
      updatedAt = Clock.System.now(),
      isArchived = false,
    )

  private fun createDownloadTask(
    taskId: UUID = UUID.randomUUID(),
    modelId: String = "model",
    status: DownloadStatus,
    progress: Float = 0.5f,
  ): DownloadTask =
    DownloadTask(
      taskId = taskId,
      modelId = modelId,
      progress = progress,
      status = status,
      bytesDownloaded = 750_000_000L,
      startedAt = Clock.System.now(),
      finishedAt = null,
      errorMessage = null,
    )
}
