package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.feature.uiux.data.ShellStateRepository
import com.vjaykrsna.nanoai.feature.uiux.state.UiPreferenceSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel responsible for theme and UI preference management. */
@HiltViewModel
class ThemeViewModel
@Inject
constructor(
  private val repository: ShellStateRepository,
  @MainImmediateDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
  /** Current UI preferences including theme, density, and other display settings. */
  val uiPreferences: StateFlow<UiPreferenceSnapshot> =
    repository.uiPreferenceSnapshot.stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = UiPreferenceSnapshot(),
    )

  /** Updates the theme preference for the active user. */
  fun updateThemePreference(theme: ThemePreference) {
    viewModelScope.launch(dispatcher) { repository.updateThemePreference(theme) }
  }

  /** Updates the visual density preference for the active user. */
  fun updateVisualDensity(density: VisualDensity) {
    viewModelScope.launch(dispatcher) { repository.updateVisualDensity(density) }
  }
}
