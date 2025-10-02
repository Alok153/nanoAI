package com.vjaykrsna.nanoai.feature.settings.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreference
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreferenceStore
import com.vjaykrsna.nanoai.core.data.preferences.RetentionPolicy
import com.vjaykrsna.nanoai.core.data.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.feature.library.domain.ModelDownloadsAndExportUseCase
import com.vjaykrsna.nanoai.feature.settings.domain.ImportService
import com.vjaykrsna.nanoai.feature.settings.domain.ImportSummary
import com.vjaykrsna.nanoai.feature.uiux.domain.ObserveUserProfileUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.ToggleCompactModeUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.UpdateThemePreferenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val apiProviderConfigRepository: ApiProviderConfigRepository,
        private val modelDownloadsAndExportUseCase: ModelDownloadsAndExportUseCase,
        private val privacyPreferenceStore: PrivacyPreferenceStore,
        private val importService: ImportService,
        private val observeUserProfileUseCase: ObserveUserProfileUseCase,
        private val updateThemePreferenceUseCase: UpdateThemePreferenceUseCase,
        private val toggleCompactModeUseCase: ToggleCompactModeUseCase,
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
        private var previousUiUxState: SettingsUiUxState? = null

        val apiProviders: StateFlow<List<APIProviderConfig>> =
            apiProviderConfigRepository
                .observeAllProviders()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val privacyPreferences: StateFlow<PrivacyPreference> =
            privacyPreferenceStore.privacyPreference
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    PrivacyPreference(
                        exportWarningsDismissed = false,
                        telemetryOptIn = false,
                        consentAcknowledgedAt = null,
                        disclaimerShownCount = 0,
                        retentionPolicy = RetentionPolicy.INDEFINITE,
                    ),
                )

        init {
            observeUserProfileUseCase.flow
                .onEach { result ->
                    val profile = result.userProfile
                    _uiUxState.update { current ->
                        current.copy(
                            themePreference = profile?.themePreference ?: ThemePreference.SYSTEM,
                            compactModeEnabled = profile?.compactMode ?: false,
                            undoAvailable = previousUiUxState != null,
                        )
                    }
                }.launchIn(viewModelScope)
        }

        fun addApiProvider(config: APIProviderConfig) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    apiProviderConfigRepository.addProvider(config)
                } catch (e: Exception) {
                    _errorEvents.emit(SettingsError.ProviderAddFailed(e.message ?: "Failed to add provider"))
                } finally {
                    _isLoading.value = false
                }
            }
        }

        fun updateApiProvider(config: APIProviderConfig) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    apiProviderConfigRepository.updateProvider(config)
                } catch (e: Exception) {
                    _errorEvents.emit(SettingsError.ProviderUpdateFailed(e.message ?: "Failed to update provider"))
                } finally {
                    _isLoading.value = false
                }
            }
        }

        fun deleteApiProvider(providerId: String) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    apiProviderConfigRepository.deleteProvider(providerId)
                } catch (e: Exception) {
                    _errorEvents.emit(SettingsError.ProviderDeleteFailed(e.message ?: "Failed to delete provider"))
                } finally {
                    _isLoading.value = false
                }
            }
        }

        fun exportBackup(
            destinationPath: String,
            includeChatHistory: Boolean = false,
        ) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    modelDownloadsAndExportUseCase
                        .exportBackup(destinationPath, includeChatHistory)
                        .onSuccess { path ->
                            _exportSuccess.emit(path)
                        }.onFailure { error ->
                            _errorEvents.emit(SettingsError.ExportFailed(error.message ?: "Export failed"))
                        }
                } catch (e: Exception) {
                    _errorEvents.emit(SettingsError.UnexpectedError(e.message ?: "Unexpected error"))
                } finally {
                    _isLoading.value = false
                }
            }
        }

        fun importBackup(uri: Uri) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    val result = importService.importBackup(uri)
                    result
                        .onSuccess { summary ->
                            _importSuccess.emit(summary)
                        }.onFailure { error ->
                            _errorEvents.emit(
                                SettingsError.ImportFailed(error.message ?: "Import failed"),
                            )
                        }
                } catch (e: Exception) {
                    _errorEvents.emit(SettingsError.UnexpectedError(e.message ?: "Unexpected error"))
                } finally {
                    _isLoading.value = false
                }
            }
        }

        fun setTelemetryOptIn(optIn: Boolean) {
            viewModelScope.launch {
                try {
                    privacyPreferenceStore.setTelemetryOptIn(optIn)
                } catch (e: Exception) {
                    _errorEvents.emit(SettingsError.PreferenceUpdateFailed(e.message ?: "Failed to update preference"))
                }
            }
        }

        fun acknowledgeConsent() {
            viewModelScope.launch {
                try {
                    privacyPreferenceStore.acknowledgeConsent(Clock.System.now())
                } catch (e: Exception) {
                    _errorEvents.emit(SettingsError.PreferenceUpdateFailed(e.message ?: "Failed to acknowledge consent"))
                }
            }
        }

        fun setRetentionPolicy(policy: RetentionPolicy) {
            viewModelScope.launch {
                try {
                    privacyPreferenceStore.setRetentionPolicy(policy)
                } catch (e: Exception) {
                    _errorEvents.emit(SettingsError.PreferenceUpdateFailed(e.message ?: "Failed to set retention policy"))
                }
            }
        }

        fun dismissExportWarnings() {
            viewModelScope.launch {
                try {
                    privacyPreferenceStore.setExportWarningsDismissed(true)
                } catch (e: Exception) {
                    _errorEvents.emit(SettingsError.PreferenceUpdateFailed(e.message ?: "Failed to dismiss warnings"))
                }
            }
        }

        fun setThemePreference(themePreference: ThemePreference) {
            previousUiUxState = _uiUxState.value
            _uiUxState.update {
                it.copy(
                    themePreference = themePreference,
                    undoAvailable = true,
                    statusMessage = "Theme updated",
                )
            }
            viewModelScope.launch {
                updateThemePreferenceUseCase.updateTheme(themePreference)
            }
        }

        fun setCompactMode(enabled: Boolean) {
            previousUiUxState = _uiUxState.value
            _uiUxState.update {
                it.copy(
                    compactModeEnabled = enabled,
                    undoAvailable = true,
                    statusMessage = if (enabled) "Compact mode enabled" else "Compact mode disabled",
                )
            }
            viewModelScope.launch {
                toggleCompactModeUseCase.toggle(enabled)
            }
        }

        fun undoUiPreferenceChange() {
            val previous = previousUiUxState ?: return
            _uiUxState.value = previous.copy(undoAvailable = false, statusMessage = "Preferences restored")
            previousUiUxState = null
            viewModelScope.launch {
                updateThemePreferenceUseCase.updateTheme(previous.themePreference)
                toggleCompactModeUseCase.toggle(previous.compactModeEnabled)
            }
        }
    }

sealed class SettingsError {
    data class ProviderAddFailed(
        val message: String,
    ) : SettingsError()

    data class ProviderUpdateFailed(
        val message: String,
    ) : SettingsError()

    data class ProviderDeleteFailed(
        val message: String,
    ) : SettingsError()

    data class ExportFailed(
        val message: String,
    ) : SettingsError()

    data class ImportFailed(
        val message: String,
    ) : SettingsError()

    data class PreferenceUpdateFailed(
        val message: String,
    ) : SettingsError()

    data class UnexpectedError(
        val message: String,
    ) : SettingsError()
}

data class SettingsUiUxState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val compactModeEnabled: Boolean = false,
    val undoAvailable: Boolean = false,
    val statusMessage: String? = null,
)
