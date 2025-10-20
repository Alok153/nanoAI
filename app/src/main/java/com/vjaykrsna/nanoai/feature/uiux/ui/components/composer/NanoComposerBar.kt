package com.vjaykrsna.nanoai.feature.uiux.ui.components.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoElevation
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoRadii
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing
import com.vjaykrsna.nanoai.feature.uiux.ui.components.primitives.NanoInputField

@Composable
fun NanoComposerBar(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  minLines: Int = 1,
  maxLines: Int = 6,
  leadingActions: (@Composable RowScope.() -> Unit)? = null,
  trailingActions: (@Composable RowScope.() -> Unit)? = null,
  onSend: (() -> Unit)? = null,
  sendEnabled: Boolean = value.isNotBlank() && enabled,
  sendIcon: ImageVector = Icons.AutoMirrored.Filled.Send,
  sendButtonContentDescription: String = "Send message",
  isSending: Boolean = false,
  keyboardOptions: KeyboardOptions =
    KeyboardOptions.Default.copy(
      imeAction = if (onSend != null) ImeAction.Send else KeyboardOptions.Default.imeAction
    ),
  keyboardActions: KeyboardActions =
    if (onSend != null) {
      KeyboardActions(onSend = { if (sendEnabled && !isSending) onSend() })
    } else {
      KeyboardActions.Default
    },
) {
  Surface(
    modifier = modifier.semantics { contentDescription = "Composer bar" },
    shape = RoundedCornerShape(NanoRadii.extraLarge),
    tonalElevation = NanoElevation.level1,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Row(
      modifier =
        Modifier.padding(horizontal = NanoSpacing.md, vertical = NanoSpacing.sm)
          .heightIn(min = 56.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(NanoSpacing.sm),
    ) {
      leadingActions?.let { actions ->
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(NanoSpacing.xs),
          content = actions,
        )
        Spacer(modifier = Modifier.width(NanoSpacing.sm))
      }

      val resolvedPlaceholder = placeholder.ifBlank { "Type a message" }

      val trailingIconContent: @Composable (() -> Unit)? =
        if (trailingActions != null || onSend != null) {
          {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(NanoSpacing.xs),
            ) {
              trailingActions?.invoke(this)

              if (onSend != null) {
                if (isSending) {
                  CircularProgressIndicator(
                    modifier =
                      Modifier.size(18.dp).semantics { contentDescription = "Sending message" },
                    strokeWidth = 2.dp,
                  )
                } else {
                  IconButton(onClick = { if (sendEnabled) onSend() }, enabled = sendEnabled) {
                    Icon(imageVector = sendIcon, contentDescription = sendButtonContentDescription)
                  }
                }
              }
            }
          }
        } else {
          null
        }

      NanoInputField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.weight(1f),
        placeholder = resolvedPlaceholder,
        enabled = enabled && !isSending,
        singleLine = false,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        trailingIcon = trailingIconContent,
      )
    }
  }
}
