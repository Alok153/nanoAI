package com.vjaykrsna.nanoai.core.domain.usecase

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.core.domain.repository.UiPreferencesRepository
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
  private val repository: UiPreferencesRepository,
  @IoDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  /** Updates theme preference. */
  suspend fun setThemePreference(themePreference: ThemePreference) =
    repository.setThemePreference(themePreference)

  /** Updates visual density. */
  suspend fun setVisualDensity(visualDensity: VisualDensity) =
    repository.setVisualDensity(visualDensity)

  /** Updates high contrast enabled. */
  @OneShot("Persist high contrast preference toggle")
  suspend fun setHighContrastEnabled(enabled: Boolean): NanoAIResult<Unit> =
    withContext(dispatcher) {
      runCatching { repository.setHighContrastEnabled(enabled) }
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
    repository.setPinnedToolIds(pinnedToolIds)

  /** Adds a pinned tool. */
  suspend fun addPinnedTool(toolId: String) = repository.addPinnedTool(toolId)

  /** Removes a pinned tool. */
  suspend fun removePinnedTool(toolId: String) = repository.removePinnedTool(toolId)

  /** Reorders pinned tools. */
  suspend fun reorderPinnedTools(orderedToolIds: List<String>) =
    repository.reorderPinnedTools(orderedToolIds)

  /** Sets command palette recents. */
  suspend fun setCommandPaletteRecents(commandIds: List<String>) =
    repository.setCommandPaletteRecents(commandIds)

  /** Records a command palette recent. */
  suspend fun recordCommandPaletteRecent(commandId: String) =
    repository.recordCommandPaletteRecent(commandId)

  /** Sets connectivity banner dismissed. */
  suspend fun setConnectivityBannerDismissed(dismissedAt: Instant?) =
    repository.setConnectivityBannerDismissed(dismissedAt)
}
