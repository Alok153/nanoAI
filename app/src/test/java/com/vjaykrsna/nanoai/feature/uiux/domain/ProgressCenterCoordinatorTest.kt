package com.vjaykrsna.nanoai.feature.uiux.domain

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.feature.library.data.DownloadManager
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import com.vjaykrsna.nanoai.feature.uiux.state.JobStatus
import com.vjaykrsna.nanoai.feature.uiux.state.JobType
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProgressCenterCoordinatorTest {
  private val scheduler = TestCoroutineScheduler()
  private val dispatcher = StandardTestDispatcher(scheduler)
  private lateinit var downloadManager: FakeDownloadManager
  private lateinit var coordinator: ProgressCenterCoordinator

  @BeforeEach
  fun setUp() {
    downloadManager = FakeDownloadManager()
    coordinator = ProgressCenterCoordinator(downloadManager, dispatcher)
  }

  @Test
  fun progressJobs_emitsDownloadAndQueuedTasks() =
    runTest(scheduler) {
      val activeTask = downloadTask(DownloadStatus.DOWNLOADING, progress = 0.6f)
      val queuedTask = downloadTask(DownloadStatus.QUEUED)

      downloadManager.setActiveTasks(listOf(activeTask))
      downloadManager.setQueuedTasks(listOf(queuedTask))

      val jobs = coordinator.progressJobs.first { it.size == 2 }

      val runningJob = jobs.first { it.jobId == activeTask.taskId }
      assertThat(runningJob.type).isEqualTo(JobType.MODEL_DOWNLOAD)
      assertThat(runningJob.status).isEqualTo(JobStatus.RUNNING)
      assertThat(runningJob.progress).isEqualTo(0.6f)
      assertThat(runningJob.canRetry).isFalse()
      assertThat(runningJob.queuedAt).isNotEqualTo(Instant.EPOCH)

      val pendingJob = jobs.first { it.jobId == queuedTask.taskId }
      assertThat(pendingJob.status).isEqualTo(JobStatus.PENDING)
      assertThat(pendingJob.canRetry).isFalse()
    }

  @Test
  fun retryPauseResume_delegateToDownloadManagerBasedOnStatus() =
    runTest(scheduler) {
      val failedTask = downloadTask(DownloadStatus.FAILED)
      val runningTask = downloadTask(DownloadStatus.DOWNLOADING)
      val pausedTask = downloadTask(DownloadStatus.PAUSED)
      val cancellableTask = downloadTask(DownloadStatus.DOWNLOADING)
      val ignoredTask = downloadTask(DownloadStatus.DOWNLOADING)

      downloadManager.registerTask(failedTask)
      downloadManager.registerTask(runningTask)
      downloadManager.registerTask(pausedTask)
      downloadManager.registerTask(cancellableTask)
      downloadManager.registerTask(ignoredTask)

      coordinator.retryJob(failedTask.taskId)
      coordinator.pauseJob(runningTask.taskId)
      coordinator.resumeJob(pausedTask.taskId)
      coordinator.cancelJob(cancellableTask.taskId)
      // This retry should be ignored because the task is not failed.
      coordinator.retryJob(ignoredTask.taskId)

      advanceUntilIdle()

      assertThat(downloadManager.actions)
        .containsExactly(
          "retry:${failedTask.taskId}",
          "pause:${runningTask.taskId}",
          "resume:${pausedTask.taskId}",
          "cancel:${cancellableTask.taskId}",
        )
        .inOrder()
    }

  private fun downloadTask(
    status: DownloadStatus,
    progress: Float = 0f,
    id: UUID = UUID.randomUUID()
  ): DownloadTask =
    DownloadTask(
      taskId = id,
      modelId = "model-${id}",
      progress = progress,
      status = status,
      startedAt = Clock.System.now(),
    )

  private class FakeDownloadManager : DownloadManager {
    private val activeFlow = MutableStateFlow<List<DownloadTask>>(emptyList())
    private val queuedFlow = MutableStateFlow<List<DownloadTask>>(emptyList())
    private val statusMap = mutableMapOf<UUID, DownloadTask>()

    val actions = mutableListOf<String>()

    suspend fun setActiveTasks(tasks: List<DownloadTask>) {
      tasks.forEach { statusMap[it.taskId] = it }
      activeFlow.emit(tasks)
    }

    suspend fun setQueuedTasks(tasks: List<DownloadTask>) {
      tasks.forEach { statusMap.putIfAbsent(it.taskId, it) }
      queuedFlow.emit(tasks)
    }

    fun registerTask(task: DownloadTask) {
      statusMap[task.taskId] = task
    }

    override suspend fun getActiveDownloads(): Flow<List<DownloadTask>> = activeFlow

    override fun getQueuedDownloads(): Flow<List<DownloadTask>> = queuedFlow

    override suspend fun getDownloadStatus(taskId: UUID): DownloadTask? = statusMap[taskId]

    override suspend fun retryDownload(taskId: UUID) {
      actions += "retry:$taskId"
    }

    override suspend fun cancelDownload(taskId: UUID) {
      actions += "cancel:$taskId"
    }

    override suspend fun pauseDownload(taskId: UUID) {
      actions += "pause:$taskId"
    }

    override suspend fun resumeDownload(taskId: UUID) {
      actions += "resume:$taskId"
    }

    override suspend fun startDownload(modelId: String): UUID =
      throw UnsupportedOperationException("Not used in tests")

    override suspend fun queueDownload(modelId: String): UUID =
      throw UnsupportedOperationException("Not used in tests")

    override suspend fun getTaskById(taskId: UUID): Flow<DownloadTask?> =
      MutableStateFlow(statusMap[taskId])

    override suspend fun getMaxConcurrentDownloads(): Int = 0

    override suspend fun updateTaskStatus(taskId: UUID, status: DownloadStatus) {
      statusMap[taskId]?.let { statusMap[taskId] = it.copy(status = status) }
    }

    override suspend fun getModelIdForTask(taskId: UUID): String? = statusMap[taskId]?.modelId

    override suspend fun getDownloadedChecksum(modelId: String): String? = null

    override suspend fun deletePartialFiles(modelId: String) = Unit

    override suspend fun resetTask(taskId: UUID) = Unit

    override fun observeProgress(taskId: UUID): Flow<Float> = emptyFlow()
  }
}
