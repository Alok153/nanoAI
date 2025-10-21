package com.vjaykrsna.nanoai.feature.uiux.domain

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.feature.uiux.data.ShellStateRepository
import com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload
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
  private val repository: ShellStateRepository,
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
            repository.completeJob(jobId)
          }
        }
      // Add more undo action types as needed
      }
      repository.recordUndoPayload(null)
    }
  }
}
