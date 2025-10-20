package com.vjaykrsna.nanoai.testing

import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import java.util.UUID

/**
 * Fake implementation of SendPromptAndPersonaUseCase for instrumentation testing. Provides
 * controllable behavior for send and switch operations.
 */
class FakeSendPromptAndPersonaUseCase {
  var shouldFailOnSend = false
  var shouldFailOnSwitch = false
  var lastPrompt: String? = null
  var lastPersonaId: UUID? = null
  var lastThreadId: UUID? = null
  var lastSwitchAction: PersonaSwitchAction? = null
  var nextSwitchResultThreadId: UUID? = null

  fun clearState() {
    lastPrompt = null
    lastPersonaId = null
    lastThreadId = null
    lastSwitchAction = null
    nextSwitchResultThreadId = null
    shouldFailOnSend = false
    shouldFailOnSwitch = false
  }

  suspend fun sendPrompt(threadId: UUID, prompt: String, personaId: UUID): Result<Unit> {
    lastThreadId = threadId
    lastPrompt = prompt
    lastPersonaId = personaId

    return if (shouldFailOnSend) {
      Result.failure(Exception("Failed to send prompt"))
    } else {
      Result.success(Unit)
    }
  }

  suspend fun switchPersona(threadId: UUID, newPersonaId: UUID, action: PersonaSwitchAction): UUID {
    if (shouldFailOnSwitch) {
      error("Failed to switch persona")
    }
    lastThreadId = threadId
    lastPersonaId = newPersonaId
    lastSwitchAction = action
    return nextSwitchResultThreadId ?: UUID.randomUUID()
  }
}
