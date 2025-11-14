package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.vjaykrsna.nanoai.feature.settings.presentation.state.SettingsUiState
import com.vjaykrsna.nanoai.shared.testing.TestingTheme
import org.junit.Rule
import org.junit.Test

class SettingsScreenContentTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun showsLoadingStateWhenUiIsLoading() {
    composeTestRule.setContent {
      TestingTheme {
        SettingsScreenContent(
          state = SettingsUiState(isLoading = true),
          snackbarHostState = SnackbarHostState(),
          actions = stubSettingsActions(),
        )
      }
    }

    composeTestRule
      .onNodeWithText("Loading settings...", substring = false, useUnmergedTree = true)
      .assertExists()
  }

  private fun stubSettingsActions(): SettingsScreenActions =
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
      onHighContrastChange = {},
      onHuggingFaceLoginClick = {},
      onHuggingFaceApiKeyClick = {},
      onHuggingFaceDisconnectClick = {},
      onStatusMessageShow = {},
      onNavigateToCoverageDashboard = {},
    )
}
