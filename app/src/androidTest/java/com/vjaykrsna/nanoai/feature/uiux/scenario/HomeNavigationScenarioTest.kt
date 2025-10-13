package com.vjaykrsna.nanoai.feature.uiux.scenario

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
 * Instrumentation scenario covering Quickstart Scenario 2 (Home Screen Navigation). The checks
 * intentionally fail until the Home screen exposes the expected test tags and behaviors.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class HomeNavigationScenarioTest {
  @get:Rule(order = 0) val environmentRule = TestEnvironmentRule()
  @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun homeScreen_expandTools_and_triggerRecentAction() {
    composeRule.onNodeWithTag("home_hub").assertIsDisplayed()

    composeRule.onNodeWithTag("home_tools_toggle").assertIsDisplayed().assertHasClickAction()
    composeRule.onNodeWithTag("home_tools_panel_collapsed").assertIsDisplayed()

    composeRule.onNodeWithTag("home_tools_toggle").performClick()

    composeRule.onNodeWithTag("home_tools_panel_expanded").assertIsDisplayed()

    val recentItems = composeRule.onAllNodesWithTag("recent_activity_item")
    recentItems[0].assertIsDisplayed().assertHasClickAction().performClick()

    composeRule.waitUntilExactlyOneExists(hasTestTag("home_recent_action_confirmation"))
    composeRule.onNodeWithTag("home_recent_action_confirmation").assertIsDisplayed()
  }
}
