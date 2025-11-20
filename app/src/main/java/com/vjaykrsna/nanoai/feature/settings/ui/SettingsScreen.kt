package com.vjaykrsna.nanoai.feature.settings.ui

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsViewModel
import com.vjaykrsna.nanoai.feature.settings.presentation.model.SettingsUiEvent
import kotlinx.coroutines.flow.collectLatest

/**
 * Settings screen for configuring app preferences and API providers.
 *
 * Features:
 * - Tabbed interface for different settings categories
 * - API provider management
 * - Privacy and security settings
 * - Theme and UI customization
 * - Backup and sync options
 *
 * @param modifier Modifier to apply to the screen
 * @param viewModel SettingsViewModel for managing settings state and operations
 */
@Composable
fun SettingsScreen(
  modifier: Modifier = Modifier,
  viewModel: SettingsViewModel = hiltViewModel(),
  onNavigateToCoverageDashboard: () -> Unit = {},
) {
  val uiState by viewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val dialogState = rememberMutableSettingsDialogState()
  val importBackupLauncher = rememberImportBackupLauncher(viewModel::importBackup)

  val actions =
    remember(viewModel, uiState.privacyPreference, dialogState, importBackupLauncher) {
      createActions(
        viewModel = viewModel,
        privacyPreferences = uiState.privacyPreference,
        importBackupLauncher = importBackupLauncher,
        dialogState = dialogState,
        onNavigateToCoverageDashboard = onNavigateToCoverageDashboard,
      )
    }
  val dialogHandlers =
    remember(viewModel, dialogState) { createDialogHandlers(viewModel, dialogState) }

  LaunchedEffect(viewModel) {
    viewModel.events.collectLatest { event ->
      when (event) {
        is SettingsUiEvent.ErrorRaised -> snackbarHostState.showSnackbar(event.envelope.userMessage)
        is SettingsUiEvent.ExportCompleted ->
          snackbarHostState.showSnackbar(exportSuccessMessage(event.destinationPath))
        is SettingsUiEvent.ImportCompleted ->
          snackbarHostState.showSnackbar(buildImportSuccessMessage(event.summary))
      }
    }
  }

  SettingsScreenContent(
    state = uiState,
    snackbarHostState = snackbarHostState,
    actions = actions,
    modifier = modifier,
  )

  val dialogs = dialogState.snapshot()

  SettingsDialogs(
    showAddProviderDialog = dialogs.showAddProviderDialog,
    editingProvider = dialogs.editingProvider,
    showExportDialog = dialogs.showExportDialog,
    onProviderDismiss = dialogHandlers.onProviderDismiss,
    onProviderSave = dialogHandlers.onProviderSave,
    onExportDismiss = dialogHandlers.onExportDismiss,
    onExportConfirm = dialogHandlers.onExportConfirm,
    showHuggingFaceLoginDialog = dialogs.showHuggingFaceLoginDialog,
    showHuggingFaceApiKeyDialog = dialogs.showHuggingFaceApiKeyDialog,
    huggingFaceDeviceAuthState = uiState.huggingFaceDeviceAuthState,
    onHuggingFaceLoginDismiss = dialogHandlers.onHuggingFaceLoginDismiss,
    onHuggingFaceLoginConfirm = dialogHandlers.onHuggingFaceLoginConfirm,
    onHuggingFaceApiKeyDismiss = dialogHandlers.onHuggingFaceApiKeyDismiss,
    onHuggingFaceApiKeySave = dialogHandlers.onHuggingFaceApiKeySave,
  )
}

@Composable
internal fun rememberImportBackupLauncher(
  onImport: (Uri) -> Unit
): ManagedActivityResultLauncher<Array<String>, Uri?> {
  return rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    uri?.let(onImport)
  }
}
