package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.settings.domain.huggingface.HuggingFaceDeviceAuthState

@Composable
internal fun HuggingFaceLoginDialogHost(
  isVisible: Boolean,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
  deviceAuthState: HuggingFaceDeviceAuthState?,
) {
  if (!isVisible) return

  HuggingFaceLoginDialog(
    onDismiss = onDismiss,
    onConfirm = onConfirm,
    deviceAuthState = deviceAuthState,
  )
}

@Composable
private fun HuggingFaceLoginDialog(
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
  deviceAuthState: HuggingFaceDeviceAuthState?,
  modifier: Modifier = Modifier,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Sign in to Hugging Face") },
    text = { HuggingFaceLoginDialogBody(deviceAuthState) },
    confirmButton = {
      TextButton(onClick = onConfirm, enabled = deviceAuthState == null) {
        Text(if (deviceAuthState == null) "Start sign-in" else "Waiting...")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    modifier = modifier,
  )
}

@Composable
private fun HuggingFaceLoginDialogBody(deviceAuthState: HuggingFaceDeviceAuthState?) {
  val clipboardManager = LocalClipboardManager.current
  val uriHandler = LocalUriHandler.current

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    if (deviceAuthState == null) {
      Text(
        text =
          "You will authenticate in the browser and authorize nanoAI to access your " +
            "Hugging Face models.",
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        text =
          "Select \"Start sign-in\" to generate a one-time code. Use it on " +
            "huggingface.co when prompted.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    } else {
      val verificationUrl =
        deviceAuthState.verificationUriComplete ?: deviceAuthState.verificationUri

      Text(
        text = "Enter this code on Hugging Face:",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
      )

      SelectionContainer {
        Text(
          text = deviceAuthState.userCode,
          style =
            MaterialTheme.typography.headlineSmall.copy(
              fontFamily = FontFamily.Monospace,
              textAlign = TextAlign.Center,
            ),
          modifier = Modifier.fillMaxWidth(),
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(
          onClick = { clipboardManager.setText(AnnotatedString(deviceAuthState.userCode)) }
        ) {
          Text("Copy code")
        }
        TextButton(onClick = { deviceAuthState.verificationUri?.let(uriHandler::openUri) }) {
          Text("Open site")
        }
      }

      Spacer(Modifier.height(8.dp))

      Text(
        text = "Complete the flow at $verificationUrl",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (deviceAuthState.isPolling) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp))
          Text(
            text = "Waiting for confirmation…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      deviceAuthState.lastError?.let { message ->
        val announcement = deviceAuthState.lastErrorAnnouncement ?: message
        Text(
          text = message,
          modifier =
            Modifier.semantics {
              contentDescription = announcement
              liveRegion = LiveRegionMode.Assertive
            },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }
}

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
        enabled = apiKey.trim().isNotEmpty()
      ) {
        Text("Save")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    modifier = modifier,
  )
}
