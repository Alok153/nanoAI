package com.vjaykrsna.nanoai.shared.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vjaykrsna.nanoai.shared.testing.TestingTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Quickstart Scenario 5 instrumentation: First-launch disclaimer accessibility + blocking
 * behaviour.
 *
 * Assertions:
 * - Dialog rendered with TalkBack semantics (`disclaimer_dialog_container`).
 * - Primary CTA blocked until user scrolls and acknowledges (`disclaimer_accept_button`).
 * - Secondary CTA dismisses with clear accessibility label (`disclaimer_decline_button`).
 */
@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DisclaimerDialogTest {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val composeTestRule = createComposeRule()

  private var acceptClicked by mutableStateOf(false)
  private var declineClicked by mutableStateOf(false)
  private var dismissRequested by mutableStateOf(false)

  @Before
  fun setUp() {
    hiltRule.inject()
    acceptClicked = false
    declineClicked = false
    dismissRequested = false
    
    composeTestRule.setContent {
      TestingTheme {
        DisclaimerDialog(
          onAccept = { acceptClicked = true },
          onDecline = { declineClicked = true },
          onDismissRequest = { dismissRequested = true },
        )
      }
    }
  }

  @Test
  fun disclaimerDialog_requiresAcknowledgement_and_readsWithTalkBack() {
    composeTestRule
      .onNodeWithTag("disclaimer_dialog_container")
      .assertIsDisplayed()
      .assertContentDescriptionEquals("nanoAI privacy disclaimer dialog")

    composeTestRule
      .onNodeWithTag("disclaimer_accept_button")
      .assertIsDisplayed()
      .assertIsNotEnabled()
      .assertContentDescriptionEquals("Accept privacy terms")

    composeTestRule
      .onNodeWithTag("disclaimer_decline_button")
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertContentDescriptionEquals("Decline and review later")
      .assertTextContains("Decline", substring = true)

    composeTestRule.onNodeWithTag("disclaimer_last_text").performScrollTo()

    composeTestRule
      .onNodeWithTag("disclaimer_accept_button")
      .assertIsDisplayed()
      .assertIsEnabled()
      .assertHasClickAction()
      .assertTextContains("Agree", substring = true)
      .performClick()
  }
}
