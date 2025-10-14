package com.vjaykrsna.nanoai.feature.uiux.contracts

import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vjaykrsna.nanoai.MainActivity
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Contract test for the Home screen. Captures FR-002 requirements around layout, ordering, and
 * collapsible tools rail behavior. Assertions currently fail until the new UI exists.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class HomeScreenContractTest {
  @get:Rule(order = 0) val environmentRule = TestEnvironmentRule()
  @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun homeScreen_modeGrid_exposesColumnMetadata() {
    composeRule.onNodeWithTag("home_hub").assertIsDisplayed()

    val expectedCollection = CollectionInfo(rowCount = 1, columnCount = 3)
    composeRule
      .onNodeWithTag("home_mode_grid")
      .assertIsDisplayed()
      .assert(SemanticsMatcher.expectValue(SemanticsProperties.CollectionInfo, expectedCollection))

    composeRule.onAllNodesWithTag("mode_card").assertCountEquals(3)
  }

  @Test
  fun homeScreen_collapsibleToolsPanel_startsCollapsed() {
    composeRule.onNodeWithTag("home_tools_toggle").assertIsDisplayed().assertHasClickAction()

    composeRule.onNodeWithTag("home_tools_panel_collapsed").assertIsDisplayed()
  }

  @Test
  fun homeScreen_recentFeed_announcesAccessibilitySemantics() {
    composeRule.onNodeWithTag("recent_activity_list").assertIsDisplayed()
    composeRule
      .onAllNodesWithTag("recent_activity_item")[0]
      .assertIsDisplayed()
      .assertContentDescriptionContains("status", substring = true)
  }
}
