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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing
import kotlinx.coroutines.launch

// NOTE: Tab order designed to match user workflow - frequently changed settings first
private enum class SettingsCategory(val title: String) {
  APPEARANCE(title = "Appearance"),
  BEHAVIOR(title = "Behavior"),
  APIS(title = "APIs"),
  PRIVACY_SECURITY(title = "Privacy & Security"),
  BACKUP_SYNC(title = "Backup & Sync"),
  ABOUT(title = "About"),
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
      if (currentCategory == SettingsCategory.APIS) {
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
        item {
          SettingsPlaceholderSection(
            title = "Animation Preferences",
            description = "Control motion and transitions throughout the app.",
            supportingText = "Accessibility option to reduce motion for sensitive users.",
          )
        }
      }
      SettingsCategory.BEHAVIOR -> {
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
        item {
          SettingsPlaceholderSection(
            title = "Accessibility",
            description = "Configure screen reader, high contrast, and assistive technologies.",
            supportingText = "TalkBack optimizations and WCAG 2.1 AA compliance features.",
          )
        }
        item {
          SettingsPlaceholderSection(
            title = "Notifications",
            description = "Control alerts for downloads, job completion, and background tasks.",
            supportingText = "WorkManager notification channels for progress updates.",
          )
        }
      }
      SettingsCategory.APIS -> {
        // NOTE: Migration card shows once after credential storage upgrade
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

        // TODO: Implement multi-provider orchestration with failover and priority routing
        item {
          SettingsPlaceholderSection(
            title = "Load Balancing",
            description = "Configure API failover, rate limiting, and provider priorities.",
            supportingText = "Multi-provider orchestration will enable seamless switching.",
          )
        }
        // TODO: Add API health monitoring and quota tracking dashboard
        item {
          SettingsPlaceholderSection(
            title = "API Testing",
            description = "Test connectivity and monitor quota usage across providers.",
            supportingText = "Real-time health checks and usage analytics for all configured APIs.",
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
            description =
              "Secure nanoAI with biometrics or a passcode and configure auto-lock timers.",
            supportingText =
              "Security shell will integrate with the existing `AppLockManager` stub.",
          )
        }
        item {
          SettingsPlaceholderSection(
            title = "Local Data Management",
            description = "Manage cached prompts, transcripts, and scratch data.",
            supportingText = "Inline clear actions will reuse DataStore and Room repositories.",
          )
        }
        // TODO: Implement encryption key rotation and secure storage configuration
        item {
          SettingsPlaceholderSection(
            title = "Encryption Settings",
            description = "Configure key rotation and secure storage policies.",
            supportingText = "Advanced encryption controls for sensitive data protection.",
          )
        }
      }
      SettingsCategory.BACKUP_SYNC -> {
        item {
          DataManagementSection(
            onImportBackupClick = actions.onImportBackupClick,
            onExportBackupClick = actions.onExportBackupClick,
          )
        }
        // TODO: Implement WorkManager-based scheduled backup system
        item {
          SettingsPlaceholderSection(
            title = "Automated Backups",
            description = "Schedule recurring backups and configure backup destinations.",
            supportingText = "Will integrate with WorkManager once backup destinations are ready.",
          )
        }
        // TODO: Implement privacy-first cloud sync with E2E encryption
        item {
          SettingsPlaceholderSection(
            title = "Cloud Sync",
            description = "Sync settings and data across devices with end-to-end encryption.",
            supportingText = "Privacy-first sync will use encrypted cloud storage.",
          )
        }
        item {
          SettingsPlaceholderSection(
            title = "Data Migration",
            description = "Import from other AI apps and convert between formats.",
            supportingText = "Support for common AI assistant data formats and conversion tools.",
          )
        }
      }
      SettingsCategory.ABOUT -> {
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
        item {
          SettingsPlaceholderSection(
            title = "Documentation",
            description = "Access user guides, API documentation, and tutorials.",
            supportingText = "Comprehensive documentation links for all features and APIs.",
          )
        }
        // TODO: Gather device specs, runtime info, and diagnostics for support tickets
        item {
          SettingsPlaceholderSection(
            title = "System Information",
            description = "View device specifications and runtime diagnostics.",
            supportingText = "Debug information for troubleshooting and support tickets.",
          )
        }
        // TODO: Implement advanced diagnostic tools and log export
        item {
          SettingsPlaceholderSection(
            title = "Advanced Diagnostics",
            description = "Capture detailed logs, attach traces, and share with support.",
            supportingText = "Diagnostics flow will lean on the telemetry pipeline in specs/002.",
          )
        }
        // TODO: Add cache management and storage cleanup tools
        item {
          SettingsPlaceholderSection(
            title = "Cache Management",
            description = "Clear inference caches, downloaded assets, and temporary data.",
            supportingText = "Storage orchestration hooks will bind to the OfflineStore module.",
          )
        }
        // TODO: Feature flags for beta testing and experimental capabilities
        item {
          SettingsPlaceholderSection(
            title = "Experimental Features",
            description = "Opt in to beta capabilities and Labs integrations.",
            supportingText =
              "Feature flag descriptors live in specs/004-fixes-and-inconsistencies/plan.md.",
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
