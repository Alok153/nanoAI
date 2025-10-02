package com.vjaykrsna.nanoai.feature.uiux.scenario

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vjaykrsna.nanoai.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

/**
 * Quickstart Scenario 6 instrumentation: Accessibility conformance.
 *
 * Expectations (fail until accessibility semantics are implemented):
 *  - TalkBack focus order is exposed via tagged nodes `accessibility_focus_step_*`
 *  - Dynamic type sample renders (`dynamic_type_preview`)
 *  - Focus traps for modal surfaces flagged by `accessibility_focus_trap`
 *  - Primary actions expose descriptive content descriptions
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AccessibilityScenarioTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun accessibility_supportsTalkBack_dynamicType_andFocusTraps() {
        // Verify a TalkBack sequence exists and maintains order
        val focusNodes = composeRule.onAllNodesWithTag("accessibility_focus_step")
        focusNodes.assertCountEquals(5)

        val semanticsNodes = focusNodes.fetchSemanticsNodes()
        assertTrue(
            semanticsNodes.any { hasClickAction().matches(it) },
            "T028: At least one accessibility focus step must be actionable for TalkBack navigation.",
        )
        assertTrue(
            semanticsNodes.any { hasContentDescription().matches(it) },
            "T028: Each focus sequence should expose descriptive content for screen readers.",
        )

        composeRule
            .onNodeWithTag("dynamic_type_preview")
            .assertIsDisplayed()
            .assertTextContains("Aa", substring = true)

        composeRule
            .onNodeWithTag("accessibility_focus_trap")
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag("primary_action_button")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertTextContains("Primary", substring = true)
    }
}
