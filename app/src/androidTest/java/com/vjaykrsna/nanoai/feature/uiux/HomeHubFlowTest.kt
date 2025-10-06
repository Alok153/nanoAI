package com.vjaykrsna.nanoai.feature.uiux

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellUiState
import com.vjaykrsna.nanoai.feature.uiux.state.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.state.CommandCategory
import com.vjaykrsna.nanoai.feature.uiux.state.CommandDestination
import com.vjaykrsna.nanoai.feature.uiux.state.CommandPaletteState
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityBannerState
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.state.ModeCard
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.RecentActivityItem
import com.vjaykrsna.nanoai.feature.uiux.state.RecentStatus
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.state.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.state.UiPreferenceSnapshot
import com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.NanoShellScaffold
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.ShellUiEvent
import java.time.Instant
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(AndroidJUnit4::class)
class HomeHubFlowTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun modeCards_renderAndTriggerModeSelection() {
    val events = mutableListOf<ShellUiEvent>()
    val state = mutableStateOf(sampleState())

    composeRule.setContent {
      NanoShellScaffold(state = state.value, onEvent = { event -> events += event })
    }

    composeRule.onAllNodesWithTag("mode_card").assertCountEquals(3)
    composeRule.onAllNodesWithTag("mode_card")[0].performClick()
    assertThat(events.filterIsInstance<ShellUiEvent.ModeSelected>().first().modeId)
      .isEqualTo(ModeId.CHAT)
  }

  @Test
  fun recentActivity_showsLatestEntries() {
    val state = mutableStateOf(sampleState())
    composeRule.setContent { NanoShellScaffold(state = state.value, onEvent = {}) }

    composeRule.onNodeWithTag("recent_activity_list").assertIsDisplayed()
    composeRule.onAllNodesWithTag("recent_activity_item").assertCountEquals(2)
  }

  @Test
  fun quickActions_sectionVisible() {
    val state = mutableStateOf(sampleState())
    composeRule.setContent { NanoShellScaffold(state = state.value, onEvent = {}) }

    composeRule.onNodeWithTag("quick_actions_row").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("New Chat").assertIsDisplayed()
  }

  private fun sampleState(): ShellUiState {
    val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(720.dp, 1024.dp))
    val modeCards =
      listOf(
        modeCard(ModeId.CHAT, "Chat", Icons.Filled.Chat),
        modeCard(ModeId.IMAGE, "Image", Icons.Filled.Image),
        modeCard(ModeId.AUDIO, "Audio", Icons.Filled.Mic),
      )
    val recent =
      listOf(
        RecentActivityItem(
          id = "r1",
          modeId = ModeId.CHAT,
          title = "Prompt brainstorm",
          timestamp = Instant.now().minusSeconds(60),
          status = RecentStatus.COMPLETED,
        ),
        RecentActivityItem(
          id = "r2",
          modeId = ModeId.IMAGE,
          title = "Moodboard",
          timestamp = Instant.now().minusSeconds(120),
          status = RecentStatus.IN_PROGRESS,
        ),
      )
    val layout =
      ShellLayoutState(
        windowSizeClass = windowSizeClass,
        isLeftDrawerOpen = false,
        isRightDrawerOpen = false,
        activeRightPanel = RightPanel.PROGRESS_CENTER,
        activeMode = ModeId.HOME,
        showCommandPalette = false,
        connectivity = ConnectivityStatus.ONLINE,
        pendingUndoAction = UndoPayload(actionId = "none"),
        progressJobs = emptyList(),
        recentActivity = recent,
      )
    val palette =
      CommandPaletteState(
        query = "",
        results = emptyList(),
        recentCommands = emptyList(),
        selectedIndex = 0,
        surfaceTarget = CommandCategory.MODES,
      )
    val banner =
      ConnectivityBannerState(
        status = ConnectivityStatus.ONLINE,
        queuedActionCount = 0,
        cta = CommandAction("view-queue", "View queue", category = CommandCategory.JOBS),
      )

    return ShellUiState(
      layout = layout,
      commandPalette = palette,
      connectivityBanner = banner,
      preferences = UiPreferenceSnapshot(),
      modeCards = modeCards,
      quickActions = modeCards.map(ModeCard::primaryAction),
    )
  }

  private fun modeCard(id: ModeId, title: String, icon: ImageVector): ModeCard =
    ModeCard(
      id = id,
      title = title,
      icon = icon,
      primaryAction =
        CommandAction(
          id = "quick-$title",
          title = "New $title",
          category = CommandCategory.MODES,
          destination = CommandDestination.Navigate(id.name.lowercase()),
        ),
      enabled = true,
      badge = null,
    )
}
