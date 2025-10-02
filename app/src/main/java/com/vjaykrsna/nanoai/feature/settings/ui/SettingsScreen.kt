package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vjaykrsna.nanoai.core.data.preferences.RetentionPolicy
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsError
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest
import java.util.UUID

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val apiProviders by viewModel.apiProviders.collectAsState()
    val privacyPreferences by viewModel.privacyPreferences.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddProviderDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var editingProvider by remember { mutableStateOf<APIProviderConfig?>(null) }

    val importBackupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                viewModel.importBackup(uri)
            }
        }

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collectLatest { error ->
            val message =
                when (error) {
                    is SettingsError.ProviderAddFailed -> "Failed to add provider: ${error.message}"
                    is SettingsError.ProviderUpdateFailed -> "Failed to update provider: ${error.message}"
                    is SettingsError.ProviderDeleteFailed -> "Failed to delete provider: ${error.message}"
                    is SettingsError.ExportFailed -> "Export failed: ${error.message}"
                    is SettingsError.ImportFailed -> "Import failed: ${error.message}"
                    is SettingsError.PreferenceUpdateFailed -> "Failed to update preference: ${error.message}"
                    is SettingsError.UnexpectedError -> "Unexpected error: ${error.message}"
                }
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.exportSuccess.collectLatest { path ->
            snackbarHostState.showSnackbar("Backup exported to ${path.substringAfterLast('/')}…")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.importSuccess.collectLatest { summary ->
            val personasTotal = summary.personasImported + summary.personasUpdated
            val providersTotal = summary.providersImported + summary.providersUpdated
            val message =
                buildString {
                    append("Imported backup: ")
                    append("$personasTotal persona${if (personasTotal == 1) "" else "s"}")
                    append(", ")
                    append("$providersTotal provider${if (providersTotal == 1) "" else "s"}")
                }
            snackbarHostState.showSnackbar(message)
        }
    }

    fun triggerExport(includeChatHistory: Boolean = true) {
        val downloadsPath =
            android.os.Environment
                .getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS,
                ).absolutePath + "/nanoai-backup-${System.currentTimeMillis()}.json"
        viewModel.exportBackup(downloadsPath, includeChatHistory)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddProviderDialog = true },
                modifier =
                    Modifier.semantics {
                        contentDescription = "Add API provider"
                    },
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
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            // Header
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            // API Providers Section
            item {
                SettingsSection(title = "API Providers") {
                    if (apiProviders.isEmpty()) {
                        Text(
                            text = "No API providers configured",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }
            }

            items(
                items = apiProviders,
                key = { it.providerId.toString() },
                contentType = { "api_provider_card" },
            ) { provider ->
                ApiProviderCard(
                    provider = provider,
                    onEdit = { editingProvider = provider },
                    onDelete = { viewModel.deleteApiProvider(provider.providerId) },
                )
            }

            // Export Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSection(title = "Data Management") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            importBackupLauncher.launch(
                                                arrayOf("application/json", "application/zip", "application/octet-stream"),
                                            )
                                        }.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Import Backup",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = "Restore personas, providers, and settings from a backup file",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(Icons.Default.Add, "Import")
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (privacyPreferences.exportWarningsDismissed) {
                                                triggerExport()
                                            } else {
                                                showExportDialog = true
                                            }
                                        }.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Export Backup",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = "Export conversations, personas, and settings",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(Icons.Default.Edit, "Export")
                            }
                        }
                    }
                }
            }

            // Privacy Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSection(title = "Privacy & Telemetry") {
                    PrivacySettings(
                        preferences = privacyPreferences,
                        onTelemetryToggle = { viewModel.setTelemetryOptIn(it) },
                        onRetentionPolicyChanged = { viewModel.setRetentionPolicy(it) },
                    )
                }
            }
        }
    }

    // Add/Edit Provider Dialog
    if (showAddProviderDialog || editingProvider != null) {
        ApiProviderDialog(
            provider = editingProvider,
            onDismiss = {
                showAddProviderDialog = false
                editingProvider = null
            },
            onSave = { name, baseUrl, apiKey ->
                val provider = editingProvider
                if (provider != null) {
                    viewModel.updateApiProvider(
                        provider.copy(
                            providerName = name,
                            baseUrl = baseUrl,
                            apiKey = apiKey ?: "",
                        ),
                    )
                } else {
                    viewModel.addApiProvider(
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
                showAddProviderDialog = false
                editingProvider = null
            },
        )
    }

    // Export Dialog
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onConfirm = { dontShowAgain ->
                if (dontShowAgain) {
                    viewModel.dismissExportWarnings()
                }
                triggerExport()
                showExportDialog = false
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
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
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                    color = if (provider.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }

            Row {
                IconButton(
                    onClick = onEdit,
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Edit ${provider.providerName}"
                        },
                ) {
                    Icon(Icons.Default.Edit, "Edit")
                }
                IconButton(
                    onClick = onDelete,
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Delete ${provider.providerName}"
                        },
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
    modifier: Modifier = Modifier,
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
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier,
    )
}

@Composable
@VisibleForTesting
internal fun ExportDialog(
    onDismiss: () -> Unit,
    onConfirm: (dontShowAgain: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dontShowAgain by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Backup") },
        text = {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = { dontShowAgain = it },
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Don't warn me again checkbox"
                            },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Don't warn me again",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(dontShowAgain) },
                modifier =
                    Modifier.semantics {
                        contentDescription = "Confirm export backup"
                    },
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier =
                    Modifier.semantics {
                        contentDescription = "Cancel export backup"
                    },
            ) {
                Text("Cancel")
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun PrivacySettings(
    preferences: com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreference?,
    onTelemetryToggle: (Boolean) -> Unit,
    onRetentionPolicyChanged: (RetentionPolicy) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Telemetry toggle
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
                    checked = preferences?.telemetryOptIn ?: false,
                    onCheckedChange = onTelemetryToggle,
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Toggle usage analytics"
                        },
                )
            }

            HorizontalDivider()

            // Retention policy
            Column {
                Text(
                    text = "Message Retention",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Message retention policy: ${preferences?.retentionPolicy?.name ?: RetentionPolicy.INDEFINITE.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            androidx.compose.material3.HorizontalDivider()

            // Consent info
            Column {
                Text(
                    text = "Privacy Notice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "All data is stored locally on your device. No cloud sync or external data sharing occurs unless you explicitly export your data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
