package com.vjaykrsna.nanoai.shared.ui.shell

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellLayoutState
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShellDrawerGesturesTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  fun swipeFromLeftEdgeEmitsToggleEvent() {
    val events = mutableListOf<ShellUiEvent>()
    val thresholds = DrawerGestureThresholds(edge = 80f, panelWidth = 320f, open = 40f, close = 30f)

    var layout by
      mutableStateOf(
        ShellLayoutState.empty(
          WindowSizeClass.calculateFromSize(DpSize(width = 360.dp, height = 780.dp))
        )
      )

    composeRule.setContent {
      Box(
        modifier =
          Modifier.size(400.dp)
            .shellDrawerGestures(layout, thresholds) { event ->
              events += event
              when (event) {
                ShellUiEvent.ToggleLeftDrawer ->
                  layout = layout.copy(isLeftDrawerOpen = !layout.isLeftDrawerOpen)
                is ShellUiEvent.ToggleRightDrawer ->
                  layout =
                    layout.copy(
                      isRightDrawerOpen = !layout.isRightDrawerOpen,
                      activeRightPanel = event.panel,
                    )
                else -> Unit
              }
            }
            .testTag("shell_drawer_target")
      )
    }

    composeRule.onNodeWithTag("shell_drawer_target").performTouchInput {
      val centerY = size.height / 2f
      down(Offset(1f, centerY))
      moveTo(Offset(200f, centerY))
      up()
    }

    composeRule.runOnIdle { assertEquals(listOf(ShellUiEvent.ToggleLeftDrawer), events) }
  }
}
