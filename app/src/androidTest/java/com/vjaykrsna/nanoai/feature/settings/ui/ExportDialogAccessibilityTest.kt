package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithContentDescription
import com.vjaykrsna.nanoai.ui.theme.NanoAITheme
import org.junit.Rule
import org.junit.Test

class ExportDialogAccessibilityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun exportDialog_controlsExposeSemantics() {
        composeRule.setContent {
            NanoAITheme {
                ExportDialog(
                    onDismiss = {},
                    onConfirm = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Confirm export backup").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Cancel export backup").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Don't warn me again checkbox").assertIsDisplayed()
        composeRule.onNode(hasText("Don't warn me again")).assertIsDisplayed()
    }
}
