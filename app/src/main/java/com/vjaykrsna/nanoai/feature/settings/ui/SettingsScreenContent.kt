package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.animation.Crossfade
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class SettingsCategory(val title: String, val subtitle: String) {
  GENERAL(title = "General", subtitle = "Language, startup behavior, input preferences"),
  APPEARANCE(title = "Appearance", subtitle = "Theme, font scale, layout density"),
  PRIVACY_SECURITY(title = "Privacy & Security", subtitle = "Local data, telemetry, app lock"),
  OFFLINE_AND_MODELS(title = "Offline & Models", subtitle = "Model management, storage control"),
  MODES(title = "Modes", subtitle = "Default preferences for each mode"),
  NOTIFICATIONS(title = "Notifications", subtitle = "Completion alerts and reminders"),
  ACCESSIBILITY(title = "Accessibility", subtitle = "Text size, contrast, voice navigation"),
  BACKUP_RESTORE(title = "Backup & Restore", subtitle = "Export and import your data"),
  ADVANCED(title = "Advanced", subtitle = "Diagnostics, cache reset, experiments"),
  ABOUT_HELP(title = "About & Help", subtitle = "Version info, feedback, policies"),
}

private val SettingsCategorySaver: Saver<SettingsCategory, String> =
  Saver(save = { it.name }, restore = { SettingsCategory.valueOf(it) })

@Composable
internal fun SettingsScreenContent(
  state: SettingsContentState,
  snackbarHostState: SnackbarHostState,
  actions: SettingsScreenActions,
  modifier: Modifier = Modifier,
) {
  var selectedCategory by
    rememberSaveable(stateSaver = SettingsCategorySaver) { mutableStateOf(SettingsCategory.GENERAL) }

  Scaffold(
    modifier =
      modifier.fillMaxSize().semantics {
        contentDescription =
          "Settings screen organized by tabs with contextual sections"
      },
    snackbarHost = {
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
      )
    },
    floatingActionButton = {
      if (selectedCategory == SettingsCategory.OFFLINE_AND_MODELS) {
        FloatingActionButton(
          onClick = actions.onAddProviderClick,
          modifier =
            Modifier.semantics {
              contentDescription = "Add API provider"
            },
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add")
        }
      }
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      SettingsHeader()
      Text(
        text = selectedCategory.subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      SettingsCategoryTabs(
        categories = SettingsCategory.entries.toList(),
        selectedCategory = selectedCategory,
        onCategorySelected = { selectedCategory = it },
      )

      Box(modifier = Modifier.weight(1f, fill = true)) {
        Crossfade(
          targetState = selectedCategory,
          label = "settings_category_crossfade",
        ) { category ->
          SettingsCategoryContent(
            category = category,
            state = state,
            actions = actions,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
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
private fun SettingsCategoryTabs(
  categories: List<SettingsCategory>,
  selectedCategory: SettingsCategory,
  onCategorySelected: (SettingsCategory) -> Unit,
  modifier: Modifier = Modifier,
) {
  val selectedIndex = categories.indexOf(selectedCategory).coerceAtLeast(0)
  ScrollableTabRow(
    selectedTabIndex = selectedIndex,
    edgePadding = 0.dp,
    modifier = modifier.fillMaxWidth(),
  ) {
    categories.forEach { category ->
      val isSelected = category == selectedCategory
      Tab(
        selected = isSelected,
        onClick = { onCategorySelected(category) },
        text = {
          Text(
            text = category.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
          )
        },
      )
    }
  }
}

@Composable
private fun SettingsCategoryContent(
  category: SettingsCategory,
  state: SettingsContentState,
  actions: SettingsScreenActions,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(bottom = 112.dp),
  ) {
    when (category) {
      SettingsCategory.GENERAL -> {
        item {
          SettingsPlaceholderSection(
            title = "Language & Region",
            description = "Choose the interface language, locale, and measurement units.",
            supportingText = "Design scaffolding is ready; controls will connect to DataStore preferences soon.",
          )
        }
        item {
          SettingsPlaceholderSection(
            title = "Startup & Home",
            description = "Define the screen nanoAI opens to and whether to restore previous sessions.",
            supportingText = "Upcoming implementation will hook into Shell launch policies.",
          )
        }
        item {
          SettingsPlaceholderSection(
            title = "Input Preferences",
            description = "Configure keyboard shortcuts, compose send behavior, and voice activation cues.",
            supportingText = "Tracked for Phase 2 once mode-specific composers land.",
          )
        }
      }

      SettingsCategory.APPEARANCE -> {
        item {
          SettingsPlaceholderSection(
            title = "Theme",
            description = "Switch between light, dark, and adaptive themes with scheduling support.",
            supportingText = "Theme toggles will integrate with AppViewModel theming.",
          )
        }
        item {
          SettingsPlaceholderSection(
            title = "Typography",
            description = "Adjust font scale, chat bubble density, and compact mode presets.",
            supportingText = "Type scale tokens are defined in specs/003-UI-UX/plan.md.",
          )
        }
        item {
          SettingsPlaceholderSection(
            title = "Layout Density",
            description = "Choose compact, comfortable, or spacious layouts for primary surfaces.",
            supportingText = "Implementation will reuse density tokens shared across feature modules.",
          )
        }
      }

      SettingsCategory.PRIVACY_SECURITY -> {
        item {
          PrivacySection(
            privacyPreferences = state.privacyPreferences,
            onTelemetryToggle = actions.onTelemetryToggle,
            onRetentionPolicyChange = actions.onRetentionPolicyChange,
          )
        }
        item {
          SettingsPlaceholderSection(
            title = "App Lock",
            description = "Secure nanoAI with biometrics or a passcode and configure auto-lock timers.",
            supportingText = "Security shell will integrate with the existing `AppLockManager` stub.",
          )
        }
        item {
          SettingsPlaceholderSection(
            title = "Local Data",
            description = "Manage cached prompts, transcripts, and scratch data.",
            supportingText = "Inline clear actions will reuse DataStore and Room repositories.",
          )
        }
      }

      SettingsCategory.OFFLINE_AND_MODELS -> {
        if (state.uiUxState.showMigrationSuccessNotification) {
          item { MigrationSuccessCard(onDismiss = actions.onDismissMigrationSuccess) }
        }

        item {
          ApiProvidersSectionHeader(hasProviders = state.apiProviders.isNotEmpty())
        }

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
          SettingsPlaceholderSection(
            title = "On-device Models",
            description = "Download, update, and reclaim storage for offline-capable models.",
            supportingText = "Hooks will connect to ModelRepository once the LiteRT pipeline lands.",
          )
        }
      }

      SettingsCategory.MODES -> {
        item {
          SettingsPlaceholderSection(
            title = "Chat Defaults",
            description = "Set default model, persona, and response style for new conversations.",
            supportingText = "Right-rail selectors will persist preferences here in Phase 2.",
          )
        }
        item {
          SettingsPlaceholderSection(
            title = "Creation Modes",
            description = "Configure prompts, presets, and output options for Image, Audio, Code, and Translate.",
            supportingText = "Specs outline per-mode defaults in specs/003-UI-UX/data-model.md.",
          )
        }
      }

      SettingsCategory.NOTIFICATIONS -> {
        item {
          SettingsPlaceholderSection(
            title = "Progress Alerts",
            description = "Control toasts and progress-center updates for long running jobs.",
            supportingText = "Implementation will connect to the unified progress queue.",
          )
        }
        item {
          SettingsPlaceholderSection(
            title = "Downloads",
            description = "Decide how nanoAI informs you when assets finish exporting.",
            supportingText = "Notification routing is planned via WorkManager workers.",
          )
        }
      }

      SettingsCategory.ACCESSIBILITY -> {
        item {
          SettingsPlaceholderSection(
            title = "Reading Preferences",
            description = "Adjust text size, line height, and accessible color contrasts.",
            supportingText = "Composable typography will reuse the adaptive text scaling work.",
          )
        }
        item {
          SettingsPlaceholderSection(
            title = "Voice Navigation",
            description = "Enable voice prompts and screen reader optimizations.",
            supportingText = "Voice control hooks align with the Live Voice mode backlog.",
          )
        }
      }

      SettingsCategory.BACKUP_RESTORE -> {
        item {
          DataManagementSection(
            onImportBackupClick = actions.onImportBackupClick,
            onExportBackupClick = actions.onExportBackupClick,
          )
        }
        item {
          SettingsPlaceholderSection(
            title = "Automations",
            description = "Schedule recurring backups and sync targets.",
            supportingText = "Will integrate with WorkManager once backup destinations are ready.",
          )
        }
      }

      SettingsCategory.ADVANCED -> {
        item {
          SettingsPlaceholderSection(
            title = "Diagnostics",
            description = "Capture logs, attach traces, and share with support.",
            supportingText = "Diagnostics flow will lean on the telemetry pipeline in specs/002.",
          )
        }
        item {
          SettingsPlaceholderSection(
            title = "Cache Reset",
            description = "Clear intermediate inference caches and downloaded assets.",
            supportingText = "Storage orchestration hooks will bind to the OfflineStore module.",
          )
        }
        item {
          SettingsPlaceholderSection(
            title = "Experimental Features",
            description = "Opt in to beta capabilities and Labs integrations.",
            supportingText = "Feature flag descriptors live in specs/004-fixes-and-inconsistencies/plan.md.",
          )
        }
      }

      SettingsCategory.ABOUT_HELP -> {
        item {
          SettingsPlaceholderSection(
            title = "About nanoAI",
            description = "View version details, licenses, and acknowledgements.",
            supportingText = "Version metadata will read from BuildConfig once wired.",
          )
        }
        item {
          SettingsPlaceholderSection(
            title = "Support & Feedback",
            description = "Send feedback, report issues, and browse documentation.",
            supportingText = "Links will target docs/ and community channels when published.",
          )
        }
      }
    }
  }
}

@Composable
private fun SettingsPlaceholderSection(
  title: String,
  description: String,
  supportingText: String,
  modifier: Modifier = Modifier,
) {
  SettingsSection(title = title, modifier = modifier) {
    SettingsPlaceholderCard(
      description = description,
      supportingText = supportingText,
    )
  }
}

@Composable
private fun SettingsPlaceholderCard(
  description: String,
  supportingText: String,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Foundation in progress",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        text = supportingText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
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
