package com.vjaykrsna.nanoai.feature.settings.presentation

import android.net.Uri
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.onFailure
import com.vjaykrsna.nanoai.core.common.onSuccess
import com.vjaykrsna.nanoai.core.domain.library.ModelDownloadsAndExportUseCase
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.ProviderCredentialMutation
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.settings.ApiProviderConfigUseCase
import com.vjaykrsna.nanoai.core.domain.settings.ImportService
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceAuthCoordinator
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
import com.vjaykrsna.nanoai.feature.settings.presentation.model.SettingsUiEvent
import com.vjaykrsna.nanoai.feature.settings.presentation.state.SettingsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

internal data class SettingsObserverDependencies(
  val apiProviderConfigUseCase: ApiProviderConfigUseCase,
  val observePrivacyPreferencesUseCase: ObservePrivacyPreferencesUseCase,
  val observeUiPreferencesUseCase: ObserveUiPreferencesUseCase,
  val observeUserProfileUseCase: ObserveUserProfileUseCase,
  val huggingFaceAuthCoordinator: HuggingFaceAuthCoordinator,
)

internal class SettingsStateObservers(
  private val scope: CoroutineScope,
  private val dependencies: SettingsObserverDependencies,
  private val updateState: (SettingsUiState.() -> SettingsUiState) -> Unit,
) {
  fun start() {
    observeApiProviders()
    observePrivacyPreferences()
    observeUserProfile()
    observeUiPreferences()
    observeHuggingFaceState()
  }

  private fun observeApiProviders() {
    scope.launch {
      dependencies.apiProviderConfigUseCase.observeAllProviders().collect { providers ->
        updateState { copy(apiProviders = providers) }
      }
    }
  }

  private fun observePrivacyPreferences() {
    scope.launch {
      dependencies.observePrivacyPreferencesUseCase().collect { preference ->
        updateState { copy(privacyPreference = preference) }
      }
    }
  }

  private fun observeUserProfile() {
    scope.launch {
      dependencies.observeUserProfileUseCase.flow.collect { result ->
        val profile = result.userProfile
        updateState {
          copy(
            themePreference = profile?.themePreference ?: ThemePreference.SYSTEM,
            compactModeEnabled = profile?.compactMode ?: false,
          )
        }
      }
    }
  }

  private fun observeUiPreferences() {
    scope.launch {
      dependencies.observeUiPreferencesUseCase().collect { prefs ->
        updateState { copy(highContrastEnabled = prefs.highContrastEnabled) }
      }
    }
  }

  private fun observeHuggingFaceState() {
    scope.launch {
      dependencies.huggingFaceAuthCoordinator.state.collect { authState ->
        updateState { copy(huggingFaceAuthState = authState) }
      }
    }
    scope.launch {
      dependencies.huggingFaceAuthCoordinator.deviceAuthState.collect { deviceState ->
        updateState { copy(huggingFaceDeviceAuthState = deviceState) }
      }
    }
    scope.launch {
      dependencies.huggingFaceAuthCoordinator.deviceAuthState
        .map { deviceState -> deviceState.deriveStatusMessage() }
        .distinctUntilChanged()
        .collect { message ->
          if (!message.isNullOrBlank()) {
            updateState { copy(statusMessage = message) }
          }
        }
    }
  }
}

private fun HuggingFaceDeviceAuthState?.deriveStatusMessage(): String? {
  if (this == null) return null
  val announcement = lastErrorAnnouncement
  val error = lastError

  return when {
    !announcement.isNullOrBlank() -> announcement
    !error.isNullOrBlank() -> error
    else -> null
  }
}

internal class SettingsApiProviderActions(
  private val scope: CoroutineScope,
  private val apiProviderConfigUseCase: ApiProviderConfigUseCase,
  private val setLoading: (Boolean) -> Unit,
  private val emitError: suspend (SettingsError) -> Unit,
) {
  fun add(config: APIProviderConfig, credential: ProviderCredentialMutation) {
    execute(
      operation = { apiProviderConfigUseCase.addProvider(config, credential) },
      error = { message -> SettingsError.ProviderAddFailed(message) },
      defaultMessage = "Failed to add provider",
    )
  }

  fun update(config: APIProviderConfig, credential: ProviderCredentialMutation) {
    execute(
      operation = { apiProviderConfigUseCase.updateProvider(config, credential) },
      error = { message -> SettingsError.ProviderUpdateFailed(message) },
      defaultMessage = "Failed to update provider",
    )
  }

  fun delete(providerId: String) {
    execute(
      operation = { apiProviderConfigUseCase.deleteProvider(providerId) },
      error = { message -> SettingsError.ProviderDeleteFailed(message) },
      defaultMessage = "Failed to delete provider",
    )
  }

  private fun execute(
    operation: suspend () -> NanoAIResult<Unit>,
    error: (String) -> SettingsError,
    defaultMessage: String,
  ) {
    scope.launch {
      setLoading(true)
      try {
        runCatching { operation() }
          .onSuccess { result ->
            result.onFailure { failure -> emitError(error(failure.message ?: defaultMessage)) }
          }
          .onFailure { throwable -> emitError(error(throwable.message ?: defaultMessage)) }
      } finally {
        setLoading(false)
      }
    }
  }
}

internal class SettingsBackupActions(
  private val scope: CoroutineScope,
  private val modelDownloadsAndExportUseCase: ModelDownloadsAndExportUseCase,
  private val importService: ImportService,
  private val setLoading: (Boolean) -> Unit,
  private val emitEvent: suspend (SettingsUiEvent) -> Unit,
  private val emitError: suspend (SettingsError) -> Unit,
) {
  fun exportBackup(destinationPath: String, includeChatHistory: Boolean) {
    scope.launch {
      setLoading(true)
      try {
        modelDownloadsAndExportUseCase
          .exportBackup(destinationPath, includeChatHistory)
          .onSuccess { path -> emitEvent(SettingsUiEvent.ExportCompleted(path)) }
          .onFailure { error ->
            emitError(SettingsError.ExportFailed(error.message ?: "Export failed"))
          }
      } finally {
        setLoading(false)
      }
    }
  }

  fun importBackup(uri: Uri) {
    scope.launch {
      setLoading(true)
      try {
        runCatching { importService.importBackup(uri) }
          .onSuccess { result ->
            result
              .onSuccess { summary -> emitEvent(SettingsUiEvent.ImportCompleted(summary)) }
              .onFailure { error ->
                emitError(SettingsError.ImportFailed(error.message ?: "Import failed"))
              }
          }
          .onFailure { throwable ->
            emitError(SettingsError.UnexpectedError(throwable.message ?: "Unexpected error"))
          }
      } finally {
        setLoading(false)
      }
    }
  }
}

internal class SettingsPrivacyActions(
  private val scope: CoroutineScope,
  private val updatePrivacyPreferencesUseCase: UpdatePrivacyPreferencesUseCase,
  private val emitError: suspend (SettingsError) -> Unit,
  private val clock: Clock,
) {
  fun setTelemetryOptIn(optIn: Boolean) {
    scope.launch {
      runCatching { updatePrivacyPreferencesUseCase.setTelemetryOptIn(optIn) }
        .onFailure { error ->
          emitError(
            SettingsError.PreferenceUpdateFailed(error.message ?: "Failed to update preference")
          )
        }
    }
  }

  fun acknowledgeConsent() {
    scope.launch {
      runCatching { updatePrivacyPreferencesUseCase.acknowledgeConsent(clock.now()) }
        .onFailure { error ->
          emitError(
            SettingsError.PreferenceUpdateFailed(error.message ?: "Failed to acknowledge consent")
          )
        }
    }
  }

  fun setRetentionPolicy(policy: RetentionPolicy) {
    scope.launch {
      runCatching { updatePrivacyPreferencesUseCase.setRetentionPolicy(policy) }
        .onFailure { error ->
          emitError(
            SettingsError.PreferenceUpdateFailed(error.message ?: "Failed to set retention policy")
          )
        }
    }
  }

  fun dismissExportWarnings() {
    scope.launch {
      runCatching { updatePrivacyPreferencesUseCase.setExportWarningsDismissed(true) }
        .onFailure { error ->
          emitError(
            SettingsError.PreferenceUpdateFailed(error.message ?: "Failed to dismiss warnings")
          )
        }
    }
  }
}

internal class SettingsUiPreferenceActions(
  private val scope: CoroutineScope,
  private val state: StateFlow<SettingsUiState>,
  private val updateState: (SettingsUiState.() -> SettingsUiState) -> Unit,
  private val settingsOperationsUseCase: SettingsOperationsUseCase,
  private val toggleCompactModeUseCase: ToggleCompactModeUseCase,
  private val updateUiPreferencesUseCase: UpdateUiPreferencesUseCase,
) {
  private var previousSnapshot: UiSnapshot? = null

  fun setThemePreference(themePreference: ThemePreference) {
    if (state.value.themePreference == themePreference) return
    captureSnapshot()
    updateState {
      copy(themePreference = themePreference, undoAvailable = true, statusMessage = "Theme updated")
    }
    scope.launch { settingsOperationsUseCase.updateTheme(themePreference) }
  }

  fun setCompactMode(enabled: Boolean) {
    if (state.value.compactModeEnabled == enabled) return
    captureSnapshot()
    updateState {
      copy(
        compactModeEnabled = enabled,
        undoAvailable = true,
        statusMessage = if (enabled) "Compact mode enabled" else "Compact mode disabled",
      )
    }
    scope.launch { toggleCompactModeUseCase.toggle(enabled) }
  }

  fun setHighContrastEnabled(enabled: Boolean) {
    if (state.value.highContrastEnabled == enabled) return
    captureSnapshot()
    updateState {
      copy(
        highContrastEnabled = enabled,
        undoAvailable = true,
        statusMessage = if (enabled) "High contrast enabled" else "High contrast disabled",
      )
    }
    scope.launch { updateUiPreferencesUseCase.setHighContrastEnabled(enabled) }
  }

  fun undoUiPreferenceChange() {
    val snapshot = previousSnapshot ?: return
    previousSnapshot = null
    updateState {
      copy(
        themePreference = snapshot.themePreference,
        compactModeEnabled = snapshot.compactModeEnabled,
        highContrastEnabled = snapshot.highContrastEnabled,
        undoAvailable = false,
        statusMessage = "Preferences restored",
      )
    }
    scope.launch {
      settingsOperationsUseCase.updateTheme(snapshot.themePreference)
      toggleCompactModeUseCase.toggle(snapshot.compactModeEnabled)
      updateUiPreferencesUseCase.setHighContrastEnabled(snapshot.highContrastEnabled)
    }
  }

  fun applyDensityPreference(compactModeEnabled: Boolean) {
    setCompactMode(compactModeEnabled)
  }

  private fun captureSnapshot() {
    if (previousSnapshot != null) return
    val current = state.value
    previousSnapshot =
      UiSnapshot(
        themePreference = current.themePreference,
        compactModeEnabled = current.compactModeEnabled,
        highContrastEnabled = current.highContrastEnabled,
      )
  }

  private data class UiSnapshot(
    val themePreference: ThemePreference,
    val compactModeEnabled: Boolean,
    val highContrastEnabled: Boolean,
  )
}

internal class SettingsHuggingFaceActions(
  private val scope: CoroutineScope,
  private val huggingFaceAuthCoordinator: HuggingFaceAuthCoordinator,
  private val huggingFaceOAuthConfig: HuggingFaceOAuthConfig,
  private val updateState: (SettingsUiState.() -> SettingsUiState) -> Unit,
  private val emitError: suspend (SettingsError) -> Unit,
) {
  fun saveApiKey(apiKey: String) {
    scope.launch {
      huggingFaceAuthCoordinator
        .savePersonalAccessToken(apiKey)
        .onSuccess { authState ->
          when {
            authState.isAuthenticated ->
              updateState { copy(statusMessage = "Hugging Face connected", undoAvailable = false) }
            !authState.lastError.isNullOrBlank() ->
              emitError(SettingsError.HuggingFaceAuthFailed(authState.lastError!!))
          }
        }
        .onFailure { throwable ->
          emitError(
            SettingsError.HuggingFaceAuthFailed(
              throwable.message ?: "Failed to save Hugging Face API key"
            )
          )
        }
    }
  }

  fun refreshAccount() {
    scope.launch { huggingFaceAuthCoordinator.refreshAccount() }
  }

  fun startOAuthLogin() {
    scope.launch {
      val clientId = huggingFaceOAuthConfig.clientId.trim()
      val scopeParam = huggingFaceOAuthConfig.scope.ifBlank { DEFAULT_OAUTH_SCOPE }

      if (clientId.isBlank()) {
        emitError(
          SettingsError.HuggingFaceAuthFailed("Hugging Face OAuth client ID is not configured")
        )
        return@launch
      }

      huggingFaceAuthCoordinator
        .beginDeviceAuthorization(clientId = clientId, scope = scopeParam)
        .onFailure { throwable ->
          emitError(
            SettingsError.HuggingFaceAuthFailed(
              throwable.message ?: "Unable to start Hugging Face sign-in"
            )
          )
        }
    }
  }

  fun cancelOAuthLogin() {
    scope.launch { huggingFaceAuthCoordinator.cancelDeviceAuthorization() }
  }

  fun disconnectAccount() {
    scope.launch {
      huggingFaceAuthCoordinator.clearCredentials()
      updateState { copy(statusMessage = "Hugging Face disconnected", undoAvailable = false) }
    }
  }

  private companion object {
    private const val DEFAULT_OAUTH_SCOPE = "all offline_access"
  }
}
