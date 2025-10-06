@file:Suppress("LongParameterList")

package com.vjaykrsna.nanoai.feature.settings.ui

import android.net.Uri
import android.os.Environment
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreference
import com.vjaykrsna.nanoai.core.data.preferences.RetentionPolicy
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsUiUxState
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsViewModel
import java.util.UUID

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
  )
}

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
        editingProvider = dialogState.editingProvider.value,
        name = name,
        baseUrl = baseUrl,
        apiKey = apiKey,
        onAdd = viewModel::addApiProvider,
        onUpdate = viewModel::updateApiProvider,
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

internal fun createActions(
  viewModel: SettingsViewModel,
  privacyPreferences: PrivacyPreference,
  importBackupLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
  dialogState: MutableSettingsDialogState,
): SettingsScreenActions {
  return createSettingsActions(
    viewModel = viewModel,
    privacyPreferences = privacyPreferences,
    importBackupLauncher = importBackupLauncher,
    onAddProviderClick = { dialogState.showAddProviderDialog.value = true },
    onEditProvider = { provider -> dialogState.editingProvider.value = provider },
    onDeleteProvider = { provider -> viewModel.deleteApiProvider(provider.providerId) },
    onShowExportDialog = { dialogState.showExportDialog.value = true },
  )
}

@Composable
internal fun rememberSettingsScreenState(viewModel: SettingsViewModel): SettingsScreenCoordinator {
  val apiProviders by viewModel.apiProviders.collectAsState()
  val privacyPreferences by viewModel.privacyPreferences.collectAsState()
  val uiUxState by viewModel.uiUxState.collectAsState()

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
    )

  return SettingsScreenCoordinator(
    contentState = contentState,
    snackbarHostState = snackbarHostState,
    actions = actions,
    dialogState = dialogState.snapshot(),
    dialogHandlers = dialogHandlers,
  )
}

@Composable
internal fun rememberImportBackupLauncher(
  onImport: (Uri) -> Unit,
): ManagedActivityResultLauncher<Array<String>, Uri?> {
  return rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    uri?.let(onImport)
  }
}

internal fun SettingsViewModel.exportBackupToDownloads(includeChatHistory: Boolean = true) {
  val downloadsPath =
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath +
      "/nanoai-backup-${System.currentTimeMillis()}.json"
  exportBackup(downloadsPath, includeChatHistory)
}

internal fun handleExportRequest(
  warningsDismissed: Boolean,
  onExport: () -> Unit,
  onShowDialog: () -> Unit,
) {
  if (warningsDismissed) {
    onExport()
  } else {
    onShowDialog()
  }
}

internal fun confirmExport(
  dontShowAgain: Boolean,
  dismissWarnings: () -> Unit,
  export: () -> Unit,
) {
  if (dontShowAgain) {
    dismissWarnings()
  }
  export()
}

internal fun saveProvider(
  editingProvider: APIProviderConfig?,
  name: String,
  baseUrl: String,
  apiKey: String?,
  onAdd: (APIProviderConfig) -> Unit,
  onUpdate: (APIProviderConfig) -> Unit,
) {
  editingProvider?.let { provider ->
    onUpdate(
      provider.copy(
        providerName = name,
        baseUrl = baseUrl,
        apiKey = apiKey ?: "",
      ),
    )
  }
    ?: onAdd(
      APIProviderConfig(
        providerId = UUID.randomUUID().toString(),
        providerName = name,
        baseUrl = baseUrl,
        apiKey = apiKey ?: "",
        apiType = APIType.OPENAI_COMPATIBLE,
        isEnabled = true,
      ),
    )
}

internal fun createSettingsActions(
  viewModel: SettingsViewModel,
  privacyPreferences: PrivacyPreference,
  importBackupLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
  onAddProviderClick: () -> Unit,
  onEditProvider: (APIProviderConfig) -> Unit,
  onDeleteProvider: (APIProviderConfig) -> Unit,
  onShowExportDialog: () -> Unit,
): SettingsScreenActions {
  return SettingsScreenActions(
    onAddProviderClick = onAddProviderClick,
    onProviderEdit = onEditProvider,
    onProviderDelete = onDeleteProvider,
    onImportBackupClick = {
      importBackupLauncher.launch(
        arrayOf("application/json", "application/zip", "application/octet-stream"),
      )
    },
    onExportBackupClick = {
      handleExportRequest(
        warningsDismissed = privacyPreferences.exportWarningsDismissed,
        onExport = { viewModel.exportBackupToDownloads() },
        onShowDialog = onShowExportDialog,
      )
    },
    onTelemetryToggle = viewModel::setTelemetryOptIn,
    onRetentionPolicyChange = viewModel::setRetentionPolicy,
    onDismissMigrationSuccess = viewModel::dismissMigrationSuccessNotification,
  )
}

internal data class SettingsContentState(
  val apiProviders: List<APIProviderConfig>,
  val privacyPreferences: PrivacyPreference,
  val uiUxState: SettingsUiUxState,
)

internal data class SettingsScreenActions(
  val onAddProviderClick: () -> Unit,
  val onProviderEdit: (APIProviderConfig) -> Unit,
  val onProviderDelete: (APIProviderConfig) -> Unit,
  val onImportBackupClick: () -> Unit,
  val onExportBackupClick: () -> Unit,
  val onTelemetryToggle: (Boolean) -> Unit,
  val onRetentionPolicyChange: (RetentionPolicy) -> Unit,
  val onDismissMigrationSuccess: () -> Unit,
)
