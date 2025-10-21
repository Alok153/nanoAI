package com.vjaykrsna.nanoai.feature.uiux.domain

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.feature.uiux.data.ShellStateRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Consolidated simple job operations for background task management. */
class JobOperationsUseCase
@Inject
constructor(
  private val repository: ShellStateRepository,
  private val progressCoordinator: ProgressCenterCoordinator,
  @IoDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private val scope = CoroutineScope(SupervisorJob() + dispatcher)

  /** Completes a job, removing it from the progress center and clearing undo payloads. */
  fun completeJob(jobId: UUID) {
    scope.launch {
      repository.completeJob(jobId)
      repository.recordUndoPayload(null)
    }
  }

  /** Attempts to retry a failed job via the progress coordinator. */
  fun retryJob(jobId: UUID) {
    scope.launch { progressCoordinator.retryJob(jobId) }
  }
}
