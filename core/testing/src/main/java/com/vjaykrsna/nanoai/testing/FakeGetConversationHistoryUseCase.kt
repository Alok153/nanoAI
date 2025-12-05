package com.vjaykrsna.nanoai.testing

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

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
    } catch (cancellationException: CancellationException) {
      throw cancellationException
    } catch (illegalStateException: IllegalStateException) {
      NanoAIResult.recoverable(
        message = "Failed to load conversation history",
        cause = illegalStateException,
        context = emptyMap(),
      )
    } catch (illegalArgumentException: IllegalArgumentException) {
      NanoAIResult.recoverable(
        message = "Failed to load conversation history",
        cause = illegalArgumentException,
        context = emptyMap(),
      )
    }
  }
}
