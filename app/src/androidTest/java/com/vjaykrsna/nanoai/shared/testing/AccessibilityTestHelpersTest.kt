package com.vjaykrsna.nanoai.shared.testing

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityTestHelpersTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun minimumTouchTargetAssertionUsesRule() {
    composeRule.setContent { TestButton() }

    with(composeRule.accessibilityHelpers()) {
      composeRule.onNodeWithTag("touch_target").assertMinimumTouchTarget()
    }
  }
}

@Composable
private fun TestButton() {
  MaterialTheme { Surface { Box(modifier = Modifier.size(64.dp).testTag("touch_target")) } }
}
