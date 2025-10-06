package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreference
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsUiUxState
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsViewModel

internal data class SettingsDialogState(
  val showAddProviderDialog: Boolean,
  val showExportDialog: Boolean,
  val editingProvider: APIProviderConfig?,
)

internal data class SettingsDialogHandlers(
  val onProviderDismiss: () -> Unit,
  val onProviderSave: (name: String, baseUrl: String, apiKey: String?) -> Unit,
  val onExportDismiss: () -> Unit,
  val onExportConfirm: (dontShowAgain: Boolean) -> Unit,
)

internal data class SettingsScreenCoordinator(
  val contentState: SettingsContentState,
  val snackbarHostState: SnackbarHostState,
  val actions: SettingsScreenActions,
  val dialogState: SettingsDialogState,
  val dialogHandlers: SettingsDialogHandlers,
)

internal data class MutableSettingsDialogState(
  val showAddProviderDialog: MutableState<Boolean>,
  val showExportDialog: MutableState<Boolean>,
  val editingProvider: MutableState<APIProviderConfig?>,
)

@Composable
internal fun rememberMutableSettingsDialogState(): MutableSettingsDialogState {
  val showAddDialogState = remember { mutableStateOf(false) }
  val showExportDialogState = remember { mutableStateOf(false) }
  val editingProviderState = remember { mutableStateOf<APIProviderConfig?>(null) }
  return remember {
    MutableSettingsDialogState(
      showAddProviderDialog = showAddDialogState,
      showExportDialog = showExportDialogState,
      editingProvider = editingProviderState,
    )
  }
}

internal fun MutableSettingsDialogState.snapshot(): SettingsDialogState =
  SettingsDialogState(
    showAddProviderDialog = showAddProviderDialog.value,
    showExportDialog = showExportDialog.value,
    editingProvider = editingProvider.value,
  )

internal fun createDialogHandlers(
  viewModel: SettingsViewModel,
  dialogState: MutableSettingsDialogState,
): SettingsDialogHandlers {
  fun hideProviderDialog() {
    dialogState.showAddProviderDialog.value = false
    dialogState.editingProvider.value = null
  }

  return SettingsDialogHandlers(
    onProviderDismiss = { hideProviderDialog() },
    onProviderSave = { name, baseUrl, apiKey ->
      saveProvider(
        mutation =
          ProviderMutation(
            editingProvider = dialogState.editingProvider.value,
            onAdd = viewModel::addApiProvider,
            onUpdate = viewModel::updateApiProvider,
          ),
        name = name,
        baseUrl = baseUrl,
        apiKey = apiKey,
      )
      hideProviderDialog()
    },
    onExportDismiss = { dialogState.showExportDialog.value = false },
    onExportConfirm = { dontShowAgain ->
      confirmExport(
        dontShowAgain = dontShowAgain,
        dismissWarnings = viewModel::dismissExportWarnings,
        export = { viewModel.exportBackupToDownloads() },
      )
      dialogState.showExportDialog.value = false
    },
  )
}

internal data class SettingsContentState(
  val apiProviders: List<APIProviderConfig>,
  val privacyPreferences: PrivacyPreference,
  val uiUxState: SettingsUiUxState,
)
