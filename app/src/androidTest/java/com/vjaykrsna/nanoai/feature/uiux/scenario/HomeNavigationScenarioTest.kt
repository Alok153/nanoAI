package com.vjaykrsna.nanoai.feature.uiux.scenario

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
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
 * Instrumentation scenario covering Quickstart Scenario 2 (Home Screen Navigation). The checks
 * intentionally fail until the Home screen exposes the expected test tags and behaviors.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class HomeNavigationScenarioTest {
  @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun homeScreen_expandTools_and_triggerRecentAction() {
    composeRule.onNodeWithTag("home_single_column_feed").assertIsDisplayed()

    composeRule
      .onNodeWithTag("home_tools_toggle")
      .assertIsDisplayed()
      .assertHasClickAction()
      .performClick()

    composeRule.onNodeWithTag("home_tools_panel_expanded").assertIsDisplayed()

    composeRule
      .onNodeWithTag("home_recent_action_0")
      .assertIsDisplayed()
      .assertHasClickAction()
      .performClick()

    composeRule.onNodeWithTag("home_latency_meter").assertIsDisplayed()
  }
}
