package com.vjaykrsna.nanoai.core.domain.uiux

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.domain.repository.NavigationRepository
import com.vjaykrsna.nanoai.core.domain.repository.ProgressRepository
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
  private val progressRepository: ProgressRepository,
  private val navigationRepository: NavigationRepository,
  private val progressCoordinator: ProgressCenterCoordinator,
  @IoDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private val scope = CoroutineScope(SupervisorJob() + dispatcher)

  /** Completes a job, removing it from the progress center and clearing undo payloads. */
  fun completeJob(jobId: UUID) {
    scope.launch {
      progressRepository.completeJob(jobId)
      navigationRepository.recordUndoPayload(null)
    }
  }

  /** Attempts to retry a failed job via the progress coordinator. */
  fun retryJob(jobId: UUID) {
    scope.launch { progressCoordinator.retryJob(jobId) }
  }
}
