package com.vjaykrsna.nanoai.feature.uiux.scenario

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vjaykrsna.nanoai.MainActivity
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation scenario covering Quickstart Scenario 3 (Sidebar and Settings). Assertions fail
 * until the sidebar + settings flows provide the required semantics.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SidebarSettingsScenarioTest {
  @get:Rule(order = 0) val environmentRule = TestEnvironmentRule()
  @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun sidebarNavigation_reachesSettings_withUndoAffordance() {
    composeRule
      .onNodeWithTag("sidebar_toggle")
      .assertIsDisplayed()
      .assertHasClickAction()
      .performClick()

    composeRule.onNodeWithTag("sidebar_drawer").assertIsDisplayed()

    composeRule
      .onNodeWithTag("sidebar_item_settings")
      .assertIsDisplayed()
      .assertHasClickAction()
      .performClick()

    composeRule.onNodeWithTag("settings_grouped_options").assertIsDisplayed()

    composeRule.onNodeWithTag("settings_inline_help").assertIsDisplayed()

    composeRule.onNodeWithTag("settings_undo_action").assertIsDisplayed().assertIsEnabled()
  }
}
