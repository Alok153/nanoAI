package com.vjaykrsna.nanoai.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreferenceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/** ViewModel responsible for coordinating the first-launch disclaimer experience. */
@HiltViewModel
class FirstLaunchDisclaimerViewModel
@Inject
constructor(
  private val privacyPreferenceStore: PrivacyPreferenceStore,
) : ViewModel() {
  /** UI state derived from the persisted privacy preferences. */
  val uiState: StateFlow<FirstLaunchDisclaimerUiState> =
    privacyPreferenceStore.privacyPreference
      .map { preference ->
        FirstLaunchDisclaimerUiState(
          shouldShowDialog = preference.consentAcknowledgedAt == null,
          disclaimerShownCount = preference.disclaimerShownCount,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = FirstLaunchDisclaimerUiState(shouldShowDialog = true),
      )

  fun onAcknowledge() {
    viewModelScope.launch {
      privacyPreferenceStore.incrementDisclaimerShown()
      privacyPreferenceStore.acknowledgeConsent(Clock.System.now())
    }
  }

  fun onDismiss() {
    viewModelScope.launch { privacyPreferenceStore.incrementDisclaimerShown() }
  }
}

/** Represents the state of the first launch disclaimer UI. */
data class FirstLaunchDisclaimerUiState(
  val shouldShowDialog: Boolean = false,
  val disclaimerShownCount: Int = 0,
)
