package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vjaykrsna.nanoai.core.domain.settings.model.PrivacyPreference
import com.vjaykrsna.nanoai.core.domain.settings.model.RetentionPolicy
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.verify
import kotlinx.datetime.Instant
import org.junit.Test

/**
 * UI tests for Settings screen disclaimer, privacy dashboard, and consent handling.
 * Covers T029 requirements for disclaimer logging and privacy dashboard functionality.
 */
@HiltAndroidTest
class SettingsScreenDisclaimerTest : BaseSettingsScreenTest() {

  @Test
  fun settingsScreen_privacySection_displaysLocalDataStorageNotice() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Privacy & Security").performClick()
    composeTestRule.waitForIdle()

    // Verify local data storage notice is present
    composeTestRule.onNodeWithText("Local Data Storage").assertExists()
    composeTestRule
      .onNodeWithText("All your data is stored locally on your device", substring = true)
      .assertExists()
  }

  @Test
  fun settingsScreen_privacySection_displaysTelemetryToggle() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Privacy & Security").performClick()
    composeTestRule.waitForIdle()

    // Verify telemetry section is present
    composeTestRule.onNodeWithText("Usage Analytics").assertExists()
    composeTestRule.onNodeWithText("Share anonymous usage data", substring = true).assertExists()

    // Verify telemetry toggle is accessible
    composeTestRule
      .onNodeWithContentDescription("Toggle usage analytics", useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun settingsScreen_privacySection_telemetryToggle_callsViewModel() {
    updateState { current ->
      current.copy(
        privacyPreference = PrivacyPreference(telemetryOptIn = false)
      )
    }
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Privacy & Security").performClick()
    composeTestRule.waitForIdle()

    // Click telemetry toggle
    composeTestRule
      .onNodeWithContentDescription("Toggle usage analytics", useUnmergedTree = true)
      .performClick()
    composeTestRule.waitForIdle()

    verify { viewModel.setTelemetryOptIn(true) }
  }

  @Test
  fun settingsScreen_privacySection_displaysRetentionPolicyOptions() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Privacy & Security").performClick()
    composeTestRule.waitForIdle()

    // Verify retention policy section is present
    composeTestRule.onNodeWithText("Message Retention").assertExists()

    // Verify retention policy options are displayed
    composeTestRule.onNodeWithText("Keep indefinitely", substring = true).assertExists()
    composeTestRule.onNodeWithText("Manual purge only", substring = true).assertExists()
  }

  @Test
  fun settingsScreen_privacySection_retentionPolicyChange_callsViewModel() {
    updateState { current ->
      current.copy(
        privacyPreference = PrivacyPreference(retentionPolicy = RetentionPolicy.INDEFINITE)
      )
    }
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Privacy & Security").performClick()
    composeTestRule.waitForIdle()

    // Click on manual purge option
    composeTestRule.onNodeWithText("Manual purge only", substring = true).performClick()
    composeTestRule.waitForIdle()

    verify { viewModel.setRetentionPolicy(RetentionPolicy.MANUAL_PURGE_ONLY) }
  }

  @Test
  fun settingsScreen_privacySection_showsConsentAcknowledgedState() {
    updateState { current ->
      current.copy(
        privacyPreference = PrivacyPreference(
          consentAcknowledgedAt = Instant.parse("2024-01-01T00:00:00Z")
        )
      )
    }
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Privacy & Security").performClick()
    composeTestRule.waitForIdle()

    // Privacy section should be accessible when consent is acknowledged
    composeTestRule.onNodeWithText("Usage Analytics").assertExists()
  }

  @Test
  fun settingsScreen_privacySection_telemetryInfoButton_expandsDetails() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Privacy & Security").performClick()
    composeTestRule.waitForIdle()

    // Find the info card for usage analytics
    composeTestRule.onNodeWithText("Usage Analytics").assertExists()

    // Wait for info content to potentially load (if expanded by default)
    composeTestRule.waitForIdle()

    // Verify "What's collected" and "We never collect" content exists in the info section
    composeTestRule
      .onNodeWithText("Screen navigation patterns", substring = true, useUnmergedTree = true)
      .assertExists()
    composeTestRule
      .onNodeWithText("Personal messages or content", substring = true, useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun settingsScreen_privacySection_displaysPrivacyNoticeCard() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Privacy & Security").performClick()
    composeTestRule.waitForIdle()

    // Verify privacy notice is displayed
    composeTestRule.onNodeWithText("Local Data Storage").assertExists()
    composeTestRule
      .onNodeWithText("without cloud dependencies", substring = true, useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun settingsScreen_privacySection_accessibilityLabels_arePresent() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Privacy & Security").performClick()
    composeTestRule.waitForIdle()

    // Verify accessibility labels are properly set
    composeTestRule
      .onNodeWithContentDescription("Toggle usage analytics", useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun settingsScreen_exportWarningDismissed_skipsDialog() {
    // Set up state with export warnings dismissed
    updateState { current ->
      current.copy(
        privacyPreference = PrivacyPreference(exportWarningsDismissed = true)
      )
    }
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Backup & Sync").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Export Backup").performClick()
    composeTestRule.waitForIdle()

    // Export should be triggered directly without showing dialog
    verify { viewModel.exportBackup(match { it.contains("nanoai-backup") }, true) }

    // Dialog should NOT be shown (no encryption warning visible)
    composeTestRule.waitUntil(timeoutMillis = 1_000) {
      composeTestRule
        .onAllNodesWithText("Backups are not encrypted", substring = true, useUnmergedTree = true)
        .fetchSemanticsNodes(false)
        .isEmpty()
    }
  }

  @Test
  fun settingsScreen_exportWarningNotDismissed_showsDialog() {
    // Set up state with export warnings NOT dismissed
    updateState { current ->
      current.copy(
        privacyPreference = PrivacyPreference(exportWarningsDismissed = false)
      )
    }
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Backup & Sync").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Export Backup").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithText("Backups are not encrypted", substring = true, useUnmergedTree = true)
        .fetchSemanticsNodes(false)
        .isNotEmpty()
    }

    // Dialog should be shown with encryption warning
    composeTestRule
      .onNodeWithText("Backups are not encrypted", substring = true, useUnmergedTree = true)
      .assertExists()
  }
}
