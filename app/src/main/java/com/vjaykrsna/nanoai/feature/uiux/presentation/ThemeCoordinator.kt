package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.core.domain.uiux.ThemeOperationsUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Coordinator responsible for theme and UI preference management.
 *
 * This is a regular injectable class (not a ViewModel) because it's composed into ShellViewModel
 * rather than used independently with ViewModelProvider.
 */
@Singleton
class ThemeCoordinator
@Inject
constructor(
  private val themeOperationsUseCase: ThemeOperationsUseCase,
  @MainImmediateDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {
  /** Current UI preferences including theme, density, and other display settings. */
  fun uiPreferences(scope: CoroutineScope): StateFlow<ShellUiPreferences> =
    themeOperationsUseCase
      .observeUiPreferences()
      .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = ShellUiPreferences())

  /** Updates the theme preference for the active user. */
  fun updateThemePreference(scope: CoroutineScope, theme: ThemePreference) {
    scope.launch(dispatcher) { themeOperationsUseCase.updateTheme(theme) }
  }

  /** Updates the visual density preference for the active user. */
  fun updateVisualDensity(scope: CoroutineScope, density: VisualDensity) {
    scope.launch(dispatcher) { themeOperationsUseCase.updateVisualDensity(density) }
  }

  /** Updates the high contrast enabled preference for the active user. */
  fun updateHighContrastEnabled(scope: CoroutineScope, enabled: Boolean) {
    scope.launch(dispatcher) { themeOperationsUseCase.updateHighContrastEnabled(enabled) }
  }
}
