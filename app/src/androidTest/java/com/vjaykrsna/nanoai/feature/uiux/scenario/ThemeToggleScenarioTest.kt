package com.vjaykrsna.nanoai.feature.uiux.scenario

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vjaykrsna.nanoai.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Quickstart Scenario 4 instrumentation: Theme toggle persistence and layout stability.
 *
 * Expectations (currently unmet, so this test fails):
 * - Settings exposes a theme toggle with semantics tag `theme_toggle_switch`
 * - Toggling theme reflects updated status via `theme_toggle_persistence_status`
 * - Recreating the activity preserves the chosen theme
 * - No layout jump indicator (tag `theme_layout_stability_check`) appears after toggle
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ThemeToggleScenarioTest {
  @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun themeToggle_persistsAcrossProcessDeath_withoutLayoutJump() {
    // Navigate to Settings via sidebar entry
    composeRule
      .onNodeWithTag("sidebar_toggle")
      .assertIsDisplayed()
      .assertHasClickAction()
      .performClick()

    composeRule.onNodeWithTag("sidebar_drawer").assertIsDisplayed()

    composeRule.onNodeWithTag("sidebar_item_settings").assertIsDisplayed().performClick()

    // Interact with theme toggle
    composeRule
      .onNodeWithTag("theme_toggle_switch")
      .assertIsDisplayed()
      .assertHasClickAction()
      .performClick()

    composeRule
      .onNodeWithTag("theme_toggle_persistence_status")
      .assertIsDisplayed()
      .assertTextContains("Dark", substring = true)

    // Simulate process death via recreation
    composeRule.activityRule.scenario.recreate()
    composeRule.waitForIdle()

    composeRule
      .onNodeWithTag("theme_toggle_persistence_status")
      .assertIsDisplayed()
      .assertTextContains("Dark", substring = true)

    composeRule.onNodeWithTag("theme_layout_stability_check").assertIsDisplayed()
  }
}
