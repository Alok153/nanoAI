package com.vjaykrsna.nanoai.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.vjaykrsna.nanoai.core.model.InferenceMode
import com.vjaykrsna.nanoai.ui.sidebar.InferencePreferenceToggleRow
import com.vjaykrsna.nanoai.ui.theme.NanoAITheme
import org.junit.Rule
import org.junit.Test

class InferenceToggleAccessibilityTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun inferenceToggle_exposesContentDescription() {
    composeRule.setContent {
      NanoAITheme {
        InferencePreferenceToggleRow(
          inferenceMode = InferenceMode.LOCAL_FIRST,
          onInferenceModeChange = {},
        )
      }
    }

    composeRule.onNodeWithContentDescription("Toggle inference preference").assertIsDisplayed()
  }
}
