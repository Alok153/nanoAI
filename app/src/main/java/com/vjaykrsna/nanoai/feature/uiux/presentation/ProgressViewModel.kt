package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.feature.uiux.data.ShellStateRepository
import com.vjaykrsna.nanoai.feature.uiux.domain.ProgressCenterCoordinator
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel responsible for background job progress management and coordination. */
@HiltViewModel
class ProgressViewModel
@Inject
constructor(
  private val repository: ShellStateRepository,
  private val progressCoordinator: ProgressCenterCoordinator,
  @MainImmediateDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {

  /** Flow of all active progress jobs for display in progress center. */
  val progressJobs: StateFlow<List<ProgressJob>> =
    progressCoordinator.progressJobs.stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = emptyList(),
    )

  /** Queues a generation job (e.g., when offline or model busy). */
  fun queueGeneration(job: ProgressJob) {
    viewModelScope.launch(dispatcher) {
      val layout = repository.shellLayoutState.first()
      repository.queueJob(job)
      repository.recordUndoPayload(
        UndoPayload(
          actionId = "queue-${job.jobId}",
          metadata =
            mapOf(
              "message" to
                buildQueuedJobMessage(
                  job,
                  layout.connectivity !=
                    com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus.ONLINE,
                ),
              "jobId" to job.jobId.toString(),
            ),
        )
      )
    }
  }

  /** Completes a job, removing it from the progress center. */
  fun completeJob(jobId: UUID) {
    viewModelScope.launch(dispatcher) {
      repository.completeJob(jobId)
      repository.recordUndoPayload(null)
    }
  }

  /** Attempts to retry a failed job via the progress coordinator. */
  fun retryJob(job: ProgressJob) {
    viewModelScope.launch(dispatcher) { progressCoordinator.retryJob(job.jobId) }
  }

  /** Cancels an active job. */
  fun cancelJob(jobId: UUID) {
    viewModelScope.launch(dispatcher) { progressCoordinator.cancelJob(jobId) }
  }

  /** Pauses a running job. */
  fun pauseJob(jobId: UUID) {
    viewModelScope.launch(dispatcher) { progressCoordinator.pauseJob(jobId) }
  }

  /** Resumes a paused job. */
  fun resumeJob(jobId: UUID) {
    viewModelScope.launch(dispatcher) { progressCoordinator.resumeJob(jobId) }
  }

  /** Executes an undo action based on the provided payload. */
  fun undoAction(payload: UndoPayload) {
    viewModelScope.launch(dispatcher) {
      // Parse the action ID to determine what to undo
      when {
        payload.actionId.startsWith("queue-") -> {
          val jobIdString = payload.actionId.removePrefix("queue-")
          val jobId = runCatching { UUID.fromString(jobIdString) }.getOrNull()
          if (jobId != null) {
            repository.completeJob(jobId)
          }
        }
      // Add more undo action types as needed
      }
      repository.recordUndoPayload(null)
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
