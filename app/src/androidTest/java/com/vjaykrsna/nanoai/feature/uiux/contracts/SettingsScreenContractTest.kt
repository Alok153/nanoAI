package com.vjaykrsna.nanoai.feature.uiux.contracts

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreference
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsUiUxState
import com.vjaykrsna.nanoai.feature.settings.domain.huggingface.HuggingFaceAuthState
import com.vjaykrsna.nanoai.feature.settings.ui.SettingsContentState
import com.vjaykrsna.nanoai.feature.settings.ui.SettingsScreenActions
import com.vjaykrsna.nanoai.feature.settings.ui.SettingsScreenContent
import com.vjaykrsna.nanoai.ui.theme.NanoAITheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Contract test for Settings screen (FR-008).
 *
 * Validates the new tabbed architecture exposes core categories, contextual actions, and carries
 * the legacy provider/backup controls in their dedicated sections.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SettingsScreenContractTest {
  @get:Rule val composeRule = createComposeRule()

  private val sampleProvider =
    APIProviderConfig(
      providerId = "provider-1",
      providerName = "Test Provider",
      baseUrl = "https://api.example.com",
      apiKey = "key",
      apiType = APIType.OPENAI_COMPATIBLE,
      isEnabled = true,
    )

  private val defaultState =
    SettingsContentState(
      apiProviders = listOf(sampleProvider),
      privacyPreferences = PrivacyPreference(),
      uiUxState = SettingsUiUxState(showMigrationSuccessNotification = true),
      huggingFaceState = HuggingFaceAuthState.unauthenticated(),
    )

  private val noopActions =
    SettingsScreenActions(
      onAddProviderClick = {},
      onProviderEdit = {},
      onProviderDelete = {},
      onImportBackupClick = {},
      onExportBackupClick = {},
      onTelemetryToggle = {},
      onRetentionPolicyChange = {},
      onDismissMigrationSuccess = {},
      onThemePreferenceChange = {},
      onVisualDensityChange = {},
      onHuggingFaceLoginClick = {},
      onHuggingFaceApiKeyClick = {},
      onHuggingFaceDisconnectClick = {},
    )

  @Test
  fun settingsScreen_displaysTabbedCategories_andGeneralCopy() {
    composeRule.setContent {
      NanoAITheme {
        SettingsScreenContent(
          state = defaultState,
          snackbarHostState = SnackbarHostState(),
          actions = noopActions,
        )
      }
    }

    composeRule.onNodeWithText("Settings").assertIsDisplayed()
    composeRule.onNodeWithText("General").assertIsDisplayed()
    composeRule.onNodeWithText("General").assertHasClickAction()
    composeRule
      .onNodeWithText("Choose the interface language, locale, and measurement units.")
      .assertIsDisplayed()
    val appearanceTab = composeRule.onNodeWithText("Appearance")
    appearanceTab.assertIsDisplayed()
    appearanceTab.assertHasClickAction()
    appearanceTab.performClick()
    composeRule
      .onNodeWithText("Switch between light, dark, or follow the system theme.")
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_offlineAndModelsSection_showsProvidersAndFab() {
    composeRule.setContent {
      NanoAITheme {
        SettingsScreenContent(
          state = defaultState,
          snackbarHostState = SnackbarHostState(),
          actions = noopActions,
        )
      }
    }

    val offlineTab = composeRule.onNodeWithText("Offline & Models")
    offlineTab.assertIsDisplayed()
    offlineTab.assertHasClickAction()
    offlineTab.performClick()

    composeRule.onNodeWithText("Test Provider").assertIsDisplayed()
    composeRule
      .onNodeWithText("Connect remote APIs or local runtimes to power nanoAI's modes.")
      .assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Add API provider").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_backupRestoreSection_exposesImportAndExport() {
    composeRule.setContent {
      NanoAITheme {
        SettingsScreenContent(
          state = defaultState,
          snackbarHostState = SnackbarHostState(),
          actions = noopActions,
        )
      }
    }

    val backupTab = composeRule.onNodeWithText("Backup & Restore")
    backupTab.assertIsDisplayed()
    backupTab.assertHasClickAction()
    backupTab.performClick()

    composeRule.onNodeWithText("Import Backup").assertIsDisplayed().assertHasClickAction()
    composeRule.onNodeWithText("Export Backup").assertIsDisplayed().assertHasClickAction()
  }
}
