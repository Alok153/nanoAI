package com.vjaykrsna.nanoai.feature.settings.presentation.state

import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceAuthState
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceDeviceAuthState
import com.vjaykrsna.nanoai.core.domain.settings.model.PrivacyPreference
import com.vjaykrsna.nanoai.shared.state.NanoAIViewState

/** Consolidated snapshot for the settings experience. */
data class SettingsUiState(
  val isLoading: Boolean = false,
  val apiProviders: List<APIProviderConfig> = emptyList(),
  val privacyPreference: PrivacyPreference = PrivacyPreference(),
  val themePreference: ThemePreference = ThemePreference.SYSTEM,
  val compactModeEnabled: Boolean = false,
  val highContrastEnabled: Boolean = false,
  val undoAvailable: Boolean = false,
  val statusMessage: String? = null,
  val showMigrationSuccessNotification: Boolean = false,
  val huggingFaceAuthState: HuggingFaceAuthState = HuggingFaceAuthState(),
  val huggingFaceDeviceAuthState: HuggingFaceDeviceAuthState? = null,
) : NanoAIViewState
