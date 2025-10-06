package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsScreenContent(
  state: SettingsContentState,
  snackbarHostState: SnackbarHostState,
  actions: SettingsScreenActions,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier.fillMaxSize().semantics {
        contentDescription = "Settings screen with API providers and privacy options"
      },
  ) {
    LazyColumn(
      contentPadding =
        PaddingValues(
          start = 20.dp,
          top = 16.dp,
          end = 20.dp,
          bottom = 112.dp,
        ),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.fillMaxSize(),
    ) {
      item { SettingsHeader() }

      if (state.uiUxState.showMigrationSuccessNotification) {
        item { MigrationSuccessCard(onDismiss = actions.onDismissMigrationSuccess) }
      }

      item { ApiProvidersSectionHeader(hasProviders = state.apiProviders.isNotEmpty()) }

      items(
        items = state.apiProviders,
        key = { it.providerId },
        contentType = { "api_provider_card" },
      ) { provider ->
        ApiProviderCard(
          provider = provider,
          onEdit = { actions.onProviderEdit(provider) },
          onDelete = { actions.onProviderDelete(provider) },
        )
      }

      item {
        DataManagementSection(
          onImportBackupClick = actions.onImportBackupClick,
          onExportBackupClick = actions.onExportBackupClick,
        )
      }

      item {
        PrivacySection(
          privacyPreferences = state.privacyPreferences,
          onTelemetryToggle = actions.onTelemetryToggle,
          onRetentionPolicyChange = actions.onRetentionPolicyChange,
        )
      }
    }

    FloatingActionButton(
      onClick = actions.onAddProviderClick,
      modifier =
        Modifier.align(Alignment.BottomEnd).padding(24.dp).semantics {
          contentDescription = "Add API provider"
        },
    ) {
      Icon(Icons.Default.Add, contentDescription = "Add")
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier =
        Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 24.dp),
    )
  }
}

@Composable
private fun SettingsHeader(modifier: Modifier = Modifier) {
  Text(
    text = "Settings",
    style = MaterialTheme.typography.headlineMedium,
    fontWeight = FontWeight.Bold,
    modifier = modifier,
  )
}

@Composable
internal fun SettingsSection(
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
internal fun MigrationSuccessCard(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
      ),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Migration Successful",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text =
          "Your provider credentials have been migrated to a more secure storage. " +
            "For enhanced security, please rotate your provider credentials.",
        style = MaterialTheme.typography.bodyMedium,
      )
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.align(Alignment.End),
      ) {
        Text("Dismiss")
      }
    }
  }
}
