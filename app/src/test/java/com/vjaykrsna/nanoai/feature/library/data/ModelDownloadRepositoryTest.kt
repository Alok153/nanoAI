package com.vjaykrsna.nanoai.feature.library.data

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult as CommonNanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModelDownloadRepositoryTest {
  private val dispatcher = StandardTestDispatcher()
  private val dataSource = TestModelDownloadDataSource()
  private val repository = DefaultModelDownloadRepository(dataSource, dispatcher)

  @Test
  fun queueDownload_returnsRecoverableWhenDuplicateExists() =
    runTest(dispatcher) {
      val existing = sampleTask(modelId = "model-1", status = DownloadStatus.DOWNLOADING)
      dataSource.downloads.value = listOf(existing)

      val result = repository.queueDownload("model-1")

      assertThat(result).isInstanceOf(CommonNanoAIResult.RecoverableError::class.java)
      assertThat(dataSource.queuedModelIds).isEmpty()
    }

  @Test
  fun resumeDownload_blocksWhenActiveDownloadPresent() =
    runTest(dispatcher) {
      val active = sampleTask(status = DownloadStatus.DOWNLOADING)
      val paused = sampleTask(taskId = UUID.randomUUID(), status = DownloadStatus.PAUSED)
      dataSource.downloads.value = listOf(active, paused)

      val result = repository.resumeDownload(paused.taskId)

      assertThat(result).isInstanceOf(CommonNanoAIResult.RecoverableError::class.java)
      assertThat(dataSource.resumedTaskIds).isEmpty()
    }

  @Test
  fun resumeDownload_allowsWhenSlotFree() =
    runTest(dispatcher) {
      val paused = sampleTask(taskId = UUID.randomUUID(), status = DownloadStatus.PAUSED)
      dataSource.downloads.value = listOf(paused)

      val result = repository.resumeDownload(paused.taskId)

      assertThat(result).isInstanceOf(CommonNanoAIResult.Success::class.java)
      assertThat(dataSource.resumedTaskIds).containsExactly(paused.taskId)
    }

  @Test
  fun completedDownload_triggersVerificationOnce() =
    runTest(dispatcher) {
      val completed = sampleTask(modelId = "model-verify", status = DownloadStatus.COMPLETED)
      dataSource.downloads.value = listOf(completed)

      advanceUntilIdle()

      assertThat(dataSource.verifiedModelIds).containsExactly("model-verify")
    }

  private fun sampleTask(
    modelId: String = "model-id",
    taskId: UUID = UUID.randomUUID(),
    status: DownloadStatus = DownloadStatus.DOWNLOADING,
  ): DownloadTask =
    DownloadTask(
      taskId = taskId,
      modelId = modelId,
      progress = 0.5f,
      status = status,
      bytesDownloaded = 512,
      totalBytes = 1024,
      startedAt = Instant.DISTANT_PAST,
      finishedAt = null,
      errorMessage = null,
    )

  private class TestModelDownloadDataSource : ModelDownloadDataSource {
    val downloads = MutableStateFlow<List<DownloadTask>>(emptyList())
    val queuedModelIds = mutableListOf<String>()
    val pausedTaskIds = mutableListOf<UUID>()
    val resumedTaskIds = mutableListOf<UUID>()
    val cancelledTaskIds = mutableListOf<UUID>()
    val retriedTaskIds = mutableListOf<UUID>()
    val deletedModelIds = mutableListOf<String>()
    val verifiedModelIds = mutableListOf<String>()

    override fun observeDownloads() = downloads

    override suspend fun observeDownload(taskId: UUID) =
      MutableStateFlow(downloads.value.firstOrNull { it.taskId == taskId })

    override fun observeDownloadProgress(taskId: UUID) = MutableStateFlow(0f)

    override suspend fun queueDownload(modelId: String): NanoAIResult<UUID> {
      queuedModelIds += modelId
      return NanoAIResult.success(UUID.randomUUID())
    }

    override suspend fun pauseDownload(taskId: UUID): NanoAIResult<Unit> {
      pausedTaskIds += taskId
      return NanoAIResult.success(Unit)
    }

    override suspend fun resumeDownload(taskId: UUID): NanoAIResult<Unit> {
      resumedTaskIds += taskId
      return NanoAIResult.success(Unit)
    }

    override suspend fun cancelDownload(taskId: UUID): NanoAIResult<Unit> {
      cancelledTaskIds += taskId
      return NanoAIResult.success(Unit)
    }

    override suspend fun retryDownload(taskId: UUID): NanoAIResult<Unit> {
      retriedTaskIds += taskId
      return NanoAIResult.success(Unit)
    }

    override suspend fun deleteModel(modelId: String): NanoAIResult<Unit> {
      deletedModelIds += modelId
      return NanoAIResult.success(Unit)
    }

    override suspend fun verifyDownload(modelId: String): NanoAIResult<Boolean> {
      verifiedModelIds += modelId
      return NanoAIResult.success(true)
    }
  }
}
