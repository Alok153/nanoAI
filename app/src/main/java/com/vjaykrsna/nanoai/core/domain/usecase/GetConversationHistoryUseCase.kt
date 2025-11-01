package com.vjaykrsna.nanoai.core.domain.usecase

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for retrieving conversation history.
 *
 * Provides synchronous access to all chat threads, wrapped in NanoAIResult for error handling.
 */
@Singleton
class GetConversationHistoryUseCase
@Inject
constructor(private val conversationRepository: ConversationRepository) {

  /**
   * Get all non-archived chat threads.
   *
   * @return NanoAIResult containing the list of chat threads or an error.
   */
  suspend operator fun invoke(): NanoAIResult<List<ChatThread>> {
    return try {
      val threads = conversationRepository.getAllThreads()
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
