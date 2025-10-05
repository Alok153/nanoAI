package com.vjaykrsna.nanoai.feature.uiux

import androidx.activity.ComponentActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellUiState
import com.vjaykrsna.nanoai.feature.uiux.state.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.state.CommandCategory
import com.vjaykrsna.nanoai.feature.uiux.state.CommandDestination
import com.vjaykrsna.nanoai.feature.uiux.state.CommandPaletteState
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityBannerState
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.state.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.state.UiPreferenceSnapshot
import com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.NanoShellScaffold
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(AndroidJUnit4::class)
class AdaptiveShellTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun compactWidth_usesModalDrawer() {
    val state = mutableStateOf(sampleState(WindowWidthSizeClass.Compact, WindowHeightSizeClass.Expanded))
    composeRule.setContent { NanoShellScaffold(state = state.value, onEvent = { }) }

    composeRule.onNodeWithTag("left_drawer_modal").assertIsDisplayed()
  }

  @Test
  fun expandedWidth_showsPermanentDrawer() {
    val state = mutableStateOf(sampleState(WindowWidthSizeClass.Expanded, WindowHeightSizeClass.Medium))
    composeRule.setContent { NanoShellScaffold(state = state.value, onEvent = { }) }

    composeRule.onNodeWithTag("left_drawer_permanent").assertIsDisplayed()
  }

  @Test
  fun accessibility_focusMovesIntoContent() {
    val state = mutableStateOf(sampleState(WindowWidthSizeClass.Medium, WindowHeightSizeClass.Medium))
    composeRule.setContent { NanoShellScaffold(state = state.value, onEvent = { }) }

    composeRule.onNodeWithTag("shell_content").assertIsDisplayed()
  }

  private fun sampleState(
    width: WindowWidthSizeClass,
    height: WindowHeightSizeClass,
  ): ShellUiState {
    val windowSizeClass = WindowSizeClass(width, height)
    val layout =
      ShellLayoutState(
        windowSizeClass = windowSizeClass,
        isLeftDrawerOpen = width == WindowWidthSizeClass.Compact,
        isRightDrawerOpen = false,
        activeRightPanel = if (width >= WindowWidthSizeClass.Medium) RightPanel.PROGRESS_CENTER else null,
        activeMode = ModeId.HOME,
        showCommandPalette = false,
        connectivity = ConnectivityStatus.ONLINE,
        pendingUndoAction = UndoPayload(actionId = "none"),
        progressJobs = emptyList(),
        recentActivity = emptyList(),
      )
    val palette = CommandPaletteState()
    val banner =
      ConnectivityBannerState(
        status = ConnectivityStatus.ONLINE,
        queuedActionCount = 0,
        cta = CommandAction("view-queue", "View queue", category = CommandCategory.JOBS, destination = CommandDestination.OpenRightPanel(RightPanel.PROGRESS_CENTER)),
      )

    return ShellUiState(
      layout = layout,
      commandPalette = palette,
      connectivityBanner = banner,
      preferences = UiPreferenceSnapshot(),
    )
  }
}
