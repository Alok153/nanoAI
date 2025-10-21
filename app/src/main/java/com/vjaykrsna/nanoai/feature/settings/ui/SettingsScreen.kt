@file:Suppress("LongParameterList")

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
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
  val coordinator = rememberSettingsScreenState(viewModel)

  SettingsScreenContent(
    state = coordinator.contentState,
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
    huggingFaceDeviceAuthState = coordinator.huggingFaceDeviceAuthState,
    onHuggingFaceLoginDismiss = coordinator.dialogHandlers.onHuggingFaceLoginDismiss,
    onHuggingFaceLoginConfirm = coordinator.dialogHandlers.onHuggingFaceLoginConfirm,
    onHuggingFaceApiKeyDismiss = coordinator.dialogHandlers.onHuggingFaceApiKeyDismiss,
    onHuggingFaceApiKeySave = coordinator.dialogHandlers.onHuggingFaceApiKeySave,
  )
}

@Composable
internal fun rememberSettingsScreenState(viewModel: SettingsViewModel): SettingsScreenCoordinator {
  val apiProviders by viewModel.apiProviders.collectAsState()
  val privacyPreferences by viewModel.privacyPreferences.collectAsState()
  val uiUxState by viewModel.uiUxState.collectAsState()
  val huggingFaceAuthState by viewModel.huggingFaceAuthState.collectAsState()
  val huggingFaceDeviceAuthState by viewModel.huggingFaceDeviceAuthState.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }
  val dialogState = rememberMutableSettingsDialogState()

  val importBackupLauncher = rememberImportBackupLauncher(viewModel::importBackup)

  CollectSettingsErrorEvents(viewModel.errorEvents, snackbarHostState)
  CollectExportSuccessEvents(viewModel.exportSuccess, snackbarHostState)
  CollectImportSuccessEvents(viewModel.importSuccess, snackbarHostState)

  val actions = createActions(viewModel, privacyPreferences, importBackupLauncher, dialogState)
  val dialogHandlers = createDialogHandlers(viewModel, dialogState)

  val contentState =
    SettingsContentState(
      apiProviders = apiProviders,
      privacyPreferences = privacyPreferences,
      uiUxState = uiUxState,
      huggingFaceState = huggingFaceAuthState,
      isLoading = isLoading,
    )

  return SettingsScreenCoordinator(
    contentState = contentState,
    snackbarHostState = snackbarHostState,
    actions = actions,
    dialogState = dialogState.snapshot(),
    dialogHandlers = dialogHandlers,
    huggingFaceDeviceAuthState = huggingFaceDeviceAuthState,
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
