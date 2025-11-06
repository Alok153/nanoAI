package com.vjaykrsna.nanoai.core.domain.repository

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Base repository interface that provides common functionality for all repositories.
 *
 * Ensures consistent coroutine dispatcher injection and provides a foundation for repository
 * abstraction patterns across the application.
 */
interface BaseRepository {
  /**
   * The IO dispatcher for performing repository operations off the main thread. All repository
   * implementations should use this dispatcher for database and network operations.
   */
  val ioDispatcher: CoroutineDispatcher
}
