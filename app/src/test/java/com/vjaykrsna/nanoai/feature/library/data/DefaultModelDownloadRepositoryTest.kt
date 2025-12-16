package com.vjaykrsna.nanoai.feature.library.data

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import com.vjaykrsna.nanoai.core.model.NanoAISuccess
import com.vjaykrsna.nanoai.feature.library.domain.CancelModelDownloadUseCase
import com.vjaykrsna.nanoai.feature.library.domain.ModelDownloadRepository
import com.vjaykrsna.nanoai.feature.library.domain.ObserveDownloadTasksUseCase
import com.vjaykrsna.nanoai.feature.library.domain.PauseModelDownloadUseCase
import com.vjaykrsna.nanoai.feature.library.domain.QueueModelDownloadUseCase
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultModelDownloadRepositoryTest {
  private val fakeDataSource = FakeModelDownloadDataSource()
  private val dispatcher = StandardTestDispatcher()
  private val repository: ModelDownloadRepository =
    DefaultModelDownloadRepository(fakeDataSource, dispatcher)
  private val queueUseCase = QueueModelDownloadUseCase(repository)
  private val pauseUseCase = PauseModelDownloadUseCase(repository)
  private val cancelUseCase = CancelModelDownloadUseCase(repository)
  private val observeTasksUseCase = ObserveDownloadTasksUseCase(repository)

  @Test
  fun queueDownload_delegatesToDataSource() = runTest {
    val result = queueUseCase("model-1")

    assertThat(fakeDataSource.queuedModelIds).containsExactly("model-1")
    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
  }

  @Test
  fun observeDownloads_emitsDataSourceFlow() = runTest {
    val task = sampleTask()
    fakeDataSource.downloads.value = listOf(task)

    val observed = observeTasksUseCase().first()

    assertThat(observed).containsExactly(task)
  }

  @Test
  fun cancelDownload_delegatesViaUseCase() = runTest {
    val taskId = UUID.randomUUID()

    val result = cancelUseCase(taskId)

    assertThat(fakeDataSource.cancelledTaskIds).containsExactly(taskId)
    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
  }

  @Test
  fun pauseDownload_routesThroughRepository() = runTest {
    val taskId = UUID.randomUUID()

    val result = pauseUseCase(taskId)

    assertThat(fakeDataSource.pausedTaskIds).containsExactly(taskId)
    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
  }

  private fun sampleTask(): DownloadTask =
    DownloadTask(
      taskId = UUID.randomUUID(),
      modelId = "model-id",
      progress = 0.5f,
      status = DownloadStatus.DOWNLOADING,
      bytesDownloaded = 512,
      totalBytes = 1024,
      startedAt = Instant.DISTANT_PAST,
      finishedAt = null,
      errorMessage = null,
    )
}

private class FakeModelDownloadDataSource : ModelDownloadDataSource {
  val downloads = MutableStateFlow<List<DownloadTask>>(emptyList())
  val downloadProgress = mutableMapOf<UUID, MutableStateFlow<Float>>()
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

  override fun observeDownloadProgress(taskId: UUID) =
    downloadProgress.getOrPut(taskId) { MutableStateFlow(0f) }

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
