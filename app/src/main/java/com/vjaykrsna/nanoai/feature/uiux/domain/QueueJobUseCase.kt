package com.vjaykrsna.nanoai.feature.uiux.domain

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.feature.uiux.data.ShellStateRepository
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Queues generation jobs and manages undo payloads for background operations. */
class QueueJobUseCase
@Inject
constructor(
  private val repository: ShellStateRepository,
  @IoDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private val scope = CoroutineScope(SupervisorJob() + dispatcher)

  fun execute(job: ProgressJob) {
    scope.launch {
      val layout = repository.shellLayoutState.first()
      repository.queueJob(job)
      repository.recordUndoPayload(
        UndoPayload(
          actionId = "queue-${job.jobId}",
          metadata =
            mapOf(
              "message" to
                buildQueuedJobMessage(job, layout.connectivity != ConnectivityStatus.ONLINE),
              "jobId" to job.jobId.toString(),
            ),
        )
      )
    }
  }

  private fun buildQueuedJobMessage(job: ProgressJob, isOffline: Boolean): String {
    val label = jobLabel(job)
    return when {
      job.status == com.vjaykrsna.nanoai.feature.uiux.state.JobStatus.FAILED && job.canRetry ->
        "$label retry scheduled"
      isOffline -> "$label queued for reconnect"
      job.status == com.vjaykrsna.nanoai.feature.uiux.state.JobStatus.PENDING -> "$label queued"
      else -> "$label updated"
    }
  }

  private fun jobLabel(job: ProgressJob): String =
    when (job.type) {
      com.vjaykrsna.nanoai.feature.uiux.state.JobType.IMAGE_GENERATION -> "Image generation"
      com.vjaykrsna.nanoai.feature.uiux.state.JobType.AUDIO_RECORDING -> "Audio recording"
      com.vjaykrsna.nanoai.feature.uiux.state.JobType.MODEL_DOWNLOAD -> "Model download"
      com.vjaykrsna.nanoai.feature.uiux.state.JobType.TEXT_GENERATION -> "Text generation"
      com.vjaykrsna.nanoai.feature.uiux.state.JobType.TRANSLATION -> "Translation"
      com.vjaykrsna.nanoai.feature.uiux.state.JobType.OTHER -> "Background task"
    }
}
