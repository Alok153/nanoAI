package com.vjaykrsna.nanoai.feature.uiux.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationSkipLinksTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun skipLinksInvokeCallbacks() {
    var contentInvocations by mutableIntStateOf(0)
    var navigationInvocations by mutableIntStateOf(0)

    composeRule.setContent {
      SkipLinksNavigation(
        onSkipToContent = { contentInvocations += 1 },
        onSkipToNavigation = { navigationInvocations += 1 },
      )
    }

    val contentNode = composeRule.onNodeWithTag("skip_link_content")
    contentNode.assertContentDescriptionContains("Skip to main content")
    contentNode.performClick()

    val navigationNode = composeRule.onNodeWithTag("skip_link_navigation")
    navigationNode.assertContentDescriptionContains("Skip to navigation menu")
    navigationNode.performClick()

    composeRule.runOnIdle {
      assertEquals(1, contentInvocations)
      assertEquals(1, navigationInvocations)
    }
  }
}
