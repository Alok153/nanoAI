package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.feature.uiux.domain.ObserveUserProfileUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.RecordOnboardingProgressUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.ToggleCompactModeUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.UIUX_DEFAULT_USER_ID
import com.vjaykrsna.nanoai.feature.uiux.domain.UpdateThemePreferenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val WELCOME_TOOLTIP_ID = "welcome_primary_tip"

@HiltViewModel
class WelcomeViewModel
    @Inject
    constructor(
        private val observeUserProfile: ObserveUserProfileUseCase,
        private val recordOnboardingProgress: RecordOnboardingProgressUseCase,
        private val updateThemePreference: UpdateThemePreferenceUseCase,
        private val toggleCompactMode: ToggleCompactModeUseCase,
        private val savedStateHandle: SavedStateHandle,
        private val analytics: WelcomeAnalytics = WelcomeAnalytics.NoOp,
        private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(WelcomeUiState())
        val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

        init {
            observeUserProfile.flow
                .onEach { result ->
                    _uiState.update { current ->
                        current.copy(
                            displayName = result.userProfile?.displayName,
                            showOnboarding = !(result.userProfile?.onboardingCompleted ?: false),
                            offline = result.offline,
                        )
                    }
                }.launchIn(viewModelScope)
        }

        fun onGetStarted() {
            analytics.track("welcome_get_started")
            markOnboardingCompleted()
        }

        fun onExploreFeatures() {
            analytics.track("welcome_explore_features")
        }

        fun onSkip() {
            analytics.track("welcome_skip")
            _uiState.update { it.copy(skipEnabled = false) }
            recordOnboardingProgress.recordDismissal(tipId = null, dismissed = false, completed = true)
        }

        fun onThemeSelected(themeName: String) {
            viewModelScope.launch(dispatcher) {
                updateThemePreference.updateTheme(
                    com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
                        .fromName(themeName),
                )
            }
        }

        fun onCompactModeToggled(enabled: Boolean) {
            viewModelScope.launch(dispatcher) {
                toggleCompactMode.toggle(enabled)
            }
        }

        fun onTooltipDismiss() {
            analytics.track("welcome_tooltip_dismiss")
            _uiState.update { it.copy(showOnboarding = false) }
            recordOnboardingProgress.recordDismissal(WELCOME_TOOLTIP_ID, dismissed = false, completed = false)
        }

        fun onTooltipDontShowAgain() {
            analytics.track("welcome_tooltip_dont_show")
            _uiState.update { it.copy(showOnboarding = false) }
            recordOnboardingProgress.recordDismissal(WELCOME_TOOLTIP_ID, dismissed = true, completed = false)
        }

        fun onTooltipHelp() {
            analytics.track("welcome_tooltip_help")
        }

        private fun markOnboardingCompleted() {
            _uiState.update { it.copy(skipEnabled = false, showOnboarding = false) }
            recordOnboardingProgress.recordDismissal(tipId = null, dismissed = false, completed = true, userId = UIUX_DEFAULT_USER_ID)
        }
    }

interface WelcomeAnalytics {
    fun track(event: String)

    object NoOp : WelcomeAnalytics {
        override fun track(event: String) = Unit
    }
}

data class WelcomeUiState(
    val displayName: String? = null,
    val showOnboarding: Boolean = true,
    val skipEnabled: Boolean = true,
    val offline: Boolean = false,
)
