package com.vjaykrsna.nanoai.core.domain.uiux

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.domain.model.uiux.UndoPayload
import com.vjaykrsna.nanoai.core.domain.repository.NavigationRepository
import com.vjaykrsna.nanoai.core.domain.repository.ProgressRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Executes undo actions based on provided payloads. */
class UndoActionUseCase
@Inject
constructor(
  private val progressRepository: ProgressRepository,
  private val navigationRepository: NavigationRepository,
  @IoDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private val scope = CoroutineScope(SupervisorJob() + dispatcher)

  fun execute(payload: UndoPayload) {
    scope.launch {
      // Parse the action ID to determine what to undo
      when {
        payload.actionId.startsWith("queue-") -> {
          val jobIdString = payload.actionId.removePrefix("queue-")
          val jobId = runCatching { UUID.fromString(jobIdString) }.getOrNull()
          if (jobId != null) {
            progressRepository.completeJob(jobId)
          }
        }
      // Add more undo action types as needed
      }
      navigationRepository.recordUndoPayload(null)
    }
  }
}
