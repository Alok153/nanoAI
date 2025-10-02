package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.ui.theme.NanoAITheme
import org.junit.Rule
import org.junit.Test

class FirstLaunchDisclaimerDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun disclaimerDialog_rendersAndAcknowledgeInvokesCallback() {
        var acknowledged = false

        composeRule.setContent {
            NanoAITheme {
                FirstLaunchDisclaimerDialog(
                    isVisible = true,
                    onAcknowledge = { acknowledged = true },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Use nanoAI responsibly").assertIsDisplayed()
        composeRule.onNodeWithText("Acknowledge").assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertThat(acknowledged).isTrue()
        }
    }

    @Test
    fun disclaimerDialog_buttonsExposeContentDescriptions() {
        composeRule.setContent {
            NanoAITheme {
                FirstLaunchDisclaimerDialog(
                    isVisible = true,
                    onAcknowledge = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Acknowledge disclaimer and continue").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Dismiss disclaimer").assertIsDisplayed()
    }
}
