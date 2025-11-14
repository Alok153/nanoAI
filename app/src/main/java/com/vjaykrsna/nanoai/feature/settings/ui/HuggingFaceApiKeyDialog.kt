package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun HuggingFaceApiKeyDialogHost(
  isVisible: Boolean,
  onDismiss: () -> Unit,
  onSave: (apiKey: String) -> Unit,
) {
  if (!isVisible) return

  HuggingFaceApiKeyDialog(onDismiss = onDismiss, onSave = onSave)
}

@Composable
private fun HuggingFaceApiKeyDialog(
  onDismiss: () -> Unit,
  onSave: (apiKey: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var apiKey by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Enter Hugging Face API Key") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text =
            "Enter your Hugging Face API token. You can generate one at " +
              "huggingface.co/settings/tokens",
          style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
          value = apiKey,
          onValueChange = { apiKey = it },
          label = { Text("API Token") },
          placeholder = { Text("hf_...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          onSave(apiKey.trim())
          onDismiss()
        },
        enabled = apiKey.trim().isNotEmpty(),
      ) {
        Text("Save")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    modifier = modifier,
  )
}
