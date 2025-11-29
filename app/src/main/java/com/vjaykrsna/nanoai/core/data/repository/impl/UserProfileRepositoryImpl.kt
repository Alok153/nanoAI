package com.vjaykrsna.nanoai.core.data.repository.impl

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.data.uiux.UserProfileLocalDataSource
import com.vjaykrsna.nanoai.core.domain.model.uiux.DataStoreUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.LayoutSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.core.domain.repository.UserProfileRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant

@Singleton
class UserProfileRepositoryImpl
@Inject
constructor(
  private val local: UserProfileLocalDataSource,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : UserProfileRepository {
  private val offlineState = MutableStateFlow(false)

  override fun observeUserProfile(userId: String): Flow<UserProfile?> =
    local.observeUserProfile(userId).onStart {
      // Ensure default profile exists on first access
      withContext(ioDispatcher) {
        if (local.getUserProfile(userId) == null) {
          val defaultProfile =
            UserProfile.fromPreferences(id = userId, preferences = DataStoreUiPreferences())
          local.saveUserProfile(defaultProfile)
        }
      }
    }

  override fun observeOfflineStatus(): Flow<Boolean> = offlineState.asStateFlow()

  override suspend fun setOfflineOverride(isOffline: Boolean) {
    withContext(ioDispatcher) { offlineState.emit(isOffline) }
  }

  override suspend fun getUserProfile(userId: String): UserProfile? =
    withContext(ioDispatcher) { local.getUserProfile(userId) }

  override fun observePreferences(): Flow<DataStoreUiPreferences> = local.observePreferences()

  override suspend fun updateThemePreference(userId: String, themePreferenceName: String) {
    val theme = ThemePreference.fromName(themePreferenceName)
    withContext(ioDispatcher) { local.updateThemePreference(userId, theme) }
  }

  override suspend fun updateVisualDensity(userId: String, visualDensityName: String) {
    val density =
      VisualDensity.values().firstOrNull { it.name.equals(visualDensityName, ignoreCase = true) }
        ?: VisualDensity.DEFAULT
    withContext(ioDispatcher) { local.updateVisualDensity(userId, density) }
  }

  override suspend fun updateCompactMode(userId: String, enabled: Boolean) {
    withContext(ioDispatcher) { local.updateCompactMode(userId, enabled) }
  }

  override suspend fun updatePinnedTools(userId: String, pinnedTools: List<String>) {
    val sanitizedPinnedTools = DataStoreUiPreferences(pinnedTools = pinnedTools).pinnedTools
    withContext(ioDispatcher) { local.updatePinnedTools(userId, sanitizedPinnedTools) }
  }

  override suspend fun saveLayoutSnapshot(userId: String, layout: LayoutSnapshot, position: Int) {
    withContext(ioDispatcher) { local.saveLayoutSnapshot(userId, layout, position) }
  }

  override suspend fun deleteLayoutSnapshot(layoutId: String) {
    withContext(ioDispatcher) { local.deleteLayoutSnapshot(layoutId) }
  }

  override fun observeUIStateSnapshot(userId: String): Flow<UIStateSnapshot?> =
    local.observeUIStateSnapshot(userId)

  override suspend fun updateLeftDrawerOpen(userId: String, open: Boolean) {
    withContext(ioDispatcher) { local.setLeftDrawerOpen(userId, open) }
  }

  override suspend fun updateRightDrawerState(userId: String, open: Boolean, panel: String?) {
    withContext(ioDispatcher) { local.setRightDrawerState(userId, open, panel) }
  }

  override suspend fun updateActiveModeRoute(userId: String, route: String) {
    withContext(ioDispatcher) { local.setActiveModeRoute(userId, route) }
  }

  override suspend fun updateCommandPaletteVisibility(userId: String, visible: Boolean) {
    withContext(ioDispatcher) { local.setCommandPaletteVisible(userId, visible) }
  }

  override suspend fun recordCommandPaletteRecent(commandId: String) {
    withContext(ioDispatcher) { local.recordCommandPaletteRecent(commandId) }
  }

  override suspend fun setCommandPaletteRecents(commandIds: List<String>) {
    withContext(ioDispatcher) { local.setCommandPaletteRecents(commandIds) }
  }

  override suspend fun setConnectivityBannerDismissed(dismissedAt: Instant?) {
    withContext(ioDispatcher) { local.setConnectivityBannerDismissed(dismissedAt?.toJavaInstant()) }
  }
}
