package com.vjaykrsna.nanoai.feature.library.data

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.library.ModelDownloadsAndExportUseCaseInterface
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import com.vjaykrsna.nanoai.core.model.NanoAISuccess
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoreModelDownloadDataSourceTest {
  private val fakeUseCase = FakeModelDownloadsAndExportUseCase()
  private val dataSource = CoreModelDownloadDataSource(fakeUseCase)

  @Test
  fun queueDownload_delegatesToUseCase() = runTest {
    val result = dataSource.queueDownload("model-1")

    assertThat(fakeUseCase.queuedModels).containsExactly("model-1")
    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
  }

  @Test
  fun observeDownloads_streamsFromUseCase() = runTest {
    val task = sampleTask()
    fakeUseCase.downloads.value = listOf(task)

    val observed = dataSource.observeDownloads().first()

    assertThat(observed).containsExactly(task)
  }

  @Test
  fun cancelDownload_delegatesToUseCase() = runTest {
    val taskId = UUID.randomUUID()

    val result = dataSource.cancelDownload(taskId)

    assertThat(fakeUseCase.cancelledTasks).containsExactly(taskId)
    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
  }

  private fun sampleTask(): DownloadTask =
    DownloadTask(
      taskId = UUID.randomUUID(),
      modelId = "model-id",
      progress = 0.25f,
      status = DownloadStatus.DOWNLOADING,
      bytesDownloaded = 256,
      totalBytes = 1024,
      startedAt = Instant.DISTANT_PAST,
      finishedAt = null,
      errorMessage = null,
    )

  private class FakeModelDownloadsAndExportUseCase : ModelDownloadsAndExportUseCaseInterface {
    val downloads = MutableStateFlow<List<DownloadTask>>(emptyList())
    val progress = mutableMapOf<UUID, MutableStateFlow<Float>>()
    val queuedModels = mutableListOf<String>()
    val pausedTasks = mutableListOf<UUID>()
    val resumedTasks = mutableListOf<UUID>()
    val cancelledTasks = mutableListOf<UUID>()
    val deletedModels = mutableListOf<String>()
    val retriedTasks = mutableListOf<UUID>()
    val exportedBundles = mutableListOf<String>()

    override fun observeDownloadTasks() = downloads

    override suspend fun observeDownloadTask(taskId: UUID) =
      MutableStateFlow(downloads.value.firstOrNull { it.taskId == taskId })

    override fun getDownloadProgress(taskId: UUID) =
      progress.getOrPut(taskId) { MutableStateFlow(0f) }

    override suspend fun downloadModel(modelId: String): NanoAIResult<UUID> {
      queuedModels += modelId
      return NanoAIResult.success(UUID.randomUUID())
    }

    override suspend fun pauseDownload(taskId: UUID): NanoAIResult<Unit> {
      pausedTasks += taskId
      return NanoAIResult.success(Unit)
    }

    override suspend fun resumeDownload(taskId: UUID): NanoAIResult<Unit> {
      resumedTasks += taskId
      return NanoAIResult.success(Unit)
    }

    override suspend fun cancelDownload(taskId: UUID): NanoAIResult<Unit> {
      cancelledTasks += taskId
      return NanoAIResult.success(Unit)
    }

    override suspend fun deleteModel(modelId: String): NanoAIResult<Unit> {
      deletedModels += modelId
      return NanoAIResult.success(Unit)
    }

    override suspend fun verifyDownloadChecksum(modelId: String): NanoAIResult<Boolean> =
      NanoAIResult.success(true)

    override suspend fun retryFailedDownload(taskId: UUID): NanoAIResult<Unit> {
      retriedTasks += taskId
      return NanoAIResult.success(Unit)
    }

    override suspend fun exportBackup(
      destinationPath: String,
      includeChatHistory: Boolean,
    ): NanoAIResult<String> {
      exportedBundles += destinationPath
      return NanoAIResult.success(destinationPath)
    }
  }
}
