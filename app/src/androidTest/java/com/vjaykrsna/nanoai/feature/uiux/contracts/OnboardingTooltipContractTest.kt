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
 * Contract test for the contextual onboarding tooltip (FR-013). Validates dismiss, "Don't show
 * again", and Help re-entry semantics.
 *
 * The assertions fail until the tooltip component wires the required semantics.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class OnboardingTooltipContractTest {
  @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun onboardingTooltip_exposesDismissAndDontShowAgain() {
    composeRule.onNodeWithTag("onboarding_tooltip_container").assertIsDisplayed()

    composeRule
      .onNodeWithTag("onboarding_tooltip_dismiss")
      .assertIsDisplayed()
      .assertHasClickAction()

    composeRule
      .onNodeWithTag("onboarding_tooltip_dont_show_again")
      .assertIsDisplayed()
      .assertHasClickAction()
  }

  @Test
  fun onboardingTooltip_helpEntry_isReachable() {
    composeRule
      .onNodeWithTag("onboarding_tooltip_help_entry")
      .assertIsDisplayed()
      .assertHasClickAction()
  }
}
