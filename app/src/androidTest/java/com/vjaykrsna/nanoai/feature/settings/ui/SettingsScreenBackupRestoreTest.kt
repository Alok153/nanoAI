package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.domain.settings.ImportSummary
import com.vjaykrsna.nanoai.feature.settings.presentation.model.SettingsUiEvent
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class SettingsScreenBackupRestoreTest : BaseSettingsScreenTest() {

  @Test
  fun settingsScreen_exportData_showsDialog() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Backup & Sync").performClick()
    composeTestRule.waitForIdle()
    // TODO: click export button when surfaced for testing
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
}
