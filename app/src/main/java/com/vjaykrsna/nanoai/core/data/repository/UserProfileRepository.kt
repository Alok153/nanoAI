package com.vjaykrsna.nanoai.core.data.repository

import com.vjaykrsna.nanoai.core.domain.model.uiux.LayoutSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UiPreferencesSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
import kotlinx.datetime.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user profile related operations.
 *
 * The repository exposes Flow-based observation for reactive UI updates and provides operations for
 * updating preferences, layouts, and UI state. Implementations should prefer offline-first
 * semantics, merging local cached data with remote sources when available.
 */
@Suppress("TooManyFunctions") // Repository pattern requires comprehensive API surface
interface UserProfileRepository {
  /**
   * Observe the merged user profile including DB and DataStore overlays. Emits null if no profile
   * exists for the given userId.
   */
  fun observeUserProfile(userId: String): Flow<UserProfile?>

  /**
   * Observe current offline status for the active user session. True indicates the app should
   * present offline affordances.
   */
  fun observeOfflineStatus(): Flow<Boolean>

  /** Get the current user profile in a one-shot call. Returns null if not found. */
  suspend fun getUserProfile(userId: String): UserProfile?

  /** Observe DataStore UiPreferences snapshot as Flow. */
  fun observePreferences(): Flow<UiPreferencesSnapshot>

  /** Update theme preference immediately (DataStore + DB persistent write). */
  suspend fun updateThemePreference(userId: String, themePreferenceName: String)

  /** Update visual density preference immediately. */
  suspend fun updateVisualDensity(userId: String, visualDensityName: String)

  /** Record onboarding completion (dismissed tips persisted). */
  suspend fun recordOnboardingProgress(
    userId: String,
    dismissedTips: Map<String, Boolean>,
    completed: Boolean
  )

  /** Update compact mode preference (and persisted visual density) for the user. */
  suspend fun updateCompactMode(userId: String, enabled: Boolean)

  /** Update pinned tools list (ordering enforced by repository). */
  suspend fun updatePinnedTools(userId: String, pinnedTools: List<String>)

  /** Save a layout snapshot. */
  suspend fun saveLayoutSnapshot(userId: String, layout: LayoutSnapshot, position: Int)

  /** Delete a layout snapshot. */
  suspend fun deleteLayoutSnapshot(layoutId: String)

  /** Observe UI state snapshot (session restoration) as Flow. */
  fun observeUIStateSnapshot(userId: String): Flow<UIStateSnapshot?>

    /** Persist whether the left drawer is open. */
    suspend fun updateLeftDrawerOpen(userId: String, open: Boolean)

    /** Persist right drawer open state and active panel identifier. */
    suspend fun updateRightDrawerState(userId: String, open: Boolean, panel: String?)

    /** Persist the current active mode navigation route. */
    suspend fun updateActiveModeRoute(userId: String, route: String)

    /** Persist command palette visibility. */
    suspend fun updateCommandPaletteVisibility(userId: String, visible: Boolean)

    /** Record a command palette action execution for recents. */
    suspend fun recordCommandPaletteRecent(commandId: String)

    /** Replace the stored command palette recents collection. */
    suspend fun setCommandPaletteRecents(commandIds: List<String>)

    /** Persist the last time the connectivity banner was dismissed. */
    suspend fun setConnectivityBannerDismissed(dismissedAt: Instant?)

  /** Debug/override hook for instrumentation tests to emulate offline mode. */
  suspend fun setOfflineOverride(isOffline: Boolean)
}
