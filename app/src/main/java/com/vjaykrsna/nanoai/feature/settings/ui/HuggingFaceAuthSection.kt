package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceAuthState
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceTokenSource
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing

@Composable
internal fun HuggingFaceAuthSectionHeader(modifier: Modifier = Modifier) {
  SettingsSection(title = "Hugging Face", modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
        text = "Connect your Hugging Face account to access models and datasets.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
internal fun HuggingFaceAuthCard(
  state: HuggingFaceAuthState,
  onLoginClick: () -> Unit,
  onApiKeyClick: () -> Unit,
  onDisconnectClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingsCard(
    title = "Hugging Face Account",
    modifier = modifier,
    showInfoButton = true,
    infoContent = { HuggingFaceInfoSheet() },
    content = {
      Column(
        modifier = Modifier.fillMaxWidth().padding(NanoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NanoSpacing.md),
      ) {
        AuthStatusHeadline(state)
        SupportingStatusLine(state)
        HuggingFaceAuthActions(onLoginClick = onLoginClick, onApiKeyClick = onApiKeyClick)
        DisconnectButton(
          state = state,
          onDisconnectClick = onDisconnectClick,
          modifier = Modifier.align(Alignment.End),
        )
      }
    },
  )
}

@Composable
private fun HuggingFaceInfoSheet() {
  Text(
    text =
      "Connect your Hugging Face account to access models and datasets from the Hugging Face Hub.",
    style = MaterialTheme.typography.bodyLarge,
  )
}

@Composable
private fun AuthStatusHeadline(state: HuggingFaceAuthState) {
  val statusText =
    when {
      state.isVerifying -> "Verifying credential…"
      state.isAuthenticated && state.accountLabel != null -> "Connected as ${state.accountLabel}"
      state.isAuthenticated -> "Connected"
      else -> "Not connected"
    }

  val statusColor =
    when {
      state.isAuthenticated -> MaterialTheme.colorScheme.primary
      state.isVerifying -> MaterialTheme.colorScheme.tertiary
      else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

  Text(
    text = statusText,
    style = MaterialTheme.typography.bodyMedium,
    color = statusColor,
    fontWeight = FontWeight.Medium,
  )
}

@Composable
private fun SupportingStatusLine(state: HuggingFaceAuthState) {
  val supportingText = supportingStatusLine(state) ?: return
  val color =
    if (state.lastError != null) {
      MaterialTheme.colorScheme.error
    } else {
      MaterialTheme.colorScheme.onSurfaceVariant
    }

  Text(text = supportingText, style = MaterialTheme.typography.bodySmall, color = color)
}

@Composable
private fun HuggingFaceAuthActions(onLoginClick: () -> Unit, onApiKeyClick: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(NanoSpacing.sm),
  ) {
    OutlinedButton(
      onClick = onLoginClick,
      modifier =
        Modifier.weight(1f).semantics { contentDescription = "Login with Hugging Face account" },
    ) {
      Icon(
        Icons.AutoMirrored.Filled.Login,
        contentDescription = null,
        modifier = Modifier.padding(end = 8.dp),
      )
      Text("Login")
    }

    FilledTonalButton(
      onClick = onApiKeyClick,
      modifier = Modifier.weight(1f).semantics { contentDescription = "Enter API key manually" },
    ) {
      Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
      Text("API Key")
    }
  }
}

@Composable
private fun DisconnectButton(
  state: HuggingFaceAuthState,
  onDisconnectClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!state.isAuthenticated) return

  TextButton(
    onClick = onDisconnectClick,
    modifier = modifier.semantics { contentDescription = "Disconnect Hugging Face account" },
  ) {
    Text("Disconnect")
  }
}

private fun supportingStatusLine(state: HuggingFaceAuthState): String? =
  when {
    state.lastError != null -> state.lastError
    state.isAuthenticated ->
      when (state.tokenSource) {
        HuggingFaceTokenSource.API_TOKEN -> "Authenticated via API token"
        HuggingFaceTokenSource.OAUTH -> "Authenticated via OAuth"
        else -> null
      }
    else -> null
  }
