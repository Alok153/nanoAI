package com.vjaykrsna.nanoai.feature.uiux.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.filters.LargeTest
import com.vjaykrsna.nanoai.MainActivity
import com.vjaykrsna.nanoai.shared.testing.TestEnvironmentRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * Instrumentation scenario covering Quickstart Scenario 3 (Sidebar and Settings). Assertions fail
 * until the sidebar + settings flows provide the required semantics.
 */
@LargeTest
@HiltAndroidTest
@Ignore("Sidebar settings scenario pending navigation wiring; see specs/003-UI-UX/plan.md")
class SidebarSettingsScenarioTest {
  @JvmField @Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @JvmField @Rule(order = 1) val environmentRule = TestEnvironmentRule()

  @JvmField @Rule(order = 2) val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun sidebarNavigation_reachesSettings_withUndoAffordance() {
    composeRule.onNodeWithTag("topbar_nav_icon").assertIsDisplayed().assertHasClickAction()
    composeRule
      .onNodeWithContentDescription("Toggle navigation drawer")
      .assertIsDisplayed()
      .assertHasClickAction()
      .performClick()

    composeRule.onNodeWithTag("sidebar_drawer").assertIsDisplayed()

    composeRule
      .onNodeWithTag("sidebar_item_settings")
      .assertIsDisplayed()
      .assertHasClickAction()
      .performClick()

    composeRule.onNodeWithText("Settings").assertIsDisplayed()
    composeRule
      .onNodeWithContentDescription("Settings screen organized by tabs with contextual sections")
      .assertIsDisplayed()

    composeRule.onNodeWithTag("settings_undo_action").assertIsDisplayed().assertIsEnabled()
  }
}
