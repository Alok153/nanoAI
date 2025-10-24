package com.vjaykrsna.nanoai.feature.uiux.domain

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.data.repository.ConnectivityRepository
import com.vjaykrsna.nanoai.core.data.repository.NavigationRepository
import com.vjaykrsna.nanoai.core.data.repository.ProgressRepository
import com.vjaykrsna.nanoai.feature.uiux.presentation.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.presentation.JobStatus
import com.vjaykrsna.nanoai.feature.uiux.presentation.JobType
import com.vjaykrsna.nanoai.feature.uiux.presentation.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.presentation.UndoPayload
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
  private val progressRepository: ProgressRepository,
  private val connectivityRepository: ConnectivityRepository,
  private val navigationRepository: NavigationRepository,
  @IoDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private val scope = CoroutineScope(SupervisorJob() + dispatcher)

  fun execute(job: ProgressJob) {
    scope.launch {
      val connectivityStatus = connectivityRepository.connectivityBannerState.first().status
      progressRepository.queueJob(job)
      navigationRepository.recordUndoPayload(
        UndoPayload(
          actionId = "queue-${job.jobId}",
          metadata =
            mapOf(
              "message" to
                buildQueuedJobMessage(job, connectivityStatus != ConnectivityStatus.ONLINE),
              "jobId" to job.jobId.toString(),
            ),
        )
      )
    }
  }

  private fun buildQueuedJobMessage(job: ProgressJob, isOffline: Boolean): String {
    val label = jobLabel(job)
    return when {
      job.status == JobStatus.FAILED && job.canRetry ->
        "$label retry scheduled"
      isOffline -> "$label queued for reconnect"
      job.status == JobStatus.PENDING -> "$label queued"
      else -> "$label updated"
    }
  }

  private fun jobLabel(job: ProgressJob): String =
    when (job.type) {
      JobType.IMAGE_GENERATION -> "Image generation"
      JobType.AUDIO_RECORDING -> "Audio recording"
      JobType.MODEL_DOWNLOAD -> "Model download"
      JobType.TEXT_GENERATION -> "Text generation"
      JobType.TRANSLATION -> "Translation"
      JobType.OTHER -> "Background task"
    }
}
