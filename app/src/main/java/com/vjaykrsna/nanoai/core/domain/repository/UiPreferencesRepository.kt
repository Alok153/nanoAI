package com.vjaykrsna.nanoai.core.domain.repository

import com.vjaykrsna.nanoai.core.domain.model.uiux.DataStoreUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Abstraction over UI preference persistence.
 *
 * Exposes reactive access to the current `DataStoreUiPreferences` snapshot along with mutation
 * helpers that encapsulate the underlying DataStore implementation.
 */
interface UiPreferencesRepository {
  /** Stream of normalized UI preferences. */
  val preferences: Flow<DataStoreUiPreferences>

  suspend fun setThemePreference(themePreference: ThemePreference)

  suspend fun setVisualDensity(visualDensity: VisualDensity)

  suspend fun setHighContrastEnabled(enabled: Boolean)

  suspend fun setPinnedToolIds(pinnedToolIds: List<String>)

  suspend fun addPinnedTool(toolId: String)

  suspend fun removePinnedTool(toolId: String)

  suspend fun reorderPinnedTools(orderedToolIds: List<String>)

  suspend fun setCommandPaletteRecents(commandIds: List<String>)

  suspend fun recordCommandPaletteRecent(commandId: String)

  suspend fun setConnectivityBannerDismissed(dismissedAt: Instant?)
}
