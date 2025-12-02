package com.vjaykrsna.nanoai.core.domain.repository

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Base repository interface for shell coordinator repositories.
 *
 * Used by repositories that back shell-level coordinators: [NavigationRepository],
 * [ConnectivityRepository], [ProgressRepository], and [ThemeRepository]. These repositories manage
 * cross-cutting state that the shell orchestrates.
 *
 * Feature-level repositories (e.g., `ConversationRepository`, `ModelCatalogRepository`) do NOT
 * extend this interface - they inject dispatchers directly and manage their own threading.
 */
interface BaseRepository {
  /**
   * IO dispatcher for repository operations. Implementations use this for database/network work.
   */
  val ioDispatcher: CoroutineDispatcher
}
