@file:Suppress("LongMethod")

package com.vjaykrsna.nanoai.feature.uiux.ui

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandAction
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandCategory
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandDestination
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandInvocationSource
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandPaletteState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityBannerState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentActivityItem
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.RightPanel
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellWindowSizeClass
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeCard
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellUiState
import com.vjaykrsna.nanoai.shared.testing.TestingTheme
import com.vjaykrsna.nanoai.shared.ui.shell.ShellUiEvent
import com.vjaykrsna.nanoai.shared.ui.window.toShellWindowSizeClass
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
class HomeHubFlowTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun modeCards_renderAndTriggerModeSelection() {
    val events = mutableListOf<ShellUiEvent>()
    val shellState = sampleState()

    renderHomeScreen(shellState) { event -> events += event }

    composeRule.waitForIdle()

    composeRule.waitForNodesWithTagCount("mode_card", expectedCount = 3)
    val modeCards = composeRule.onAllNodesWithTag("mode_card")
    modeCards.assertCountEquals(3)
    modeCards[0].performClick()

    composeRule.waitForIdle()
    composeRule.runOnIdle {
      assertThat(events.filterIsInstance<ShellUiEvent.ModeSelected>().first().modeId)
        .isEqualTo(ModeId.CHAT)
    }
  }

  @Test
  fun recentActivity_showsLatestEntries() {
    val shellState = sampleState()
    renderHomeScreen(shellState)

    composeRule.waitForIdle()

    composeRule.waitForNodesWithTagPrefixCount(prefix = "recent_activity_item", expectedCount = 2)

    composeRule.onNodeWithTag("recent_activity_list").assertExists()
    val items = composeRule.onAllNodesWithTagPrefix("recent_activity_item")
    items.assertCountEquals(2)
    items[0].assertExists()
  }

  @Test
  fun quickActions_sectionVisible() {
    val events = mutableListOf<ShellUiEvent>()
    val shellState = sampleState()
    renderHomeScreen(shellState) { event -> events += event }

    composeRule.waitForIdle()

    composeRule.onNodeWithTag("home_tools_toggle").assertExists().performClick()

    composeRule.waitForNodesWithTagCount(tag = "quick_actions_row", expectedCount = 1)

    composeRule.onNodeWithTag("quick_actions_row").assertExists()
    composeRule.onNodeWithContentDescription("New Chat").assertExists().performClick()

    composeRule.waitForIdle()
    composeRule.runOnIdle {
      assertThat(events.filterIsInstance<ShellUiEvent.CommandInvoked>().first().action.title)
        .isEqualTo("New Chat")
      assertThat(
          events.filterIsInstance<ShellUiEvent.ModeSelected>().any { it.modeId == ModeId.CHAT }
        )
        .isTrue()
    }
  }

  private fun sampleState(): ShellUiState {
    val windowSizeClass: ShellWindowSizeClass =
      WindowSizeClass.calculateFromSize(DpSize(720.dp, 1024.dp)).toShellWindowSizeClass()
    val modeCards =
      listOf(
        modeCard(ModeId.CHAT, "Chat", Icons.AutoMirrored.Filled.Chat),
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
        activeRightPanel = RightPanel.MODEL_SELECTOR,
        activeMode = ModeId.HOME,
        showCommandPalette = false,
        connectivity = ConnectivityStatus.ONLINE,
        pendingUndoAction = null,
        progressJobs = emptyList(),
        recentActivity = recent,
        showCoverageDashboard = false,
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
      preferences = ShellUiPreferences(),
      modeCards = modeCards,
      quickActions = modeCards.map(ModeCard::primaryAction),
    )
  }

  private fun renderHomeScreen(state: ShellUiState, onEvent: (ShellUiEvent) -> Unit = {}) {
    composeRule.setContent {
      TestingTheme {
        HomeScreen(
          layout = state.layout,
          modeCards = state.modeCards,
          quickActions = state.quickActions,
          recentActivity = state.layout.recentActivity,
          progressJobs = state.layout.progressJobs,
          onModeSelect = { mode -> onEvent(ShellUiEvent.ModeSelected(mode)) },
          onQuickActionSelect = { action ->
            onEvent(ShellUiEvent.CommandInvoked(action, CommandInvocationSource.QUICK_ACTION))
          },
          onRecentActivitySelect = { item -> onEvent(ShellUiEvent.ModeSelected(item.modeId)) },
          onProgressRetry = { job -> onEvent(ShellUiEvent.RetryJob(job)) },
          onProgressDismiss = { job -> onEvent(ShellUiEvent.CompleteJob(job.jobId)) },
        )
      }
    }
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

  private fun ComposeContentTestRule.waitUntilWithClock(
    timeoutMillis: Long = 5_000,
    condition: () -> Boolean,
  ) {
    val deadline = SystemClock.elapsedRealtime() + timeoutMillis
    while (SystemClock.elapsedRealtime() < deadline) {
      if (condition()) return
      runOnIdle {}
    }
    throw AssertionError("Condition not met within $timeoutMillis ms")
  }

  private fun ComposeContentTestRule.waitForNodesWithTagCount(
    tag: String,
    expectedCount: Int,
    useUnmergedTree: Boolean = false,
    timeoutMillis: Long = 5_000,
  ) {
    waitUntilWithClock(timeoutMillis) {
      onAllNodesWithTag(tag, useUnmergedTree).fetchSemanticsNodes(false).size >= expectedCount
    }
  }

  private fun ComposeContentTestRule.waitForNodesWithTagPrefixCount(
    prefix: String,
    expectedCount: Int,
    useUnmergedTree: Boolean = false,
    timeoutMillis: Long = 5_000,
  ) {
    waitUntilWithClock(timeoutMillis) {
      onAllNodesWithTagPrefix(prefix, useUnmergedTree).fetchSemanticsNodes(false).size >=
        expectedCount
    }
  }

  private fun ComposeContentTestRule.onAllNodesWithTagPrefix(
    prefix: String,
    useUnmergedTree: Boolean = false,
  ): SemanticsNodeInteractionCollection =
    onAllNodes(
      SemanticsMatcher("Has test tag with prefix $prefix") { node ->
        val tag = node.config.getOrNull(SemanticsProperties.TestTag)
        tag?.startsWith(prefix) == true
      },
      useUnmergedTree = useUnmergedTree,
    )
}
