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
 * Contract test for Settings screen (FR-008).
 * Ensures grouped cards with help affordances and undo mechanics.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SettingsScreenContractTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun settingsScreen_displaysGroupedCards_withInlineHelp() {
        composeRule
            .onNodeWithTag("settings_group_theme")
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag("settings_group_preferences")
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag("settings_inline_help_button")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun settingsScreen_offersUndoAffordance_forChanges() {
        composeRule
            .onNodeWithTag("settings_undo_button")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}
