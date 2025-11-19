package com.vjaykrsna.nanoai.core.domain.usecase

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferencesStore
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

/**
 * Use case for updating UI preferences.
 *
 * Provides a clean domain interface for UI preference updates, abstracting the store implementation
 * details.
 */
class UpdateUiPreferencesUseCase
@Inject
constructor(
  private val uiPreferencesStore: UiPreferencesStore,
  @IoDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  /** Updates theme preference. */
  suspend fun setThemePreference(themePreference: ThemePreference) =
    uiPreferencesStore.setThemePreference(themePreference)

  /** Updates visual density. */
  suspend fun setVisualDensity(visualDensity: VisualDensity) =
    uiPreferencesStore.setVisualDensity(visualDensity)

  /** Updates high contrast enabled. */
  @OneShot("Persist high contrast preference toggle")
  suspend fun setHighContrastEnabled(enabled: Boolean): NanoAIResult<Unit> =
    withContext(dispatcher) {
      runCatching { uiPreferencesStore.setHighContrastEnabled(enabled) }
        .fold(
          onSuccess = { NanoAIResult.success(Unit) },
          onFailure = {
            NanoAIResult.recoverable(
              message = "Failed to update high contrast preference",
              cause = it,
              context = mapOf("highContrastEnabled" to enabled.toString()),
            )
          },
        )
    }

  /** Sets pinned tool IDs. */
  suspend fun setPinnedToolIds(pinnedToolIds: List<String>) =
    uiPreferencesStore.setPinnedToolIds(pinnedToolIds)

  /** Adds a pinned tool. */
  suspend fun addPinnedTool(toolId: String) = uiPreferencesStore.addPinnedTool(toolId)

  /** Removes a pinned tool. */
  suspend fun removePinnedTool(toolId: String) = uiPreferencesStore.removePinnedTool(toolId)

  /** Reorders pinned tools. */
  suspend fun reorderPinnedTools(orderedToolIds: List<String>) =
    uiPreferencesStore.reorderPinnedTools(orderedToolIds)

  /** Sets command palette recents. */
  suspend fun setCommandPaletteRecents(commandIds: List<String>) =
    uiPreferencesStore.setCommandPaletteRecents(commandIds)

  /** Records a command palette recent. */
  suspend fun recordCommandPaletteRecent(commandId: String) =
    uiPreferencesStore.recordCommandPaletteRecent(commandId)

  /** Sets connectivity banner dismissed. */
  suspend fun setConnectivityBannerDismissed(dismissedAt: Instant?) =
    uiPreferencesStore.setConnectivityBannerDismissed(dismissedAt)
}
