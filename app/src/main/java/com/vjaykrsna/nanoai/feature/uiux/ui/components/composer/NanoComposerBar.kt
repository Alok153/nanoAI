package com.vjaykrsna.nanoai.feature.uiux.ui.components.composer

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing

@Composable
fun NanoComposerBar(
  value: String,
  onValueChange: (String) -> Unit,
  onSend: () -> Unit,
  modifier: Modifier = Modifier,
  placeholder: String = "Type a message",
  enabled: Boolean = true,
  sendEnabled: Boolean = true,
  isSending: Boolean = false,
  onImageSelect: () -> Unit = {},
  onAudioRecord: () -> Unit = {},
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.padding(vertical = NanoSpacing.sm),
  ) {
    ComposerAttachmentButtons(
      enabled = enabled,
      onImageSelect = onImageSelect,
      onAudioRecord = onAudioRecord,
    )
    ComposerInputField(
      value = value,
      onValueChange = onValueChange,
      placeholder = placeholder,
      enabled = enabled,
    )
    Spacer(modifier = Modifier.width(NanoSpacing.sm))
    ComposerSendSection(isSending = isSending, sendEnabled = sendEnabled, onSend = onSend)
  }
}

@Composable
private fun ComposerAttachmentButtons(
  enabled: Boolean,
  onImageSelect: () -> Unit,
  onAudioRecord: () -> Unit,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    IconButton(
      onClick = onImageSelect,
      enabled = enabled,
      modifier = Modifier.semantics { contentDescription = "Attach an image" },
    ) {
      Icon(
        imageVector = Icons.Default.AddAPhoto,
        contentDescription = "Attach image",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    IconButton(
      onClick = onAudioRecord,
      enabled = enabled,
      modifier = Modifier.semantics { contentDescription = "Record audio" },
    ) {
      Icon(
        imageVector = Icons.Default.Mic,
        contentDescription = "Record audio",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun RowScope.ComposerInputField(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  enabled: Boolean,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = Modifier.weight(1f),
    placeholder = { Text(text = placeholder) },
    enabled = enabled,
    keyboardOptions =
      KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
  )
}

@Composable
private fun ComposerSendSection(isSending: Boolean, sendEnabled: Boolean, onSend: () -> Unit) {
  AnimatedContent(targetState = isSending, label = "composer_send_state") { sending ->
    if (sending) {
      CircularProgressIndicator(modifier = Modifier.size(24.dp))
    } else {
      IconButton(
        onClick = onSend,
        enabled = sendEnabled,
        modifier = Modifier.semantics { contentDescription = "Send message" },
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = "Send",
          tint =
            if (sendEnabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
      }
    }
  }
}
