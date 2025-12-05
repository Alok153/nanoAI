package com.vjaykrsna.nanoai.core.data.preferences

import com.vjaykrsna.nanoai.core.domain.model.uiux.DataStoreUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.core.domain.repository.UiPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

/** Data-layer implementation that bridges [UiPreferencesStore] to the domain contract. */
@Singleton
class UiPreferencesRepositoryImpl
@Inject
constructor(private val uiPreferencesStore: UiPreferencesStore) : UiPreferencesRepository {
  override val preferences: Flow<DataStoreUiPreferences> =
    uiPreferencesStore.uiPreferences.map { prefs -> prefs.toDomainSnapshot() }

  override suspend fun setThemePreference(themePreference: ThemePreference) {
    uiPreferencesStore.setThemePreference(themePreference)
  }

  override suspend fun setVisualDensity(visualDensity: VisualDensity) {
    uiPreferencesStore.setVisualDensity(visualDensity)
  }

  override suspend fun setHighContrastEnabled(enabled: Boolean) {
    uiPreferencesStore.setHighContrastEnabled(enabled)
  }

  override suspend fun setPinnedToolIds(pinnedToolIds: List<String>) {
    uiPreferencesStore.setPinnedToolIds(pinnedToolIds)
  }

  override suspend fun addPinnedTool(toolId: String) {
    uiPreferencesStore.addPinnedTool(toolId)
  }

  override suspend fun removePinnedTool(toolId: String) {
    uiPreferencesStore.removePinnedTool(toolId)
  }

  override suspend fun reorderPinnedTools(orderedToolIds: List<String>) {
    uiPreferencesStore.reorderPinnedTools(orderedToolIds)
  }

  override suspend fun setCommandPaletteRecents(commandIds: List<String>) {
    uiPreferencesStore.setCommandPaletteRecents(commandIds)
  }

  override suspend fun recordCommandPaletteRecent(commandId: String) {
    uiPreferencesStore.recordCommandPaletteRecent(commandId)
  }

  override suspend fun setConnectivityBannerDismissed(dismissedAt: Instant?) {
    uiPreferencesStore.setConnectivityBannerDismissed(dismissedAt)
  }
}
