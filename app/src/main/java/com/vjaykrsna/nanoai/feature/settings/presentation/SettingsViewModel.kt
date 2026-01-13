package com.vjaykrsna.nanoai.feature.settings.presentation

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.ProviderCredentialMutation
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.settings.ApiProviderConfigUseCase
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceAuthCoordinator
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceOAuthConfig
import com.vjaykrsna.nanoai.core.domain.settings.model.RetentionPolicy
import com.vjaykrsna.nanoai.core.domain.uiux.ObserveUserProfileUseCase
import com.vjaykrsna.nanoai.core.domain.uiux.SettingsOperationsUseCase
import com.vjaykrsna.nanoai.core.domain.uiux.ToggleCompactModeUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.ObservePrivacyPreferencesUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.ObserveUiPreferencesUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.UpdatePrivacyPreferencesUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.UpdateUiPreferencesUseCase
import com.vjaykrsna.nanoai.feature.settings.domain.BackupUseCase
import com.vjaykrsna.nanoai.feature.settings.presentation.model.SettingsUiEvent
import com.vjaykrsna.nanoai.feature.settings.presentation.state.SettingsUiState
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHost
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.datetime.Clock

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
  private val apiProviderConfigUseCase: ApiProviderConfigUseCase,
  private val backupUseCase: BackupUseCase,
  private val observePrivacyPreferencesUseCase: ObservePrivacyPreferencesUseCase,
  private val observeUiPreferencesUseCase: ObserveUiPreferencesUseCase,
  private val updatePrivacyPreferencesUseCase: UpdatePrivacyPreferencesUseCase,
  private val updateUiPreferencesUseCase: UpdateUiPreferencesUseCase,
  private val observeUserProfileUseCase: ObserveUserProfileUseCase,
  private val settingsOperationsUseCase: SettingsOperationsUseCase,
  private val toggleCompactModeUseCase: ToggleCompactModeUseCase,
  private val huggingFaceAuthCoordinator: HuggingFaceAuthCoordinator,
  private val huggingFaceOAuthConfig: HuggingFaceOAuthConfig,
  @MainImmediateDispatcher dispatcher: CoroutineDispatcher,
) :
  ViewModelStateHost<SettingsUiState, SettingsUiEvent>(
    initialState = SettingsUiState(),
    dispatcher = dispatcher,
  ) {

  private val observers =
    SettingsStateObservers(
      scope = viewModelScope,
      dependencies =
        SettingsObserverDependencies(
          apiProviderConfigUseCase = apiProviderConfigUseCase,
          observePrivacyPreferencesUseCase = observePrivacyPreferencesUseCase,
          observeUiPreferencesUseCase = observeUiPreferencesUseCase,
          observeUserProfileUseCase = observeUserProfileUseCase,
          huggingFaceAuthCoordinator = huggingFaceAuthCoordinator,
        ),
      updateState = { reducer -> updateState(reducer) },
    )

  private val providerActions =
    SettingsApiProviderActions(
      scope = viewModelScope,
      apiProviderConfigUseCase = apiProviderConfigUseCase,
      setLoading = ::setLoading,
      emitError = ::emitError,
    )

  private val backupActions =
    SettingsBackupActions(
      scope = viewModelScope,
      backupUseCase = backupUseCase,
      setLoading = ::setLoading,
      emitEvent = { event -> emitEvent(event) },
      emitError = ::emitError,
    )

  private val privacyActions =
    SettingsPrivacyActions(
      scope = viewModelScope,
      updatePrivacyPreferencesUseCase = updatePrivacyPreferencesUseCase,
      emitError = ::emitError,
      clock = Clock.System,
    )

  private val uiPreferenceActions =
    SettingsUiPreferenceActions(
      scope = viewModelScope,
      state = state,
      updateState = { reducer -> updateState(reducer) },
      dependencies =
        SettingsUiPreferenceDependencies(
          settingsOperationsUseCase = settingsOperationsUseCase,
          toggleCompactModeUseCase = toggleCompactModeUseCase,
          updateUiPreferencesUseCase = updateUiPreferencesUseCase,
          emitError = { envelope -> emitError(envelope) },
        ),
    )

  private val huggingFaceActions =
    SettingsHuggingFaceActions(
      scope = viewModelScope,
      huggingFaceAuthCoordinator = huggingFaceAuthCoordinator,
      huggingFaceOAuthConfig = huggingFaceOAuthConfig,
      updateState = { reducer -> updateState(reducer) },
      emitError = ::emitError,
    )

  init {
    observers.start()
  }

  fun addApiProvider(config: APIProviderConfig, credential: ProviderCredentialMutation) =
    providerActions.add(config, credential)

  fun updateApiProvider(config: APIProviderConfig, credential: ProviderCredentialMutation) =
    providerActions.update(config, credential)

  fun deleteApiProvider(providerId: String) = providerActions.delete(providerId)

  fun exportBackup(destinationPath: String, includeChatHistory: Boolean = false) =
    backupActions.exportBackup(destinationPath, includeChatHistory)

  fun importBackup(uri: Uri) = backupActions.importBackup(uri)

  fun setTelemetryOptIn(optIn: Boolean) = privacyActions.setTelemetryOptIn(optIn)

  fun acknowledgeConsent() = privacyActions.acknowledgeConsent()

  fun setRetentionPolicy(policy: RetentionPolicy) = privacyActions.setRetentionPolicy(policy)

  fun dismissExportWarnings() = privacyActions.dismissExportWarnings()

  fun setThemePreference(themePreference: ThemePreference) =
    uiPreferenceActions.setThemePreference(themePreference)

  fun applyDensityPreference(compactModeEnabled: Boolean) =
    uiPreferenceActions.applyDensityPreference(compactModeEnabled)

  fun setCompactMode(enabled: Boolean) = uiPreferenceActions.setCompactMode(enabled)

  fun setHighContrastEnabled(enabled: Boolean) = uiPreferenceActions.setHighContrastEnabled(enabled)

  fun undoUiPreferenceChange() = uiPreferenceActions.undoUiPreferenceChange()

  fun saveHuggingFaceApiKey(apiKey: String) = huggingFaceActions.saveApiKey(apiKey)

  fun refreshHuggingFaceAccount() = huggingFaceActions.refreshAccount()

  fun startHuggingFaceOAuthLogin() = huggingFaceActions.startOAuthLogin()

  fun cancelHuggingFaceOAuthLogin() = huggingFaceActions.cancelOAuthLogin()

  fun disconnectHuggingFaceAccount() = huggingFaceActions.disconnectAccount()

  fun onMigrationSuccess() {
    updateState { copy(showMigrationSuccessNotification = true) }
  }

  fun dismissMigrationSuccessNotification() {
    updateState { copy(showMigrationSuccessNotification = false) }
  }

  fun clearStatusMessage() {
    updateState { copy(statusMessage = null) }
  }

  fun clearErrorMessage() {
    updateState { copy(lastErrorMessage = null) }
  }

  private suspend fun emitError(envelope: NanoAIErrorEnvelope) {
    updateState { copy(lastErrorMessage = envelope.userMessage) }
    emitEvent(SettingsUiEvent.ErrorRaised(envelope))
  }

  private fun setLoading(isLoading: Boolean) {
    updateState { copy(isLoading = isLoading) }
  }
}
