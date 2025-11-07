package com.vjaykrsna.nanoai.feature.settings.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.onFailure
import com.vjaykrsna.nanoai.core.common.onSuccess
import com.vjaykrsna.nanoai.core.domain.library.ModelDownloadsAndExportUseCase
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.settings.ApiProviderConfigUseCase
import com.vjaykrsna.nanoai.core.domain.settings.ImportService
import com.vjaykrsna.nanoai.core.domain.settings.ImportSummary
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceAuthCoordinator
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceAuthState
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceDeviceAuthState
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceOAuthConfig
import com.vjaykrsna.nanoai.core.domain.settings.model.RetentionPolicy
import com.vjaykrsna.nanoai.core.domain.uiux.ObserveUserProfileUseCase
import com.vjaykrsna.nanoai.core.domain.uiux.SettingsOperationsUseCase
import com.vjaykrsna.nanoai.core.domain.uiux.ToggleCompactModeUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.ObservePrivacyPreferencesUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.ObserveUiPreferencesUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.UpdatePrivacyPreferencesUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.UpdateUiPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.jvm.JvmName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
  private val apiProviderConfigUseCase: ApiProviderConfigUseCase,
  private val modelDownloadsAndExportUseCase: ModelDownloadsAndExportUseCase,
  private val observePrivacyPreferencesUseCase: ObservePrivacyPreferencesUseCase,
  private val observeUiPreferencesUseCase: ObserveUiPreferencesUseCase,
  private val updatePrivacyPreferencesUseCase: UpdatePrivacyPreferencesUseCase,
  private val updateUiPreferencesUseCase: UpdateUiPreferencesUseCase,
  private val importService: ImportService,
  private val observeUserProfileUseCase: ObserveUserProfileUseCase,
  private val settingsOperationsUseCase: SettingsOperationsUseCase,
  private val toggleCompactModeUseCase: ToggleCompactModeUseCase,
  private val huggingFaceAuthCoordinator: HuggingFaceAuthCoordinator,
  private val huggingFaceOAuthConfig: HuggingFaceOAuthConfig,
) : ViewModel() {
  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorEvents = MutableSharedFlow<SettingsError>()
  val errorEvents = _errorEvents.asSharedFlow()

  private val _exportSuccess = MutableSharedFlow<String>()
  val exportSuccess = _exportSuccess.asSharedFlow()

  private val _importSuccess = MutableSharedFlow<ImportSummary>()
  val importSuccess = _importSuccess.asSharedFlow()

  private val _uiUxState = MutableStateFlow(SettingsUiUxState())
  val uiUxState: StateFlow<SettingsUiUxState> = _uiUxState.asStateFlow()

  private val apiProviderController =
    ApiProviderController(
      scope = viewModelScope,
      apiProviderConfigUseCase = apiProviderConfigUseCase,
      isLoading = _isLoading,
      errorEvents = _errorEvents,
    )

  private val backupSignals =
    BackupSignals(
      isLoading = _isLoading,
      exportSuccess = _exportSuccess,
      importSuccess = _importSuccess,
      errorEvents = _errorEvents,
    )

  private val backupController =
    BackupController(
      scope = viewModelScope,
      modelDownloadsAndExportUseCase = modelDownloadsAndExportUseCase,
      importService = importService,
      signals = backupSignals,
    )

  private val privacyPreferenceController =
    PrivacyPreferenceController(
      scope = viewModelScope,
      updatePrivacyPreferencesUseCase = updatePrivacyPreferencesUseCase,
      errorEvents = _errorEvents,
    )

  private val uiPreferenceController =
    UiPreferenceController(
      scope = viewModelScope,
      uiUxState = _uiUxState,
      settingsOperationsUseCase = settingsOperationsUseCase,
      toggleCompactModeUseCase = toggleCompactModeUseCase,
      updateUiPreferencesUseCase = updateUiPreferencesUseCase,
    )

  private val huggingFaceController =
    HuggingFaceAuthController(
      scope = viewModelScope,
      coordinator = huggingFaceAuthCoordinator,
      oauthConfig = huggingFaceOAuthConfig,
      uiUxState = _uiUxState,
      errorEvents = _errorEvents,
    )

  val huggingFaceAuthState: StateFlow<HuggingFaceAuthState> = huggingFaceAuthCoordinator.state

  val huggingFaceDeviceAuthState: StateFlow<HuggingFaceDeviceAuthState?> =
    huggingFaceAuthCoordinator.deviceAuthState

  val apiProviders: StateFlow<List<APIProviderConfig>> =
    apiProviderConfigUseCase
      .observeAllProviders()
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val privacyPreferences:
    StateFlow<com.vjaykrsna.nanoai.core.domain.settings.model.PrivacyPreference> =
    observePrivacyPreferencesUseCase()
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        com.vjaykrsna.nanoai.core.domain.settings.model.PrivacyPreference(
          exportWarningsDismissed = false,
          telemetryOptIn = false,
          consentAcknowledgedAt = null,
          disclaimerShownCount = 0,
          retentionPolicy = RetentionPolicy.INDEFINITE,
        ),
      )

  val uiPreferences: StateFlow<com.vjaykrsna.nanoai.core.data.preferences.UiPreferences> =
    observeUiPreferencesUseCase()
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        com.vjaykrsna.nanoai.core.data.preferences.UiPreferences(),
      )

  init {
    observeUserProfileUseCase.flow
      .onEach { result ->
        val profile = result.userProfile
        _uiUxState.update { current ->
          current.copy(
            themePreference = profile?.themePreference ?: ThemePreference.SYSTEM,
            compactModeEnabled = profile?.compactMode ?: false,
            undoAvailable = uiPreferenceController.hasPendingUndo,
          )
        }
      }
      .launchIn(viewModelScope)

    uiPreferences
      .onEach { prefs ->
        _uiUxState.update { current ->
          current.copy(highContrastEnabled = prefs.highContrastEnabled)
        }
      }
      .launchIn(viewModelScope)

    huggingFaceAuthCoordinator.deviceAuthState
      .map { deviceState ->
        val announcement = deviceState?.lastErrorAnnouncement
        when {
          !announcement.isNullOrBlank() -> announcement
          !deviceState?.lastError.isNullOrBlank() -> deviceState?.lastError
          else -> null
        }
      }
      .distinctUntilChanged()
      .onEach { announcement ->
        if (announcement != null) {
          _uiUxState.update { current -> current.copy(statusMessage = announcement) }
        }
      }
      .launchIn(viewModelScope)
  }

  fun addApiProvider(config: APIProviderConfig) = apiProviderController.add(config)

  fun updateApiProvider(config: APIProviderConfig) = apiProviderController.update(config)

  fun deleteApiProvider(providerId: String) = apiProviderController.delete(providerId)

  fun exportBackup(destinationPath: String, includeChatHistory: Boolean = false) =
    backupController.export(destinationPath, includeChatHistory)

  fun importBackup(uri: Uri) = backupController.import(uri)

  fun setTelemetryOptIn(optIn: Boolean) = privacyPreferenceController.setTelemetryOptIn(optIn)

  fun acknowledgeConsent() = privacyPreferenceController.acknowledgeConsent()

  fun setRetentionPolicy(policy: RetentionPolicy) =
    privacyPreferenceController.setRetentionPolicy(policy)

  fun dismissExportWarnings() = privacyPreferenceController.dismissExportWarnings()

  fun setThemePreference(themePreference: ThemePreference) =
    uiPreferenceController.setThemePreference(themePreference)

  fun applyDensityPreference(compactModeEnabled: Boolean) =
    uiPreferenceController.applyDensityPreference(compactModeEnabled)

  @JvmName("internalSetLayoutMode")
  fun setCompactMode(enabled: Boolean) = uiPreferenceController.setCompactMode(enabled)

  fun setHighContrastEnabled(enabled: Boolean) =
    uiPreferenceController.setHighContrastEnabled(enabled)

  fun undoUiPreferenceChange() = uiPreferenceController.undo()

  fun onMigrationSuccess() = uiPreferenceController.onMigrationSuccess()

  fun dismissMigrationSuccessNotification() =
    uiPreferenceController.dismissMigrationSuccessNotification()

  fun saveHuggingFaceApiKey(apiKey: String) = huggingFaceController.saveApiKey(apiKey)

  fun refreshHuggingFaceAccount() = huggingFaceController.refreshAccount()

  fun startHuggingFaceOAuthLogin() = huggingFaceController.startOAuthLogin()

  fun cancelHuggingFaceOAuthLogin() = huggingFaceController.cancelOAuth()

  fun disconnectHuggingFaceAccount() = huggingFaceController.disconnect()

  fun clearStatusMessage() = uiPreferenceController.clearStatusMessage()
}

sealed class SettingsError {
  data class ProviderAddFailed(val message: String) : SettingsError()

  data class ProviderUpdateFailed(val message: String) : SettingsError()

  data class ProviderDeleteFailed(val message: String) : SettingsError()

  data class ExportFailed(val message: String) : SettingsError()

  data class ImportFailed(val message: String) : SettingsError()

  data class PreferenceUpdateFailed(val message: String) : SettingsError()

  data class UnexpectedError(val message: String) : SettingsError()

  data class HuggingFaceAuthFailed(val message: String) : SettingsError()
}

data class SettingsUiUxState(
  val themePreference: ThemePreference = ThemePreference.SYSTEM,
  val compactModeEnabled: Boolean = false,
  val highContrastEnabled: Boolean = false,
  val undoAvailable: Boolean = false,
  val statusMessage: String? = null,
  val showMigrationSuccessNotification: Boolean = false,
)

private class ApiProviderController(
  private val scope: CoroutineScope,
  private val apiProviderConfigUseCase: ApiProviderConfigUseCase,
  private val isLoading: MutableStateFlow<Boolean>,
  private val errorEvents: MutableSharedFlow<SettingsError>,
) {
  fun add(config: APIProviderConfig) {
    scope.launch {
      isLoading.value = true
      runCatching { apiProviderConfigUseCase.addProvider(config) }
        .onSuccess { result ->
          result.onFailure { error ->
            errorEvents.emit(
              SettingsError.ProviderAddFailed(error.message ?: "Failed to add provider")
            )
          }
        }
        .onFailure { throwable ->
          errorEvents.emit(
            SettingsError.ProviderAddFailed(throwable.message ?: "Failed to add provider")
          )
        }
      isLoading.value = false
    }
  }

  fun update(config: APIProviderConfig) {
    scope.launch {
      isLoading.value = true
      runCatching { apiProviderConfigUseCase.updateProvider(config) }
        .onSuccess { result ->
          result.onFailure { error ->
            errorEvents.emit(
              SettingsError.ProviderUpdateFailed(error.message ?: "Failed to update provider")
            )
          }
        }
        .onFailure { throwable ->
          errorEvents.emit(
            SettingsError.ProviderUpdateFailed(throwable.message ?: "Failed to update provider")
          )
        }
      isLoading.value = false
    }
  }

  fun delete(providerId: String) {
    scope.launch {
      isLoading.value = true
      runCatching { apiProviderConfigUseCase.deleteProvider(providerId) }
        .onSuccess { result ->
          result.onFailure { error ->
            errorEvents.emit(
              SettingsError.ProviderDeleteFailed(error.message ?: "Failed to delete provider")
            )
          }
        }
        .onFailure { throwable ->
          errorEvents.emit(
            SettingsError.ProviderDeleteFailed(throwable.message ?: "Failed to delete provider")
          )
        }
      isLoading.value = false
    }
  }
}

private data class BackupSignals(
  val isLoading: MutableStateFlow<Boolean>,
  val exportSuccess: MutableSharedFlow<String>,
  val importSuccess: MutableSharedFlow<ImportSummary>,
  val errorEvents: MutableSharedFlow<SettingsError>,
)

private class BackupController(
  private val scope: CoroutineScope,
  private val modelDownloadsAndExportUseCase: ModelDownloadsAndExportUseCase,
  private val importService: ImportService,
  private val signals: BackupSignals,
) {
  fun export(destinationPath: String, includeChatHistory: Boolean) {
    scope.launch {
      signals.isLoading.value = true
      modelDownloadsAndExportUseCase
        .exportBackup(destinationPath, includeChatHistory)
        .onSuccess { path -> signals.exportSuccess.emit(path) }
        .onFailure { error ->
          signals.errorEvents.emit(SettingsError.ExportFailed(error.message ?: "Export failed"))
        }
      signals.isLoading.value = false
    }
  }

  fun import(uri: Uri) {
    scope.launch {
      signals.isLoading.value = true
      runCatching { importService.importBackup(uri) }
        .fold(
          onSuccess = { result ->
            result
              .onSuccess { summary -> signals.importSuccess.emit(summary) }
              .onFailure { error ->
                signals.errorEvents.emit(
                  SettingsError.ImportFailed(error.message ?: "Import failed")
                )
              }
          },
          onFailure = { error ->
            signals.errorEvents.emit(
              SettingsError.UnexpectedError(error.message ?: "Unexpected error")
            )
          },
        )
      signals.isLoading.value = false
    }
  }
}

private class PrivacyPreferenceController(
  private val scope: CoroutineScope,
  private val updatePrivacyPreferencesUseCase: UpdatePrivacyPreferencesUseCase,
  private val errorEvents: MutableSharedFlow<SettingsError>,
) {
  fun setTelemetryOptIn(optIn: Boolean) {
    scope.launch {
      runCatching { updatePrivacyPreferencesUseCase.setTelemetryOptIn(optIn) }
        .onFailure { error ->
          errorEvents.emit(
            SettingsError.PreferenceUpdateFailed(error.message ?: "Failed to update preference")
          )
        }
    }
  }

  fun acknowledgeConsent() {
    scope.launch {
      runCatching { updatePrivacyPreferencesUseCase.acknowledgeConsent(Clock.System.now()) }
        .onFailure { error ->
          errorEvents.emit(
            SettingsError.PreferenceUpdateFailed(error.message ?: "Failed to acknowledge consent")
          )
        }
    }
  }

  fun setRetentionPolicy(policy: RetentionPolicy) {
    scope.launch {
      runCatching { updatePrivacyPreferencesUseCase.setRetentionPolicy(policy) }
        .onFailure { error ->
          errorEvents.emit(
            SettingsError.PreferenceUpdateFailed(error.message ?: "Failed to set retention policy")
          )
        }
    }
  }

  fun dismissExportWarnings() {
    scope.launch {
      runCatching { updatePrivacyPreferencesUseCase.setExportWarningsDismissed(true) }
        .onFailure { error ->
          errorEvents.emit(
            SettingsError.PreferenceUpdateFailed(error.message ?: "Failed to dismiss warnings")
          )
        }
    }
  }
}

private class UiPreferenceController(
  private val scope: CoroutineScope,
  private val uiUxState: MutableStateFlow<SettingsUiUxState>,
  private val settingsOperationsUseCase: SettingsOperationsUseCase,
  private val toggleCompactModeUseCase: ToggleCompactModeUseCase,
  private val updateUiPreferencesUseCase: UpdateUiPreferencesUseCase,
) {
  private var previousUiUxState: SettingsUiUxState? = null

  val hasPendingUndo: Boolean
    get() = previousUiUxState != null

  fun setThemePreference(themePreference: ThemePreference) {
    previousUiUxState = uiUxState.value
    uiUxState.update {
      it.copy(
        themePreference = themePreference,
        undoAvailable = true,
        statusMessage = "Theme updated",
      )
    }
    scope.launch { settingsOperationsUseCase.updateTheme(themePreference) }
  }

  fun applyDensityPreference(compactModeEnabled: Boolean) = setCompactMode(compactModeEnabled)

  fun setCompactMode(enabled: Boolean) {
    previousUiUxState = uiUxState.value
    uiUxState.update {
      it.copy(
        compactModeEnabled = enabled,
        undoAvailable = true,
        statusMessage = if (enabled) "Compact mode enabled" else "Compact mode disabled",
      )
    }
    scope.launch { toggleCompactModeUseCase.toggle(enabled) }
  }

  fun setHighContrastEnabled(enabled: Boolean) {
    uiUxState.update {
      it.copy(
        highContrastEnabled = enabled,
        undoAvailable = true,
        statusMessage = if (enabled) "High contrast enabled" else "High contrast disabled",
      )
    }
    scope.launch { updateUiPreferencesUseCase.setHighContrastEnabled(enabled) }
  }

  fun undo() {
    val previous = previousUiUxState ?: return
    uiUxState.value = previous.copy(undoAvailable = false, statusMessage = "Preferences restored")
    previousUiUxState = null
    scope.launch {
      settingsOperationsUseCase.updateTheme(previous.themePreference)
      toggleCompactModeUseCase.toggle(previous.compactModeEnabled)
    }
  }

  fun onMigrationSuccess() {
    uiUxState.update { it.copy(showMigrationSuccessNotification = true) }
  }

  fun dismissMigrationSuccessNotification() {
    uiUxState.update { it.copy(showMigrationSuccessNotification = false) }
  }

  fun clearStatusMessage() {
    uiUxState.update { it.copy(statusMessage = null) }
  }
}

private class HuggingFaceAuthController(
  private val scope: CoroutineScope,
  private val coordinator: HuggingFaceAuthCoordinator,
  private val oauthConfig: HuggingFaceOAuthConfig,
  private val uiUxState: MutableStateFlow<SettingsUiUxState>,
  private val errorEvents: MutableSharedFlow<SettingsError>,
) {
  fun saveApiKey(apiKey: String) {
    scope.launch {
      coordinator
        .savePersonalAccessToken(apiKey)
        .onSuccess { authState ->
          when {
            authState.isAuthenticated ->
              uiUxState.update {
                it.copy(statusMessage = "Hugging Face connected", undoAvailable = false)
              }
            authState.lastError != null ->
              errorEvents.emit(SettingsError.HuggingFaceAuthFailed(authState.lastError))
          }
        }
        .onFailure { throwable ->
          errorEvents.emit(
            SettingsError.HuggingFaceAuthFailed(
              throwable.message ?: "Failed to save Hugging Face API key"
            )
          )
        }
    }
  }

  fun refreshAccount() {
    scope.launch { coordinator.refreshAccount() }
  }

  fun startOAuthLogin() {
    scope.launch {
      val clientId = oauthConfig.clientId.trim()
      val scopeParam = oauthConfig.scope.ifBlank { DEFAULT_OAUTH_SCOPE }

      if (clientId.isBlank()) {
        errorEvents.emit(
          SettingsError.HuggingFaceAuthFailed("Hugging Face OAuth client ID is not configured")
        )
        return@launch
      }

      coordinator.beginDeviceAuthorization(clientId = clientId, scope = scopeParam).onFailure {
        throwable ->
        errorEvents.emit(
          SettingsError.HuggingFaceAuthFailed(
            throwable.message ?: "Unable to start Hugging Face sign-in"
          )
        )
      }
    }
  }

  fun cancelOAuth() {
    scope.launch { coordinator.cancelDeviceAuthorization() }
  }

  fun disconnect() {
    scope.launch {
      coordinator.clearCredentials()
      uiUxState.update {
        it.copy(statusMessage = "Hugging Face disconnected", undoAvailable = false)
      }
    }
  }

  private companion object {
    private const val DEFAULT_OAUTH_SCOPE = "all offline_access"
  }
}
