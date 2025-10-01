package com.vjaykrsna.nanoai.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreference
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreferenceStore
import com.vjaykrsna.nanoai.core.data.preferences.RetentionPolicy
import com.vjaykrsna.nanoai.core.data.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.feature.library.domain.ModelDownloadsAndExportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiProviderConfigRepository: ApiProviderConfigRepository,
    private val modelDownloadsAndExportUseCase: ModelDownloadsAndExportUseCase,
    private val privacyPreferenceStore: PrivacyPreferenceStore
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorEvents = MutableSharedFlow<SettingsError>()
    val errorEvents = _errorEvents.asSharedFlow()

    private val _exportSuccess = MutableSharedFlow<String>()
    val exportSuccess = _exportSuccess.asSharedFlow()

    val apiProviders: StateFlow<List<APIProviderConfig>> = apiProviderConfigRepository.observeAllProviders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val privacyPreferences: StateFlow<PrivacyPreference> = privacyPreferenceStore.privacyPreference
        .stateIn(
            viewModelScope, 
            SharingStarted.WhileSubscribed(5000),
            PrivacyPreference(
                exportWarningsDismissed = false,
                telemetryOptIn = false,
                consentAcknowledgedAt = null,
                retentionPolicy = RetentionPolicy.INDEFINITE
            )
        )

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

    fun exportBackup(destinationPath: String, includeChatHistory: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                modelDownloadsAndExportUseCase.exportBackup(destinationPath, includeChatHistory)
                    .onSuccess { path ->
                        _exportSuccess.emit(path)
                    }
                    .onFailure { error ->
                        _errorEvents.emit(SettingsError.ExportFailed(error.message ?: "Export failed"))
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
}

sealed class SettingsError {
    data class ProviderAddFailed(val message: String) : SettingsError()
    data class ProviderUpdateFailed(val message: String) : SettingsError()
    data class ProviderDeleteFailed(val message: String) : SettingsError()
    data class ExportFailed(val message: String) : SettingsError()
    data class PreferenceUpdateFailed(val message: String) : SettingsError()
    data class UnexpectedError(val message: String) : SettingsError()
}
