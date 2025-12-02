package com.vjaykrsna.nanoai.core.domain.repository

import com.vjaykrsna.nanoai.core.domain.model.PersonaSwitchLog
import java.util.UUID

/**
 * Repository for persona switch logging.
 *
 * Tracks when users switch personas in threads for analytics and UI context.
 */
interface PersonaSwitchLogRepository {
  /** Log a persona switch event. */
  suspend fun logSwitch(log: PersonaSwitchLog)

  /** Get switch history for a thread. */
  suspend fun getSwitchHistory(threadId: UUID): List<PersonaSwitchLog>

  /** Get the most recent switch for a thread. */
  suspend fun getLatestSwitch(threadId: UUID): PersonaSwitchLog?

  /** Observe persona switch history for a specific thread. */
  suspend fun getLogsByThreadId(
    threadId: UUID
  ): kotlinx.coroutines.flow.Flow<List<PersonaSwitchLog>>
}
