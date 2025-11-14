package com.vjaykrsna.nanoai.feature.settings.ui

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceDeviceAuthState

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
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    if (deviceAuthState == null) {
      HuggingFaceLoginInstructions()
    } else {
      HuggingFaceDeviceAuthDetails(state = deviceAuthState)
    }
  }
}

@Composable
private fun HuggingFaceLoginInstructions() {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text =
        "You will authenticate in the browser and authorize nanoAI to access your " +
          "Hugging Face models.",
      style = MaterialTheme.typography.bodyMedium,
    )
    Text(
      text =
        "Select \"Start sign-in\" to generate a one-time code. Use it on huggingface.co when prompted.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun HuggingFaceDeviceAuthDetails(state: HuggingFaceDeviceAuthState) {
  val context = LocalContext.current
  val uriHandler = LocalUriHandler.current
  val verificationUrl = state.verificationUriComplete ?: state.verificationUri

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
      text = "Enter this code on Hugging Face:",
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
    )

    SelectionContainer {
      Text(
        text = state.userCode,
        style =
          MaterialTheme.typography.headlineSmall.copy(
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
          ),
        modifier = Modifier.fillMaxWidth(),
      )
    }

    DeviceAuthActions(state = state, uriHandler = uriHandler, context = context)

    Text(
      text = "Complete the flow at $verificationUrl",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    DeviceAuthPollingIndicator(isPolling = state.isPolling)
    DeviceAuthError(state = state)
  }
}

@Composable
private fun DeviceAuthActions(
  state: HuggingFaceDeviceAuthState,
  uriHandler: androidx.compose.ui.platform.UriHandler,
  context: android.content.Context,
) {
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    TextButton(onClick = { copyUserCode(context, state.userCode) }) { Text("Copy code") }
    TextButton(onClick = { state.verificationUri?.let(uriHandler::openUri) }) { Text("Open site") }
  }
}

private fun copyUserCode(context: android.content.Context, userCode: String) {
  val clipboardManager =
    context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
      as android.content.ClipboardManager
  val clipData = ClipData.newPlainText("user code", userCode)
  clipboardManager.setPrimaryClip(clipData)
}

@Composable
private fun DeviceAuthPollingIndicator(isPolling: Boolean) {
  if (!isPolling) return

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    CircularProgressIndicator(modifier = Modifier.size(20.dp))
    Text(
      text = "Waiting for confirmation…",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun DeviceAuthError(state: HuggingFaceDeviceAuthState) {
  val message = state.lastError ?: return
  val announcement = state.lastErrorAnnouncement ?: message

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
