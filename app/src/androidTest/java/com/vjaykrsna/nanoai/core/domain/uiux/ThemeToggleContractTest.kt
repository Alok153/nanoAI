package com.vjaykrsna.nanoai.core.domain.uiux

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.filters.LargeTest
import com.vjaykrsna.nanoai.MainActivity
import com.vjaykrsna.nanoai.shared.testing.TestEnvironmentRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * Contract test for Theme toggle (FR-011). Checks instant theme switch semantics and persistence
 * signal.
 */
@LargeTest
@HiltAndroidTest
@Ignore("Theme toggle contract pending retained preferences storage; see specs/003-UI-UX/plan.md")
class ThemeToggleContractTest {

  @JvmField @Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @JvmField @Rule(order = 1) val environmentRule = TestEnvironmentRule()
  @JvmField @Rule(order = 2) val composeRule = createAndroidComposeRule<MainActivity>()

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun themeToggle_switchPresent_andInteractive() {
    composeRule
      .onNodeWithTag("theme_toggle_switch")
      .assertIsDisplayed()
      .assertHasClickAction()
      .performClick()

    composeRule.waitForIdle()
    composeRule
      .onNodeWithTag("theme_toggle_persistence_status")
      .assertIsDisplayed()
      .assertTextContains("Current:", substring = true)
  }

  @Test
  fun themeToggle_persistsSelection_acrossRecomposition() {
    composeRule.onNodeWithTag("theme_toggle_switch").performClick()
    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(500)
    composeRule.waitForIdle()

    composeRule
      .onNodeWithTag("theme_toggle_persistence_status")
      .assertIsDisplayed()
      .assertTextContains("Dark", substring = true)

    composeRule.activityRule.scenario.recreate()
    composeRule.waitForIdle()

    composeRule
      .onNodeWithTag("theme_toggle_persistence_status")
      .assertIsDisplayed()
      .assertTextContains("Dark", substring = true)
  }
}
