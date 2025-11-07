package com.vjaykrsna.nanoai.core.domain.repository

import com.vjaykrsna.nanoai.core.domain.model.uiux.LayoutSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UiPreferencesSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Repository interface for user profile related operations.
 *
 * The contract is composed of focused slices to avoid relying on large suppression blocks while
 * keeping a single aggregation type for DI consumers.
 */
interface UserProfileRepository :
  UserProfileObservationRepository,
  UserPreferenceWriteRepository,
  UserLayoutSnapshotRepository,
  UserInterfaceStateRepository,
  UserCommandPaletteRepository,
  UserConnectivityRepository

/** Observability-focused accessors for user profile and preference data. */
interface UserProfileObservationRepository {
  fun observeUserProfile(userId: String): Flow<UserProfile?>

  fun observeOfflineStatus(): Flow<Boolean>

  suspend fun getUserProfile(userId: String): UserProfile?

  fun observePreferences(): Flow<UiPreferencesSnapshot>

  fun observeUIStateSnapshot(userId: String): Flow<UIStateSnapshot?>
}

/** Preference mutations spanning both DataStore and Room-backed data. */
interface UserPreferenceWriteRepository {
  suspend fun updateThemePreference(userId: String, themePreferenceName: String)

  suspend fun updateVisualDensity(userId: String, visualDensityName: String)

  suspend fun updateCompactMode(userId: String, enabled: Boolean)

  suspend fun updatePinnedTools(userId: String, pinnedTools: List<String>)
}

/** Layout snapshot persistence helpers for restoring UI configurations. */
interface UserLayoutSnapshotRepository {
  suspend fun saveLayoutSnapshot(userId: String, layout: LayoutSnapshot, position: Int)

  suspend fun deleteLayoutSnapshot(layoutId: String)
}

/** UI state persistence hooks for drawers, mode routing, and palette visibility. */
interface UserInterfaceStateRepository {
  suspend fun updateLeftDrawerOpen(userId: String, open: Boolean)

  suspend fun updateRightDrawerState(userId: String, open: Boolean, panel: String?)

  suspend fun updateActiveModeRoute(userId: String, route: String)

  suspend fun updateCommandPaletteVisibility(userId: String, visible: Boolean)
}

/** Command palette recents coordination. */
interface UserCommandPaletteRepository {
  suspend fun recordCommandPaletteRecent(commandId: String)

  suspend fun setCommandPaletteRecents(commandIds: List<String>)
}

/** Connectivity and offline affordance helpers. */
interface UserConnectivityRepository {
  suspend fun setConnectivityBannerDismissed(dismissedAt: Instant?)

  suspend fun setOfflineOverride(isOffline: Boolean)
}
