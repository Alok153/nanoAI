package com.vjaykrsna.nanoai.feature.uiux.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityBannerState
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus

/** Elevated banner communicating connectivity state and queued actions. */
@Composable
fun ConnectivityBanner(
  state: ConnectivityBannerState,
  onCtaClick: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val containerColor =
    when (state.status) {
      ConnectivityStatus.ONLINE -> MaterialTheme.colorScheme.secondaryContainer
      ConnectivityStatus.OFFLINE -> MaterialTheme.colorScheme.errorContainer
      ConnectivityStatus.LIMITED -> MaterialTheme.colorScheme.tertiaryContainer
    }
  val contentColor =
    when (state.status) {
      ConnectivityStatus.ONLINE -> MaterialTheme.colorScheme.onSecondaryContainer
      ConnectivityStatus.OFFLINE -> MaterialTheme.colorScheme.onErrorContainer
      ConnectivityStatus.LIMITED -> MaterialTheme.colorScheme.onTertiaryContainer
    }
  val icon =
    when (state.status) {
      ConnectivityStatus.ONLINE -> Icons.Rounded.Wifi
      ConnectivityStatus.OFFLINE -> Icons.Rounded.CloudOff
      ConnectivityStatus.LIMITED -> Icons.Rounded.Speed
    }
  val statusLabel =
    when (state.status) {
      ConnectivityStatus.ONLINE -> "Online"
      ConnectivityStatus.OFFLINE -> "Offline"
      ConnectivityStatus.LIMITED -> "Limited connectivity"
    }

  Surface(
    modifier =
      modifier.semantics {
        contentDescription = buildString {
          append(statusLabel)
          append(", ")
          append(state.headline)
          state.supportingText
            .takeIf { it.isNotBlank() }
            ?.let { text ->
              append(". ")
              append(text)
            }
          if (state.queuedActionCount > 0) {
            append(". ")
            append("Queued actions: ${state.queuedActionCount}")
          }
        }
        stateDescription = statusLabel
      },
    color = containerColor,
    contentColor = contentColor,
    tonalElevation = 2.dp,
    shape = MaterialTheme.shapes.large,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(icon, contentDescription = "$statusLabel icon")
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = state.headline,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = state.supportingText,
          style = MaterialTheme.typography.bodySmall,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      state.ctaLabel?.let { label ->
        Button(
          onClick = onCtaClick,
          modifier = Modifier.testTag("connectivity_banner_cta"),
        ) {
          Text(label)
        }
      }
      IconButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("connectivity_banner_close"),
      ) {
        Icon(Icons.Outlined.Close, contentDescription = "Dismiss connectivity banner")
      }
    }
  }
}
