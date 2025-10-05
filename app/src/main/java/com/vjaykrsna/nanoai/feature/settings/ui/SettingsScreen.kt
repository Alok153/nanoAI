@file:Suppress("TooManyFunctions", "LongParameterList") // Composable UI with many helpers

package com.vjaykrsna.nanoai.feature.settings.ui

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreference
import com.vjaykrsna.nanoai.core.data.preferences.RetentionPolicy
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.feature.settings.domain.ImportSummary
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsError
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsUiUxState
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsViewModel
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
  val apiProviders by viewModel.apiProviders.collectAsState()
  val privacyPreferences by viewModel.privacyPreferences.collectAsState()
  val uiUxState by viewModel.uiUxState.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }
  var showAddProviderDialog by remember { mutableStateOf(false) }
  var showExportDialog by remember { mutableStateOf(false) }
  var editingProvider by remember { mutableStateOf<APIProviderConfig?>(null) }

  val importBackupLauncher = rememberImportBackupLauncher(viewModel::importBackup)

  CollectSettingsErrorEvents(viewModel.errorEvents, snackbarHostState)
  CollectExportSuccessEvents(viewModel.exportSuccess, snackbarHostState)
  CollectImportSuccessEvents(viewModel.importSuccess, snackbarHostState)

  fun hideProviderDialog() {
    showAddProviderDialog = false
    editingProvider = null
  }

  val actions =
    createSettingsActions(
      viewModel = viewModel,
      privacyPreferences = privacyPreferences,
      importBackupLauncher = importBackupLauncher,
      onAddProviderClick = { showAddProviderDialog = true },
      onEditProvider = { editingProvider = it },
      onDeleteProvider = { provider -> viewModel.deleteApiProvider(provider.providerId) },
      onShowExportDialog = { showExportDialog = true },
    )

  SettingsScreenContent(
    state =
      SettingsContentState(
        apiProviders = apiProviders,
        privacyPreferences = privacyPreferences,
        uiUxState = uiUxState,
      ),
    snackbarHostState = snackbarHostState,
    actions = actions,
    modifier = modifier,
  )

  SettingsDialogs(
    showAddProviderDialog = showAddProviderDialog,
    editingProvider = editingProvider,
    showExportDialog = showExportDialog,
    onProviderDismiss = { hideProviderDialog() },
    onProviderSave = { name, baseUrl, apiKey ->
      saveProvider(
        editingProvider = editingProvider,
        name = name,
        baseUrl = baseUrl,
        apiKey = apiKey,
        onAdd = viewModel::addApiProvider,
        onUpdate = viewModel::updateApiProvider,
      )
      hideProviderDialog()
    },
    onExportDismiss = { showExportDialog = false },
    onExportConfirm = { dontShowAgain ->
      confirmExport(
        dontShowAgain = dontShowAgain,
        dismissWarnings = viewModel::dismissExportWarnings,
        export = { viewModel.exportBackupToDownloads() },
      )
      showExportDialog = false
    },
  )
}

@Composable
private fun CollectSettingsErrorEvents(
  errorEvents: Flow<SettingsError>,
  snackbarHostState: SnackbarHostState,
) {
  LaunchedEffect(Unit) {
    errorEvents.collectLatest { error -> snackbarHostState.showSnackbar(error.toUserMessage()) }
  }
}

@Composable
private fun CollectExportSuccessEvents(
  exportSuccess: Flow<String>,
  snackbarHostState: SnackbarHostState,
) {
  LaunchedEffect(Unit) {
    exportSuccess.collectLatest { path ->
      snackbarHostState.showSnackbar(exportSuccessMessage(path))
    }
  }
}

@Composable
private fun CollectImportSuccessEvents(
  importSuccess: Flow<ImportSummary>,
  snackbarHostState: SnackbarHostState,
) {
  LaunchedEffect(Unit) {
    importSuccess.collectLatest { summary ->
      snackbarHostState.showSnackbar(buildImportSuccessMessage(summary))
    }
  }
}

@Composable
private fun rememberImportBackupLauncher(
  onImport: (Uri) -> Unit,
): ManagedActivityResultLauncher<Array<String>, Uri?> {
  return rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    uri?.let(onImport)
  }
}

private fun SettingsError.toUserMessage(): String =
  when (this) {
    is SettingsError.ProviderAddFailed -> "Failed to add provider: ${message}"
    is SettingsError.ProviderUpdateFailed -> "Failed to update provider: ${message}"
    is SettingsError.ProviderDeleteFailed -> "Failed to delete provider: ${message}"
    is SettingsError.ExportFailed -> "Export failed: ${message}"
    is SettingsError.ImportFailed -> "Import failed: ${message}"
    is SettingsError.PreferenceUpdateFailed -> "Failed to update preference: ${message}"
    is SettingsError.UnexpectedError -> "Unexpected error: ${message}"
  }

private fun exportSuccessMessage(path: String): String =
  "Backup exported to ${path.substringAfterLast('/')}…"

private fun buildImportSuccessMessage(summary: ImportSummary): String {
  val personasTotal = summary.personasImported + summary.personasUpdated
  val providersTotal = summary.providersImported + summary.providersUpdated
  return buildString {
    append("Imported backup: ")
    append("$personasTotal persona${if (personasTotal == 1) "" else "s"}")
    append(", ")
    append("$providersTotal provider${if (providersTotal == 1) "" else "s"}")
  }
}

private fun SettingsViewModel.exportBackupToDownloads(includeChatHistory: Boolean = true) {
  val downloadsPath =
    android.os.Environment.getExternalStoragePublicDirectory(
        android.os.Environment.DIRECTORY_DOWNLOADS,
      )
      .absolutePath + "/nanoai-backup-${System.currentTimeMillis()}.json"
  exportBackup(downloadsPath, includeChatHistory)
}

private fun handleExportRequest(
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

private fun confirmExport(
  dontShowAgain: Boolean,
  dismissWarnings: () -> Unit,
  export: () -> Unit,
) {
  if (dontShowAgain) {
    dismissWarnings()
  }
  export()
}

private fun saveProvider(
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
        apiType = com.vjaykrsna.nanoai.core.model.APIType.OPENAI_COMPATIBLE,
        isEnabled = true,
      ),
    )
}

private fun createSettingsActions(
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

private data class SettingsContentState(
  val apiProviders: List<APIProviderConfig>,
  val privacyPreferences: PrivacyPreference,
  val uiUxState: SettingsUiUxState,
)

private data class SettingsScreenActions(
  val onAddProviderClick: () -> Unit,
  val onProviderEdit: (APIProviderConfig) -> Unit,
  val onProviderDelete: (APIProviderConfig) -> Unit,
  val onImportBackupClick: () -> Unit,
  val onExportBackupClick: () -> Unit,
  val onTelemetryToggle: (Boolean) -> Unit,
  val onRetentionPolicyChange: (RetentionPolicy) -> Unit,
  val onDismissMigrationSuccess: () -> Unit,
)

@Composable
private fun SettingsScreenContent(
  state: SettingsContentState,
  snackbarHostState: SnackbarHostState,
  actions: SettingsScreenActions,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    floatingActionButton = {
      FloatingActionButton(
        onClick = actions.onAddProviderClick,
        modifier = Modifier.semantics { contentDescription = "Add API provider" },
      ) {
        Icon(Icons.Default.Add, "Add")
      }
    },
    modifier =
      modifier.semantics {
        contentDescription = "Settings screen with API providers and privacy options"
      },
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.fillMaxSize().padding(innerPadding),
    ) {
      item { SettingsHeader() }

      if (state.uiUxState.showMigrationSuccessNotification) {
        item { MigrationSuccessCard(onDismiss = actions.onDismissMigrationSuccess) }
      }

      item { ApiProvidersSectionHeader(hasProviders = state.apiProviders.isNotEmpty()) }

      items(
        items = state.apiProviders,
        key = { it.providerId },
        contentType = { "api_provider_card" },
      ) { provider ->
        ApiProviderCard(
          provider = provider,
          onEdit = { actions.onProviderEdit(provider) },
          onDelete = { actions.onProviderDelete(provider) },
        )
      }

      item {
        DataManagementSection(
          onImportBackupClick = actions.onImportBackupClick,
          onExportBackupClick = actions.onExportBackupClick,
        )
      }

      item {
        PrivacySection(
          privacyPreferences = state.privacyPreferences,
          onTelemetryToggle = actions.onTelemetryToggle,
          onRetentionPolicyChange = actions.onRetentionPolicyChange,
        )
      }
    }
  }
}

@Composable
private fun SettingsHeader(modifier: Modifier = Modifier) {
  Text(
    text = "Settings",
    style = MaterialTheme.typography.headlineMedium,
    fontWeight = FontWeight.Bold,
    modifier = modifier,
  )
}

@Composable
private fun ApiProvidersSectionHeader(
  hasProviders: Boolean,
  modifier: Modifier = Modifier,
) {
  SettingsSection(title = "API Providers", modifier = modifier) {
    if (!hasProviders) {
      Text(
        text = "No API providers configured",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 16.dp),
      )
    }
  }
}

@Composable
private fun DataManagementSection(
  onImportBackupClick: () -> Unit,
  onExportBackupClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingsSection(title = "Data Management", modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      DataManagementCard(
        title = "Import Backup",
        description = "Restore personas, providers, and settings from a backup file",
        icon = Icons.Default.Add,
        contentDescription = "Import",
        onClick = onImportBackupClick,
      )

      DataManagementCard(
        title = "Export Backup",
        description = "Export conversations, personas, and settings",
        icon = Icons.Default.Edit,
        contentDescription = "Export",
        onClick = onExportBackupClick,
      )
    }
  }
}

@Composable
private fun DataManagementCard(
  title: String,
  description: String,
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium,
        )
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Icon(icon, contentDescription)
    }
  }
}

@Composable
private fun PrivacySection(
  privacyPreferences: PrivacyPreference,
  onTelemetryToggle: (Boolean) -> Unit,
  onRetentionPolicyChange: (RetentionPolicy) -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingsSection(title = "Privacy & Telemetry", modifier = modifier) {
    PrivacySettings(
      preferences = privacyPreferences,
      onTelemetryToggle = onTelemetryToggle,
      onRetentionPolicyChange = onRetentionPolicyChange,
    )
  }
}

@Composable
fun MigrationSuccessCard(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
      ),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Migration Successful",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text =
          "Your provider credentials have been migrated to a more secure storage. " +
            "For enhanced security, please rotate your provider credentials.",
        style = MaterialTheme.typography.bodyMedium,
      )
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.align(Alignment.End),
      ) {
        Text("Dismiss")
      }
    }
  }
}

@Composable
private fun SettingsDialogs(
  showAddProviderDialog: Boolean,
  editingProvider: APIProviderConfig?,
  showExportDialog: Boolean,
  onProviderDismiss: () -> Unit,
  onProviderSave: (name: String, baseUrl: String, apiKey: String?) -> Unit,
  onExportDismiss: () -> Unit,
  onExportConfirm: (dontShowAgain: Boolean) -> Unit,
) {
  val isProviderDialogVisible = showAddProviderDialog || editingProvider != null

  ProviderDialogHost(
    isVisible = isProviderDialogVisible,
    provider = editingProvider,
    onDismiss = onProviderDismiss,
    onSave = onProviderSave,
  )

  ExportDialogHost(
    isVisible = showExportDialog,
    onDismiss = onExportDismiss,
    onConfirm = onExportConfirm,
  )
}

@Composable
private fun ProviderDialogHost(
  isVisible: Boolean,
  provider: APIProviderConfig?,
  onDismiss: () -> Unit,
  onSave: (name: String, baseUrl: String, apiKey: String?) -> Unit,
) {
  if (isVisible) {
    ApiProviderDialog(
      provider = provider,
      onDismiss = onDismiss,
      onSave = onSave,
    )
  }
}

@Composable
private fun ExportDialogHost(
  isVisible: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (dontShowAgain: Boolean) -> Unit,
) {
  if (isVisible) {
    ExportDialog(
      onDismiss = onDismiss,
      onConfirm = onConfirm,
    )
  }
}

private fun RetentionPolicy.displayLabel(): String =
  when (this) {
    RetentionPolicy.INDEFINITE -> "Keep indefinitely"
    RetentionPolicy.MANUAL_PURGE_ONLY -> "Manual purge only"
  }

@Composable
private fun SettingsSection(
  title: String,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(12.dp))
    content()
  }
}

@Composable
private fun ApiProviderCard(
  provider: APIProviderConfig,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = provider.providerName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = provider.baseUrl,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Type: ${provider.apiType.name}",
          style = MaterialTheme.typography.labelSmall,
          color =
            if (provider.isEnabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
        )
      }

      Row {
        IconButton(
          onClick = onEdit,
          modifier = Modifier.semantics { contentDescription = "Edit ${provider.providerName}" },
        ) {
          Icon(Icons.Default.Edit, "Edit")
        }
        IconButton(
          onClick = onDelete,
          modifier = Modifier.semantics { contentDescription = "Delete ${provider.providerName}" },
        ) {
          Icon(Icons.Default.Delete, "Delete")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiProviderDialog(
  provider: APIProviderConfig?,
  onDismiss: () -> Unit,
  onSave: (name: String, baseUrl: String, apiKey: String?) -> Unit,
  modifier: Modifier = Modifier
) {
  var name by remember { mutableStateOf(provider?.providerName ?: "") }
  var baseUrl by remember { mutableStateOf(provider?.baseUrl ?: "") }
  var apiKey by remember { mutableStateOf(provider?.apiKey ?: "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (provider != null) "Edit API Provider" else "Add API Provider") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = baseUrl,
          onValueChange = { baseUrl = it },
          label = { Text("Base URL") },
          placeholder = { Text("https://api.example.com") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = apiKey,
          onValueChange = { apiKey = it },
          label = { Text("API Key (Optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (name.isNotBlank() && baseUrl.isNotBlank()) {
            onSave(name, baseUrl, apiKey.ifBlank { null })
          }
        },
        enabled = name.isNotBlank() && baseUrl.isNotBlank(),
      ) {
        Text("Save")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    modifier = modifier,
  )
}

@Composable
@VisibleForTesting
internal fun ExportDialog(
  onDismiss: () -> Unit,
  onConfirm: (dontShowAgain: Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  var dontShowAgain by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Export Backup") },
    text = {
      ExportDialogBody(
        dontShowAgain = dontShowAgain,
        onDontShowAgainChange = { dontShowAgain = it },
      )
    },
    confirmButton = {
      TextButton(
        onClick = { onConfirm(dontShowAgain) },
        modifier = Modifier.semantics { contentDescription = "Confirm export backup" },
      ) {
        Text("Export")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.semantics { contentDescription = "Cancel export backup" },
      ) {
        Text("Cancel")
      }
    },
    modifier = modifier,
  )
}

@Composable
private fun ExportDialogBody(
  dontShowAgain: Boolean,
  onDontShowAgainChange: (Boolean) -> Unit,
) {
  Column {
    Text("This will export all your data including:")
    Spacer(modifier = Modifier.height(8.dp))
    Text("• Conversations and messages")
    Text("• Persona profiles")
    Text("• API provider configurations")
    Text("• Settings and preferences")
    Spacer(modifier = Modifier.height(12.dp))
    Text(
      "The backup will be saved to your Downloads folder.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
      "Backups are not encrypted. Store the exported JSON securely and delete it when finished.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.error,
      fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(12.dp))
    ExportDialogCheckbox(
      checked = dontShowAgain,
      onCheckedChange = onDontShowAgainChange,
    )
  }
}

@Composable
private fun ExportDialogCheckbox(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Checkbox(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.semantics { contentDescription = "Don't warn me again checkbox" },
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = "Don't warn me again",
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}

@Composable
private fun PrivacySettings(
  preferences: PrivacyPreference?,
  onTelemetryToggle: (Boolean) -> Unit,
  onRetentionPolicyChange: (RetentionPolicy) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
      ),
  ) {
    val telemetryOptIn = preferences?.telemetryOptIn ?: false
    val selectedPolicy = preferences?.retentionPolicy ?: RetentionPolicy.INDEFINITE
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      TelemetryPreferenceRow(
        telemetryOptIn = telemetryOptIn,
        onTelemetryToggle = onTelemetryToggle,
      )

      HorizontalDivider()

      RetentionPolicySection(
        selectedPolicy = selectedPolicy,
        onRetentionPolicyChange = onRetentionPolicyChange,
      )

      HorizontalDivider()

      PrivacyNoticeSection()
    }
  }
}

@Composable
private fun TelemetryPreferenceRow(
  telemetryOptIn: Boolean,
  onTelemetryToggle: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = "Usage Analytics",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
      )
      Text(
        text = "Help improve the app by sharing anonymous usage data",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = telemetryOptIn,
      onCheckedChange = onTelemetryToggle,
      modifier = Modifier.semantics { contentDescription = "Toggle usage analytics" },
    )
  }
}

@Composable
private fun RetentionPolicySection(
  selectedPolicy: RetentionPolicy,
  onRetentionPolicyChange: (RetentionPolicy) -> Unit,
) {
  Column {
    Text(
      text = "Message Retention",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Medium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Message retention policy: ${selectedPolicy.name}",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
      modifier = Modifier.horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      RetentionPolicy.values().forEach { policy ->
        FilterChip(
          selected = selectedPolicy == policy,
          onClick = { onRetentionPolicyChange(policy) },
          label = { Text(policy.displayLabel()) },
        )
      }
    }
  }
}

@Composable
private fun PrivacyNoticeSection() {
  Column {
    Text(
      text = "Privacy Notice",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Medium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "All data is stored locally on your device.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
