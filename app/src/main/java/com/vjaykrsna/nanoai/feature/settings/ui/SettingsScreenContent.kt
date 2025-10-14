package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing
import kotlinx.coroutines.launch

private enum class SettingsCategory(val title: String) {
  GENERAL(title = "General"),
  APPEARANCE(title = "Appearance"),
  PRIVACY_SECURITY(title = "Privacy & Security"),
  OFFLINE_AND_MODELS(title = "Offline & Models"),
  MODES(title = "Modes"),
  NOTIFICATIONS(title = "Notifications"),
  BACKUP_RESTORE(title = "Backup & Restore"),
  ADVANCED(title = "Advanced"),
  ABOUT_HELP(title = "About & Help"),
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettingsScreenContent(
  state: SettingsContentState,
  snackbarHostState: SnackbarHostState,
  actions: SettingsScreenActions,
  modifier: Modifier = Modifier,
) {
  val categories = SettingsCategory.entries.toList()
  val pagerState = rememberPagerState(initialPage = 0) { categories.size }
  val coroutineScope = rememberCoroutineScope()
  val currentCategory = categories[pagerState.currentPage]

  Scaffold(
    modifier =
      modifier.fillMaxSize().semantics {
        contentDescription = "Settings screen organized by tabs with contextual sections"
      },
    snackbarHost = {
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
      )
    },
    floatingActionButton = {
      if (currentCategory == SettingsCategory.OFFLINE_AND_MODELS) {
        FloatingActionButton(
          onClick = actions.onAddProviderClick,
          modifier = Modifier.semantics { contentDescription = "Add API provider" },
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add")
        }
      }
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = NanoSpacing.lg),
    ) {
      SettingsCategoryTabs(
        categories = categories,
        selectedCategory = currentCategory,
        onCategorySelect = { category ->
          val targetIndex = categories.indexOf(category).coerceAtLeast(0)
          coroutineScope.launch { pagerState.animateScrollToPage(targetIndex) }
        },
      )

      Box(modifier = Modifier.weight(1f, fill = true)) {
        HorizontalPager(
          state = pagerState,
          modifier = Modifier.fillMaxSize(),
        ) { pageIndex ->
          val category = categories[pageIndex]
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
private fun SettingsCategoryTabs(
  categories: List<SettingsCategory>,
  selectedCategory: SettingsCategory,
  onCategorySelect: (SettingsCategory) -> Unit,
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
        onClick = { onCategorySelect(category) },
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
    verticalArrangement = Arrangement.spacedBy(NanoSpacing.lg),
    contentPadding = PaddingValues(top = NanoSpacing.lg, bottom = 112.dp),
  ) {
    when (category) {
      SettingsCategory.GENERAL -> {
        item {
          SettingsPlaceholderSection(
            title = "Language & Region",
            description = "Choose the interface language, locale, and measurement units.",
            supportingText =
              "Design scaffolding is ready; controls will connect to DataStore preferences soon.",
          )
        }
        item {
          SettingsPlaceholderSection(
            title = "Startup & Home",
            description =
              "Define the screen nanoAI opens to and whether to restore previous sessions.",
            supportingText = "Upcoming implementation will hook into Shell launch policies.",
          )
        }
        item {
          SettingsPlaceholderSection(
            title = "Input Preferences",
            description =
              "Configure keyboard shortcuts, compose send behavior, and voice activation cues.",
            supportingText = "Tracked for Phase 2 once mode-specific composers land.",
          )
        }
      }
      SettingsCategory.APPEARANCE -> {
        item {
          AppearanceThemeSection(
            uiUxState = state.uiUxState,
            onThemeChange = actions.onThemePreferenceChange,
          )
        }
        item {
          AppearanceDensitySection(
            uiUxState = state.uiUxState,
            onDensityChange = actions.onVisualDensityChange,
          )
        }
        item { AppearanceTypographyPlaceholder() }
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
            description =
              "Secure nanoAI with biometrics or a passcode and configure auto-lock timers.",
            supportingText =
              "Security shell will integrate with the existing `AppLockManager` stub.",
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

        item { HuggingFaceAuthSectionHeader() }

        item {
          HuggingFaceAuthCard(
            state = state.huggingFaceState,
            onLoginClick = actions.onHuggingFaceLoginClick,
            onApiKeyClick = actions.onHuggingFaceApiKeyClick,
            onDisconnectClick = actions.onHuggingFaceDisconnectClick,
          )
        }

        item {
          OnDeviceModelsSection(
            statusMessage = state.uiUxState.statusMessage,
            onStatusMessageShow = actions.onStatusMessageShow,
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
            description =
              "Configure prompts, presets, and output options " +
                "for Image, Audio, Code, and Translate.",
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
            supportingText =
              "Feature flag descriptors live in specs/004-fixes-and-inconsistencies/plan.md.",
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
private fun OnDeviceModelsSection(
  statusMessage: String?,
  onStatusMessageShow: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val latestOnStatusMessageShow = rememberUpdatedState(onStatusMessageShow)
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.padding(NanoSpacing.lg),
      verticalArrangement = Arrangement.spacedBy(NanoSpacing.md),
    ) {
      Text(
        text = "On-device Models",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text =
          "Models added by nanoAI sync automatically appear in the library. Pull down on the " +
            "library list to refresh from the catalog.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (statusMessage != null) {
        LaunchedEffect(statusMessage) { latestOnStatusMessageShow.value() }
        Text(
          text = statusMessage,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(vertical = NanoSpacing.sm),
        )
      }

      Text(
        text =
          "If the catalog changes, the library keeps your install status " +
            "and local downloads intact.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
internal fun MigrationSuccessCard(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
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
