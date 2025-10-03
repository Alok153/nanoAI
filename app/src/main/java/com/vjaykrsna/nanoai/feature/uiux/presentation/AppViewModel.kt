package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.feature.uiux.domain.ObserveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Top-level application view model responsible for exposing global UI state such as theme
 * preferences, onboarding completion, and hydration status.
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
                    val onboardingCompleted = profile?.onboardingCompleted ?: false
                    val hydrating = profile == null && result.hydratedFromCache

                    _uiState.update {
                        it.copy(
                            themePreference = themePreference,
                            onboardingCompleted = onboardingCompleted,
                            isHydrating = hydrating,
                            offline = result.offline,
                        )
                    }
                }
            }
            // Timeout hydration after 5 seconds to prevent indefinite loading
            viewModelScope.launch {
                delay(5000)
                _uiState.update { it.copy(isHydrating = false) }
            }
        }
    }

/**
 * Global application UI state exposed from [AppViewModel].
 */
data class AppUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val onboardingCompleted: Boolean = false,
    val isHydrating: Boolean = true,
    val offline: Boolean = false,
) {
    val shouldShowWelcome: Boolean
        get() = !onboardingCompleted
}
