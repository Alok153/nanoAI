package com.vjaykrsna.nanoai.feature.settings.presentation

import android.net.Uri
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.common.error.toErrorEnvelope
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

internal data class SettingsUiPreferenceDependencies(
  val settingsOperationsUseCase: SettingsOperationsUseCase,
  val toggleCompactModeUseCase: ToggleCompactModeUseCase,
  val updateUiPreferencesUseCase: UpdateUiPreferencesUseCase,
  val emitError: suspend (NanoAIErrorEnvelope) -> Unit,
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
  private val emitError: suspend (NanoAIErrorEnvelope) -> Unit,
) {
  fun add(config: APIProviderConfig, credential: ProviderCredentialMutation) {
    execute(
      operation = { apiProviderConfigUseCase.addProvider(config, credential) },
      fallbackMessage = PROVIDER_ADD_FAILURE,
    )
  }

  fun update(config: APIProviderConfig, credential: ProviderCredentialMutation) {
    execute(
      operation = { apiProviderConfigUseCase.updateProvider(config, credential) },
      fallbackMessage = PROVIDER_UPDATE_FAILURE,
    )
  }

  fun delete(providerId: String) {
    execute(
      operation = { apiProviderConfigUseCase.deleteProvider(providerId) },
      fallbackMessage = PROVIDER_DELETE_FAILURE,
    )
  }

  private fun execute(operation: suspend () -> NanoAIResult<Unit>, fallbackMessage: String) {
    scope.launch {
      setLoading(true)
      try {
        when (val result = operation()) {
          is NanoAIResult.Success -> Unit
          else -> emitError(result.toErrorEnvelope(fallbackMessage))
        }
      } catch (throwable: Throwable) {
        emitError(throwable.toErrorEnvelope(fallbackMessage))
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
  private val emitError: suspend (NanoAIErrorEnvelope) -> Unit,
) {
  fun exportBackup(destinationPath: String, includeChatHistory: Boolean) {
    scope.launch {
      setLoading(true)
      try {
        when (
          val result =
            modelDownloadsAndExportUseCase.exportBackup(destinationPath, includeChatHistory)
        ) {
          is NanoAIResult.Success -> emitEvent(SettingsUiEvent.ExportCompleted(result.value))
          else -> emitError(result.toErrorEnvelope(EXPORT_FAILURE_MESSAGE))
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
        when (val result = importService.importBackup(uri)) {
          is NanoAIResult.Success -> emitEvent(SettingsUiEvent.ImportCompleted(result.value))
          else -> emitError(result.toErrorEnvelope(IMPORT_FAILURE_MESSAGE))
        }
      } catch (error: Throwable) {
        emitError(error.toErrorEnvelope(IMPORT_FAILURE_MESSAGE))
      } finally {
        setLoading(false)
      }
    }
  }
}

internal class SettingsPrivacyActions(
  private val scope: CoroutineScope,
  private val updatePrivacyPreferencesUseCase: UpdatePrivacyPreferencesUseCase,
  private val emitError: suspend (NanoAIErrorEnvelope) -> Unit,
  private val clock: Clock,
) {
  fun setTelemetryOptIn(optIn: Boolean) {
    scope.launch {
      runCatching { updatePrivacyPreferencesUseCase.setTelemetryOptIn(optIn) }
        .onFailure { error -> emitError(error.toErrorEnvelope(PRIVACY_UPDATE_FAILURE)) }
    }
  }

  fun acknowledgeConsent() {
    scope.launch {
      runCatching { updatePrivacyPreferencesUseCase.acknowledgeConsent(clock.now()) }
        .onFailure { error -> emitError(error.toErrorEnvelope(CONSENT_ACK_FAILURE)) }
    }
  }

  fun setRetentionPolicy(policy: RetentionPolicy) {
    scope.launch {
      runCatching { updatePrivacyPreferencesUseCase.setRetentionPolicy(policy) }
        .onFailure { error -> emitError(error.toErrorEnvelope(RETENTION_POLICY_FAILURE)) }
    }
  }

  fun dismissExportWarnings() {
    scope.launch {
      runCatching { updatePrivacyPreferencesUseCase.setExportWarningsDismissed(true) }
        .onFailure { error -> emitError(error.toErrorEnvelope(EXPORT_WARNING_DISMISS_FAILURE)) }
    }
  }
}

internal class SettingsUiPreferenceActions(
  private val scope: CoroutineScope,
  private val state: StateFlow<SettingsUiState>,
  private val updateState: (SettingsUiState.() -> SettingsUiState) -> Unit,
  dependencies: SettingsUiPreferenceDependencies,
) {
  private val settingsOperationsUseCase = dependencies.settingsOperationsUseCase
  private val toggleCompactModeUseCase = dependencies.toggleCompactModeUseCase
  private val updateUiPreferencesUseCase = dependencies.updateUiPreferencesUseCase
  private val emitError: suspend (NanoAIErrorEnvelope) -> Unit = dependencies.emitError
  private var previousSnapshot: UiSnapshot? = null

  fun setThemePreference(themePreference: ThemePreference) {
    if (state.value.themePreference == themePreference) return
    captureSnapshot()
    updateState {
      copy(themePreference = themePreference, undoAvailable = true, statusMessage = "Theme updated")
    }
    launchPreferenceUpdate(THEME_UPDATE_FAILURE) {
      settingsOperationsUseCase.updateTheme(themePreference)
    }
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
    launchPreferenceUpdate(COMPACT_MODE_UPDATE_FAILURE) {
      toggleCompactModeUseCase.setCompactMode(enabled)
    }
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
    launchPreferenceUpdate(HIGH_CONTRAST_UPDATE_FAILURE) {
      updateUiPreferencesUseCase.setHighContrastEnabled(enabled)
    }
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
      executePreferenceUpdate(THEME_UPDATE_FAILURE) {
        settingsOperationsUseCase.updateTheme(snapshot.themePreference)
      }
      executePreferenceUpdate(COMPACT_MODE_UPDATE_FAILURE) {
        toggleCompactModeUseCase.setCompactMode(snapshot.compactModeEnabled)
      }
      executePreferenceUpdate(HIGH_CONTRAST_UPDATE_FAILURE) {
        updateUiPreferencesUseCase.setHighContrastEnabled(snapshot.highContrastEnabled)
      }
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

  private fun launchPreferenceUpdate(
    fallbackMessage: String,
    operation: suspend () -> NanoAIResult<Unit>,
  ) {
    scope.launch { executePreferenceUpdate(fallbackMessage, operation) }
  }

  private suspend fun executePreferenceUpdate(
    fallbackMessage: String,
    operation: suspend () -> NanoAIResult<Unit>,
  ) {
    runCatching { operation() }
      .onSuccess { result ->
        if (result !is NanoAIResult.Success) {
          emitError(result.toErrorEnvelope(fallbackMessage))
        }
      }
      .onFailure { error -> emitError(error.toErrorEnvelope(fallbackMessage)) }
  }
}

internal class SettingsHuggingFaceActions(
  private val scope: CoroutineScope,
  private val huggingFaceAuthCoordinator: HuggingFaceAuthCoordinator,
  private val huggingFaceOAuthConfig: HuggingFaceOAuthConfig,
  private val updateState: (SettingsUiState.() -> SettingsUiState) -> Unit,
  private val emitError: suspend (NanoAIErrorEnvelope) -> Unit,
) {
  fun saveApiKey(apiKey: String) {
    scope.launch {
      when (val result = huggingFaceAuthCoordinator.savePersonalAccessToken(apiKey)) {
        is NanoAIResult.Success -> {
          val authState = result.value
          when {
            authState.isAuthenticated ->
              updateState { copy(statusMessage = "Hugging Face connected", undoAvailable = false) }
            !authState.lastError.isNullOrBlank() ->
              emitError(NanoAIErrorEnvelope(authState.lastError!!))
          }
        }
        else -> emitError(result.toErrorEnvelope(HUGGING_FACE_API_KEY_FAILURE))
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
        emitError(NanoAIErrorEnvelope(HUGGING_FACE_MISSING_CLIENT_ID))
        return@launch
      }

      when (
        val result =
          huggingFaceAuthCoordinator.beginDeviceAuthorization(
            clientId = clientId,
            scope = scopeParam,
          )
      ) {
        is NanoAIResult.Success -> Unit
        else -> emitError(result.toErrorEnvelope(HUGGING_FACE_SIGN_IN_FAILURE))
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

private const val PROVIDER_ADD_FAILURE = "Failed to add provider"
private const val PROVIDER_UPDATE_FAILURE = "Failed to update provider"
private const val PROVIDER_DELETE_FAILURE = "Failed to delete provider"
private const val EXPORT_FAILURE_MESSAGE = "Failed to export backup"
private const val IMPORT_FAILURE_MESSAGE = "Failed to import backup"
private const val PRIVACY_UPDATE_FAILURE = "Failed to update preference"
private const val CONSENT_ACK_FAILURE = "Failed to acknowledge consent"
private const val RETENTION_POLICY_FAILURE = "Failed to set retention policy"
private const val EXPORT_WARNING_DISMISS_FAILURE = "Failed to dismiss export warnings"
private const val THEME_UPDATE_FAILURE = "Failed to update theme preference"
private const val COMPACT_MODE_UPDATE_FAILURE = "Failed to update compact mode preference"
private const val HIGH_CONTRAST_UPDATE_FAILURE = "Failed to update high contrast preference"
private const val HUGGING_FACE_API_KEY_FAILURE = "Failed to save Hugging Face API key"
private const val HUGGING_FACE_MISSING_CLIENT_ID = "Hugging Face OAuth client ID is not configured"
private const val HUGGING_FACE_SIGN_IN_FAILURE = "Unable to start Hugging Face sign-in"
