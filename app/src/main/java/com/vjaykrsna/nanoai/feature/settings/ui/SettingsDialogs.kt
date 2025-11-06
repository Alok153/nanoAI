package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceDeviceAuthState

@Composable
internal fun SettingsDialogs(
  showAddProviderDialog: Boolean,
  editingProvider: APIProviderConfig?,
  showExportDialog: Boolean,
  onProviderDismiss: () -> Unit,
  onProviderSave: (name: String, baseUrl: String, apiKey: String?) -> Unit,
  onExportDismiss: () -> Unit,
  onExportConfirm: (dontShowAgain: Boolean) -> Unit,
  showHuggingFaceLoginDialog: Boolean = false,
  showHuggingFaceApiKeyDialog: Boolean = false,
  huggingFaceDeviceAuthState: HuggingFaceDeviceAuthState? = null,
  onHuggingFaceLoginDismiss: () -> Unit = {},
  onHuggingFaceLoginConfirm: () -> Unit = {},
  onHuggingFaceApiKeyDismiss: () -> Unit = {},
  onHuggingFaceApiKeySave: (apiKey: String) -> Unit = {},
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

  HuggingFaceLoginDialogHost(
    isVisible = showHuggingFaceLoginDialog,
    onDismiss = onHuggingFaceLoginDismiss,
    onConfirm = onHuggingFaceLoginConfirm,
    deviceAuthState = huggingFaceDeviceAuthState,
  )

  HuggingFaceApiKeyDialogHost(
    isVisible = showHuggingFaceApiKeyDialog,
    onDismiss = onHuggingFaceApiKeyDismiss,
    onSave = onHuggingFaceApiKeySave,
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
    ApiProviderDialog(provider = provider, onDismiss = onDismiss, onSave = onSave)
  }
}

@Composable
private fun ExportDialogHost(
  isVisible: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (dontShowAgain: Boolean) -> Unit,
) {
  if (isVisible) {
    ExportDialog(onDismiss = onDismiss, onConfirm = onConfirm)
  }
}

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
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
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
private fun ExportDialogBody(dontShowAgain: Boolean, onDontShowAgainChange: (Boolean) -> Unit) {
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
    ExportDialogCheckbox(checked = dontShowAgain, onCheckedChange = onDontShowAgainChange)
  }
}

@Composable
private fun ExportDialogCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Checkbox(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.semantics { contentDescription = "Don't warn me again checkbox" },
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(text = "Don't warn me again", style = MaterialTheme.typography.bodyMedium)
  }
}
