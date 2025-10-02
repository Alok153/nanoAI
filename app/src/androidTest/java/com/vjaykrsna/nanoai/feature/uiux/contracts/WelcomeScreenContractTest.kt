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
 * Contract test for the Welcome screen. This test captures FR-001 requirements:
 * hero messaging, CTA semantics, and skip affordance.
 *
 * The assertions currently fail because the production UI has not yet implemented
 * the required test tags and semantics. Implementing the Welcome screen should
 * make these assertions pass.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class WelcomeScreenContractTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun welcomeScreen_displaysHeroMessage_andPrimaryCtas() {
        composeRule.onNodeWithTag("welcome_hero_title")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("welcome_cta_get_started")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeRule.onNodeWithTag("welcome_cta_explore")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun welcomeScreen_offersSkipControlWithinSemantics() {
        composeRule.onNodeWithTag("welcome_skip")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}
