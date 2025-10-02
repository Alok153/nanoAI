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
 * Instrumentation scenario covering Quickstart Scenario 1 (First-Time Welcome).
 *
 * The assertions intentionally fail until the welcome and home flows wire the
 * specified semantics and navigation hooks.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class FirstTimeWelcomeScenarioTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun firstTimeUser_progresses_fromWelcome_toHome() {
        composeRule
            .onNodeWithTag("welcome_hero_title")
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag("welcome_cta_get_started")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        composeRule
            .onNodeWithTag("home_single_column_feed")
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag("onboarding_tooltip_entry")
            .assertIsDisplayed()
    }
}
