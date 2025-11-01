package com.vjaykrsna.nanoai.testing

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import javax.inject.Inject
import javax.inject.Singleton

/** Fake implementation of [GetConversationHistoryUseCase] for testing. */
@Singleton
class FakeGetConversationHistoryUseCase
@Inject
constructor(private val fakeConversationRepository: FakeConversationRepository) {

  var shouldFail = false

  suspend operator fun invoke(): NanoAIResult<List<ChatThread>> {
    if (shouldFail) {
      return NanoAIResult.recoverable(
        message = "Failed to load conversation history",
        cause = null,
        context = emptyMap(),
      )
    }
    return try {
      val threads = fakeConversationRepository.getAllThreads()
      NanoAIResult.success(threads)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to load conversation history",
        cause = e,
        context = emptyMap(),
      )
    }
  }
}
