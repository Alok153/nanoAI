package com.vjaykrsna.nanoai.core.domain.usecase

import android.database.sqlite.SQLiteException
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.repository.ConversationRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

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
    } catch (cancellationException: CancellationException) {
      throw cancellationException
    } catch (sqliteException: SQLiteException) {
      NanoAIResult.recoverable(
        message = "Failed to load conversation history",
        cause = sqliteException,
        context = emptyMap(),
      )
    } catch (ioException: IOException) {
      NanoAIResult.recoverable(
        message = "Failed to load conversation history",
        cause = ioException,
        context = emptyMap(),
      )
    } catch (illegalStateException: IllegalStateException) {
      NanoAIResult.recoverable(
        message = "Failed to load conversation history",
        cause = illegalStateException,
        context = emptyMap(),
      )
    }
  }
}
