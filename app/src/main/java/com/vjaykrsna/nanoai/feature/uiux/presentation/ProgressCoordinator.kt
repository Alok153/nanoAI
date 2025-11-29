package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.domain.model.uiux.ProgressJob
import com.vjaykrsna.nanoai.core.domain.model.uiux.UndoPayload
import com.vjaykrsna.nanoai.core.domain.uiux.JobOperationsUseCase
import com.vjaykrsna.nanoai.core.domain.uiux.ProgressCenterCoordinator
import com.vjaykrsna.nanoai.core.domain.uiux.QueueJobUseCase
import com.vjaykrsna.nanoai.core.domain.uiux.UndoActionUseCase
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Coordinator responsible for background job progress management and coordination.
 *
 * This is a regular injectable class (not a ViewModel) because it's composed into ShellViewModel
 * rather than used independently with ViewModelProvider.
 */
@Singleton
class ProgressCoordinator
@Inject
constructor(
  private val progressCenterCoordinator: ProgressCenterCoordinator,
  private val queueJobUseCase: QueueJobUseCase,
  private val jobOperationsUseCase: JobOperationsUseCase,
  private val undoActionUseCase: UndoActionUseCase,
  @MainImmediateDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {

  /** Flow of all active progress jobs for display in progress center. */
  fun progressJobs(scope: CoroutineScope): StateFlow<List<ProgressJob>> =
    progressCenterCoordinator.progressJobs.stateIn(
      scope = scope,
      started = SharingStarted.Eagerly,
      initialValue = emptyList(),
    )

  /** Queues a generation job (e.g., when offline or model busy). */
  fun queueGeneration(job: ProgressJob) {
    queueJobUseCase.execute(job)
  }

  /** Completes a job, removing it from the progress center. */
  fun completeJob(jobId: UUID) {
    jobOperationsUseCase.completeJob(jobId)
  }

  /** Attempts to retry a failed job via the progress coordinator. */
  fun retryJob(job: ProgressJob) {
    jobOperationsUseCase.retryJob(job.jobId)
  }

  /** Cancels an active job. */
  fun cancelJob(scope: CoroutineScope, jobId: UUID) {
    scope.launch(dispatcher) { progressCenterCoordinator.cancelJob(jobId) }
  }

  /** Pauses a running job. */
  fun pauseJob(scope: CoroutineScope, jobId: UUID) {
    scope.launch(dispatcher) { progressCenterCoordinator.pauseJob(jobId) }
  }

  /** Resumes a paused job. */
  fun resumeJob(scope: CoroutineScope, jobId: UUID) {
    scope.launch(dispatcher) { progressCenterCoordinator.resumeJob(jobId) }
  }

  /** Executes an undo action based on the provided payload. */
  fun undoAction(payload: UndoPayload) {
    undoActionUseCase.execute(payload)
  }
}
