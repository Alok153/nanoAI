package com.vjaykrsna.nanoai.feature.uiux.contracts

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vjaykrsna.nanoai.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Contract test for the Home screen. Captures FR-002 requirements around layout, ordering,
 * and collapsible tools rail behavior. Assertions currently fail until the new UI exists.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class HomeScreenContractTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreen_rendersSingleColumnFeed_withRecentActions() {
        composeRule.onNodeWithTag("home_single_column_feed")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("home_recent_actions_header")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("home_recent_action_0")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun homeScreen_collapsibleToolsPanel_startsCollapsed() {
        composeRule.onNodeWithTag("home_tools_toggle")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeRule.onNodeWithTag("home_tools_panel_collapsed")
            .assertIsDisplayed()
    }
}
