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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityBannerState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus

/** Elevated banner communicating connectivity state and queued actions. */
@Composable
fun ConnectivityBanner(
  state: ConnectivityBannerState,
  onCtaClick: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val visuals =
    remember(state.status, colorScheme) { connectivityBannerVisuals(state.status, colorScheme) }
  val description =
    remember(state, visuals) { buildBannerContentDescription(state, visuals.statusLabel) }

  Surface(
    modifier = modifier.bannerSemantics(description, visuals.statusLabel),
    color = visuals.containerColor,
    contentColor = visuals.contentColor,
    tonalElevation = 2.dp,
    shape = MaterialTheme.shapes.large,
  ) {
    ConnectivityBannerContent(
      state = state,
      visuals = visuals,
      onCtaClick = onCtaClick,
      onDismiss = onDismiss,
    )
  }
}

@Composable
private fun ConnectivityBannerContent(
  state: ConnectivityBannerState,
  visuals: ConnectivityBannerVisuals,
  onCtaClick: () -> Unit,
  onDismiss: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(visuals.icon, contentDescription = "${visuals.statusLabel} icon")
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
      Button(onClick = onCtaClick, modifier = Modifier.testTag("connectivity_banner_cta")) {
        Text(label)
      }
    }
    IconButton(onClick = onDismiss, modifier = Modifier.testTag("connectivity_banner_close")) {
      Icon(Icons.Outlined.Close, contentDescription = "Dismiss connectivity banner")
    }
  }
}

private fun Modifier.bannerSemantics(description: String, statusLabel: String): Modifier =
  semantics {
    contentDescription = description
    stateDescription = statusLabel
  }

private fun connectivityBannerVisuals(
  status: ConnectivityStatus,
  colorScheme: androidx.compose.material3.ColorScheme,
): ConnectivityBannerVisuals {
  return when (status) {
    ConnectivityStatus.ONLINE ->
      ConnectivityBannerVisuals(
        containerColor = colorScheme.secondaryContainer,
        contentColor = colorScheme.onSecondaryContainer,
        icon = Icons.Rounded.Wifi,
        statusLabel = "Online",
      )
    ConnectivityStatus.OFFLINE ->
      ConnectivityBannerVisuals(
        containerColor = colorScheme.errorContainer,
        contentColor = colorScheme.onErrorContainer,
        icon = Icons.Rounded.CloudOff,
        statusLabel = "Offline",
      )
    ConnectivityStatus.LIMITED ->
      ConnectivityBannerVisuals(
        containerColor = colorScheme.tertiaryContainer,
        contentColor = colorScheme.onTertiaryContainer,
        icon = Icons.Rounded.Speed,
        statusLabel = "Limited connectivity",
      )
  }
}

private fun buildBannerContentDescription(
  state: ConnectivityBannerState,
  statusLabel: String,
): String {
  return buildString {
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
}

private data class ConnectivityBannerVisuals(
  val containerColor: Color,
  val contentColor: Color,
  val icon: ImageVector,
  val statusLabel: String,
)
