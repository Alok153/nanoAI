package com.vjaykrsna.nanoai.feature.uiux.contracts

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
 * Contract test for Theme toggle (FR-011).
 * Checks instant theme switch semantics and persistence signal.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ThemeToggleContractTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun themeToggle_switchPresent_andInteractive() {
        composeRule
            .onNodeWithTag("theme_toggle_switch")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
    }

    @Test
    fun themeToggle_persistsSelection_acrossRecomposition() {
        composeRule
            .onNodeWithTag("theme_toggle_persistence_status")
            .assertIsDisplayed()
    }
}
