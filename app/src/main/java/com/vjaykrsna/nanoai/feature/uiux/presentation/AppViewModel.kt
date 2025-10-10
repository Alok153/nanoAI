package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.feature.uiux.domain.ObserveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Top-level application view model responsible for exposing global UI state such as theme
 * preferences and hydration status.
 */
@HiltViewModel
class AppViewModel
@Inject
constructor(
  observeUserProfileUseCase: ObserveUserProfileUseCase,
) : ViewModel() {
  private val _uiState = MutableStateFlow(AppUiState())
  val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      observeUserProfileUseCase.flow.collect { result ->
        val profile = result.userProfile
        val themePreference = profile?.themePreference ?: ThemePreference.SYSTEM
        val hydrating = profile == null && result.hydratedFromCache

        _uiState.update {
          it.copy(
            themePreference = themePreference,
            isHydrating = hydrating,
            offline = result.offline,
          )
        }
      }
    }
  }
}

/** Global application UI state exposed from [AppViewModel]. */
data class AppUiState(
  val themePreference: ThemePreference = ThemePreference.SYSTEM,
  val isHydrating: Boolean = true,
  val offline: Boolean = false,
)
