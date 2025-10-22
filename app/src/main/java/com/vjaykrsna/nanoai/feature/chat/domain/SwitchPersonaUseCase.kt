package com.vjaykrsna.nanoai.feature.chat.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.data.repository.PersonaSwitchLogRepository
import com.vjaykrsna.nanoai.core.domain.model.PersonaSwitchLog
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Clock

/** Use case for switching personas and managing conversation threads. */
@Singleton
class SwitchPersonaUseCase
@Inject
constructor(
  private val conversationRepository: ConversationRepository,
  private val personaSwitchLogRepository: PersonaSwitchLogRepository,
) {
  suspend operator fun invoke(
    threadId: UUID,
    newPersonaId: UUID,
    action: PersonaSwitchAction,
  ): NanoAIResult<UUID> {
    return try {
      val previousPersonaId = conversationRepository.getCurrentPersonaForThread(threadId)
      val targetThreadId =
        when (action) {
          PersonaSwitchAction.CONTINUE_THREAD -> {
            conversationRepository.updateThreadPersona(threadId, newPersonaId)
            threadId
          }
          PersonaSwitchAction.START_NEW_THREAD -> {
            conversationRepository.createNewThread(newPersonaId)
          }
        }

      val log =
        PersonaSwitchLog(
          logId = UUID.randomUUID(),
          threadId = targetThreadId,
          previousPersonaId = previousPersonaId,
          newPersonaId = newPersonaId,
          actionTaken = action,
          createdAt = Clock.System.now(),
        )
      personaSwitchLogRepository.logSwitch(log)
      NanoAIResult.success(targetThreadId)
    } catch (error: Throwable) {
      NanoAIResult.recoverable(
        message = "Failed to switch persona: ${error.message}",
        cause = error,
      )
    }
  }
}
