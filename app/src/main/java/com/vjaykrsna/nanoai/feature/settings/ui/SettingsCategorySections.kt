package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.settings.presentation.state.SettingsUiState

@Composable
internal fun SettingsCategoryLoading(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.semantics { contentDescription = "Loading settings" },
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      CircularProgressIndicator()
      Text(
        "Loading settings...",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

internal fun LazyListScope.settingsCategoryItems(
  category: SettingsCategory,
  state: SettingsUiState,
  actions: SettingsScreenActions,
) {
  when (category) {
    SettingsCategory.APPEARANCE -> appearanceSettingsItems(state, actions)
    SettingsCategory.BEHAVIOR -> behaviorSettingsItems()
    SettingsCategory.APIS -> apiSettingsItems(state, actions)
    SettingsCategory.PRIVACY_SECURITY -> privacySettingsItems(state, actions)
    SettingsCategory.BACKUP_SYNC -> backupSettingsItems(actions)
    SettingsCategory.ABOUT -> aboutSettingsItems(actions)
  }
}

private fun LazyListScope.appearanceSettingsItems(
  state: SettingsUiState,
  actions: SettingsScreenActions,
) {
  item {
    AppearanceThemeCard(
      state = state,
      onThemeChange = actions.onThemePreferenceChange,
      onHighContrastChange = actions.onHighContrastChange,
      modifier = Modifier.testTag("appearance_theme_card"),
    )
  }
  item { AppearanceDensityCard(state = state, onDensityChange = actions.onVisualDensityChange) }
  item { AppearanceTypographyCard() }
  item { AppearanceAnimationPreferencesCard() }
}

private fun LazyListScope.behaviorSettingsItems() {
  item { BehaviorStartupCard() }
  item { BehaviorInputPreferencesCard() }
  item { BehaviorAccessibilityCard() }
  item { BehaviorNotificationsCard() }
}

private fun LazyListScope.apiSettingsItems(state: SettingsUiState, actions: SettingsScreenActions) {
  if (state.showMigrationSuccessNotification) {
    item { MigrationSuccessCard(onDismiss = actions.onDismissMigrationSuccess) }
  }

  item { ApiProvidersCard(hasProviders = state.apiProviders.isNotEmpty()) }

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
    HuggingFaceAuthCard(
      state = state.huggingFaceAuthState,
      onLoginClick = actions.onHuggingFaceLoginClick,
      onApiKeyClick = actions.onHuggingFaceApiKeyClick,
      onDisconnectClick = actions.onHuggingFaceDisconnectClick,
    )
  }

  item { APIsLoadBalancingCard() }
  item { APIsTestingCard() }
}

private fun LazyListScope.privacySettingsItems(
  state: SettingsUiState,
  actions: SettingsScreenActions,
) {
  item { PrivacyDashboardCard(summary = state.privacyDashboardSummary) }
  item {
    PrivacySection(
      privacyPreferences = state.privacyPreference,
      onTelemetryToggle = actions.onTelemetryToggle,
      onRetentionPolicyChange = actions.onRetentionPolicyChange,
    )
  }
  item { PrivacyAppLockCard() }
  item { PrivacyDataManagementCard() }
  item { PrivacyEncryptionCard() }
}

private fun LazyListScope.backupSettingsItems(actions: SettingsScreenActions) {
  item {
    DataManagementSection(
      onImportBackupClick = actions.onImportBackupClick,
      onExportBackupClick = actions.onExportBackupClick,
    )
  }
  item { BackupAutomatedCard() }
  item { BackupCloudSyncCard() }
  item { BackupDataMigrationCard() }
}

private fun LazyListScope.aboutSettingsItems(actions: SettingsScreenActions) {
  item { AboutNanoAICard() }
  item { AboutSupportFeedbackCard() }
  item { AboutDocumentationCard() }
  item { AboutSystemInformationCard() }
  item {
    AboutAdvancedDiagnosticsCard(
      onNavigateToCoverageDashboard = actions.onNavigateToCoverageDashboard
    )
  }
  item { AboutCacheManagementCard() }
  item { AboutExperimentalFeaturesCard() }
}
