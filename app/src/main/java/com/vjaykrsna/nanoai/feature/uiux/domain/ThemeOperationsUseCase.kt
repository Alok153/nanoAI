package com.vjaykrsna.nanoai.feature.uiux.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.repository.ThemeRepository
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.feature.uiux.presentation.UiPreferenceSnapshot
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use case for theme and UI preference operations.
 *
 * Provides clean domain interface for theme-related operations, wrapping ThemeRepository.
 */
class ThemeOperationsUseCase @Inject constructor(private val themeRepository: ThemeRepository) {

  /** Observes UI preference snapshot with reactive updates. */
  fun observeUiPreferences(): Flow<UiPreferenceSnapshot> = themeRepository.uiPreferenceSnapshot

  /** Updates theme preference. */
  suspend fun updateTheme(theme: ThemePreference): NanoAIResult<Unit> {
    return try {
      themeRepository.updateThemePreference(theme)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to update theme",
        cause = e,
        context = mapOf("theme" to theme.name),
      )
    }
  }

  /** Updates visual density. */
  suspend fun updateVisualDensity(density: VisualDensity): NanoAIResult<Unit> {
    return try {
      themeRepository.updateVisualDensity(density)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to update visual density",
        cause = e,
        context = mapOf("density" to density.name),
      )
    }
  }

  /** Updates high contrast enabled. */
  suspend fun updateHighContrastEnabled(enabled: Boolean): NanoAIResult<Unit> {
    return try {
      themeRepository.updateHighContrastEnabled(enabled)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to update high contrast",
        cause = e,
        context = mapOf("enabled" to enabled.toString()),
      )
    }
  }
}
