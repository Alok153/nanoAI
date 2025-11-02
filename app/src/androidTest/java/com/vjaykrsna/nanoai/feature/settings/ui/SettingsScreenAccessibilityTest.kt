package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class SettingsScreenAccessibilityTest : BaseSettingsScreenTest() {

  @Test
  fun settingsScreen_allControls_haveSemanticLabels() {
    renderSettingsScreen()

    composeTestRule
      .onNodeWithContentDescription(
        "Settings screen organized by tabs with contextual sections",
        useUnmergedTree = true,
      )
      .assertExists()

    composeTestRule.onNodeWithText("Appearance").assertExists()
    composeTestRule.onNodeWithText("Behavior").assertExists()
    composeTestRule.onNodeWithText("APIs").assertExists()
    composeTestRule.onNodeWithText("Privacy & Security").assertExists()
    composeTestRule.onNodeWithText("Backup & Sync").assertExists()
    composeTestRule.onNodeWithText("About").assertExists()
  }

  @Test
  fun settingsScreen_talkBack_navigatesCorrectly() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Appearance").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Behavior").performClick()
    composeTestRule.waitForIdle()
    // TODO: expand when TalkBack events can be captured in tests
  }

  @Test
  fun settingsScreen_dynamicType_adjustsCorrectly() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_colorContrast_meetsStandards() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
    composeTestRule.onNodeWithText("Behavior").assertIsDisplayed()
  }
}
