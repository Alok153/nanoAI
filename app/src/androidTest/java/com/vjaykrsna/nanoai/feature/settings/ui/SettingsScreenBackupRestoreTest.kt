package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.domain.settings.ImportSummary
import com.vjaykrsna.nanoai.feature.settings.presentation.model.SettingsUiEvent
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.verify
import org.junit.Test

@HiltAndroidTest
class SettingsScreenBackupRestoreTest : BaseSettingsScreenTest() {

  @Test
  fun settingsScreen_exportData_showsDialog() {
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

    composeTestRule
      .onNodeWithText("Backups are not encrypted", substring = true, useUnmergedTree = true)
      .assertExists()

    composeTestRule.onNodeWithText("Don't warn me again", substring = true).performClick()
    composeTestRule.onNodeWithText("Export", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()

    verify { viewModel.dismissExportWarnings() }
    verify { viewModel.exportBackup(match { it.contains("nanoai-backup") }, true) }
  }

  @Test
  fun settingsScreen_exportSuccess_showsSnackbar() {
    renderSettingsScreen()

    composeTestRule.runOnIdle { emitEvent(SettingsUiEvent.ExportCompleted("Export successful")) }

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithText(
          "Backup exported to Export successful",
          substring = true,
          useUnmergedTree = true,
        )
        .fetchSemanticsNodes(false)
        .isNotEmpty()
    }
    composeTestRule
      .onNodeWithText(
        "Backup exported to Export successful",
        substring = true,
        useUnmergedTree = true,
      )
      .assertExists()
  }

  @Test
  fun settingsScreen_importData_triggersFilePicker() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Backup & Sync").performClick()
    composeTestRule.waitForIdle()
    // TODO: click import button when file picker hook is exposed
  }

  @Test
  fun settingsScreen_importSuccess_showsSnackbar() {
    renderSettingsScreen()

    composeTestRule.runOnIdle {
      emitEvent(
        SettingsUiEvent.ImportCompleted(
          ImportSummary(
            personasImported = 5,
            personasUpdated = 0,
            providersImported = 2,
            providersUpdated = 1,
          )
        )
      )
    }

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithText(
          "Imported backup: 5 personas, 3 providers",
          substring = false,
          useUnmergedTree = true,
        )
        .fetchSemanticsNodes(false)
        .isNotEmpty()
    }
    composeTestRule
      .onNodeWithText(
        "Imported backup: 5 personas, 3 providers",
        substring = false,
        useUnmergedTree = true,
      )
      .assertExists()
  }

  @Test
  fun settingsScreen_error_showsSnackbar() {
    renderSettingsScreen()

    composeTestRule.runOnIdle {
      emitEvent(SettingsUiEvent.ErrorRaised(NanoAIErrorEnvelope("Test error")))
    }

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithText("Test error", substring = false, useUnmergedTree = true)
        .fetchSemanticsNodes(false)
        .isNotEmpty()
    }
    composeTestRule
      .onNodeWithText("Test error", substring = false, useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun settingsScreen_exportDialog_showsDisclaimerWarning() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Backup & Sync").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Export Backup").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithText("Export Backup", useUnmergedTree = true)
        .fetchSemanticsNodes(false)
        .size > 1
    }

    // Verify dialog content items are present
    composeTestRule
      .onNodeWithText("Conversations and messages", substring = true, useUnmergedTree = true)
      .assertExists()
    composeTestRule
      .onNodeWithText("Persona profiles", substring = true, useUnmergedTree = true)
      .assertExists()
    composeTestRule
      .onNodeWithText("API provider configurations", substring = true, useUnmergedTree = true)
      .assertExists()
    composeTestRule
      .onNodeWithText("Settings and preferences", substring = true, useUnmergedTree = true)
      .assertExists()

    // Verify encryption warning is visible
    composeTestRule
      .onNodeWithText("Backups are not encrypted", substring = true, useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun settingsScreen_exportDialog_cancelDismissesDialog() {
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

    // Click cancel button
    composeTestRule
      .onNodeWithContentDescription("Cancel export backup", useUnmergedTree = true)
      .performClick()
    composeTestRule.waitForIdle()

    // Dialog should be dismissed - encryption warning should no longer be visible
    composeTestRule.waitUntil(timeoutMillis = 3_000) {
      composeTestRule
        .onAllNodesWithText("Backups are not encrypted", substring = true, useUnmergedTree = true)
        .fetchSemanticsNodes(false)
        .isEmpty()
    }
  }

  @Test
  fun settingsScreen_importSuccess_showsPersonaAndProviderCounts() {
    renderSettingsScreen()

    composeTestRule.runOnIdle {
      emitEvent(
        SettingsUiEvent.ImportCompleted(
          ImportSummary(
            personasImported = 10,
            personasUpdated = 2,
            providersImported = 5,
            providersUpdated = 3,
          )
        )
      )
    }

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithText(
          "Imported backup: 12 personas, 8 providers",
          substring = false,
          useUnmergedTree = true,
        )
        .fetchSemanticsNodes(false)
        .isNotEmpty()
    }
    composeTestRule
      .onNodeWithText(
        "Imported backup: 12 personas, 8 providers",
        substring = false,
        useUnmergedTree = true,
      )
      .assertExists()
  }

  @Test
  fun settingsScreen_exportDialog_dontWarnAgainCheckbox_updatesOnClick() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Backup & Sync").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Export Backup").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithText("Don't warn me again", substring = true, useUnmergedTree = true)
        .fetchSemanticsNodes(false)
        .isNotEmpty()
    }

    // Find and verify the checkbox
    composeTestRule
      .onNodeWithContentDescription("Don't warn me again checkbox", useUnmergedTree = true)
      .assertExists()
      .performClick()

    // Confirm export with checkbox selected
    composeTestRule
      .onNodeWithContentDescription("Confirm export backup", useUnmergedTree = true)
      .performClick()
    composeTestRule.waitForIdle()

    // Verify dismissExportWarnings was called because checkbox was checked
    verify { viewModel.dismissExportWarnings() }
  }

  @Test
  fun settingsScreen_exportWithoutDontWarnAgain_doesNotDismissWarnings() {
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

    // Click export without checking "Don't warn me again"
    composeTestRule
      .onNodeWithContentDescription("Confirm export backup", useUnmergedTree = true)
      .performClick()
    composeTestRule.waitForIdle()

    // Verify dismissExportWarnings was NOT called (0 invocations)
    verify(exactly = 0) { viewModel.dismissExportWarnings() }
    // But export should still be triggered
    verify { viewModel.exportBackup(match { it.contains("nanoai-backup") }, true) }
  }

  @Test
  fun settingsScreen_backupSection_displaysImportExportOptions() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Backup & Sync").performClick()
    composeTestRule.waitForIdle()

    // Verify backup restore card is displayed
    composeTestRule.onNodeWithText("Backup & Restore").assertExists()
    composeTestRule.onNodeWithText("Import Backup").assertExists()
    composeTestRule.onNodeWithText("Export Backup").assertExists()

    // Verify descriptions are present
    composeTestRule
      .onNodeWithText("Restore personas, providers, and settings", substring = true)
      .assertExists()
    composeTestRule
      .onNodeWithText("Export conversations, personas, and settings", substring = true)
      .assertExists()
  }

  @Test
  fun settingsScreen_importError_showsErrorSnackbar() {
    renderSettingsScreen()

    composeTestRule.runOnIdle {
      emitEvent(
        SettingsUiEvent.ErrorRaised(
          NanoAIErrorEnvelope("Failed to import backup: Invalid file format")
        )
      )
    }

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithText(
          "Failed to import backup: Invalid file format",
          substring = false,
          useUnmergedTree = true,
        )
        .fetchSemanticsNodes(false)
        .isNotEmpty()
    }
    composeTestRule
      .onNodeWithText(
        "Failed to import backup: Invalid file format",
        substring = false,
        useUnmergedTree = true,
      )
      .assertExists()
  }

  @Test
  fun settingsScreen_exportError_showsErrorSnackbar() {
    renderSettingsScreen()

    composeTestRule.runOnIdle {
      emitEvent(
        SettingsUiEvent.ErrorRaised(NanoAIErrorEnvelope("Failed to export backup: Disk full"))
      )
    }

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithText(
          "Failed to export backup: Disk full",
          substring = false,
          useUnmergedTree = true,
        )
        .fetchSemanticsNodes(false)
        .isNotEmpty()
    }
    composeTestRule
      .onNodeWithText(
        "Failed to export backup: Disk full",
        substring = false,
        useUnmergedTree = true,
      )
      .assertExists()
  }
}
