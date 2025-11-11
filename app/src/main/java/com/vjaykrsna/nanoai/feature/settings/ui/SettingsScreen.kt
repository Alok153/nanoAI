package com.vjaykrsna.nanoai.feature.settings.ui

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsViewModel

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
  val coordinator = rememberSettingsScreenState(viewModel, onNavigateToCoverageDashboard)

  SettingsScreenContent(
    state = coordinator.uiState,
    snackbarHostState = coordinator.snackbarHostState,
    actions = coordinator.actions,
    modifier = modifier,
  )

  SettingsDialogs(
    showAddProviderDialog = coordinator.dialogState.showAddProviderDialog,
    editingProvider = coordinator.dialogState.editingProvider,
    showExportDialog = coordinator.dialogState.showExportDialog,
    onProviderDismiss = coordinator.dialogHandlers.onProviderDismiss,
    onProviderSave = coordinator.dialogHandlers.onProviderSave,
    onExportDismiss = coordinator.dialogHandlers.onExportDismiss,
    onExportConfirm = coordinator.dialogHandlers.onExportConfirm,
    showHuggingFaceLoginDialog = coordinator.dialogState.showHuggingFaceLoginDialog,
    showHuggingFaceApiKeyDialog = coordinator.dialogState.showHuggingFaceApiKeyDialog,
    huggingFaceDeviceAuthState = coordinator.uiState.huggingFaceDeviceAuthState,
    onHuggingFaceLoginDismiss = coordinator.dialogHandlers.onHuggingFaceLoginDismiss,
    onHuggingFaceLoginConfirm = coordinator.dialogHandlers.onHuggingFaceLoginConfirm,
    onHuggingFaceApiKeyDismiss = coordinator.dialogHandlers.onHuggingFaceApiKeyDismiss,
    onHuggingFaceApiKeySave = coordinator.dialogHandlers.onHuggingFaceApiKeySave,
  )
}

@Composable
internal fun rememberSettingsScreenState(
  viewModel: SettingsViewModel,
  onNavigateToCoverageDashboard: () -> Unit = {},
): SettingsScreenCoordinator {
  val uiState by viewModel.state.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }
  val dialogState = rememberMutableSettingsDialogState()

  val importBackupLauncher = rememberImportBackupLauncher(viewModel::importBackup)

  CollectSettingsEvents(viewModel.events, snackbarHostState)

  val actions =
    createActions(
      viewModel,
      uiState.privacyPreference,
      importBackupLauncher,
      dialogState,
      onNavigateToCoverageDashboard,
    )
  val dialogHandlers = createDialogHandlers(viewModel, dialogState)

  return SettingsScreenCoordinator(
    uiState = uiState,
    snackbarHostState = snackbarHostState,
    actions = actions,
    dialogState = dialogState.snapshot(),
    dialogHandlers = dialogHandlers,
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
