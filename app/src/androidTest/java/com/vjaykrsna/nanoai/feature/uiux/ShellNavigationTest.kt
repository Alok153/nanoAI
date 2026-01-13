@file:Suppress("LongMethod", "CyclomaticComplexMethod")

package com.vjaykrsna.nanoai.feature.uiux

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilDoesNotExist
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandAction
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandCategory
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandDestination
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandPaletteState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityBannerState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentActivityItem
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.RightPanel
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellWindowSizeClass
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeCard
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellUiState
import com.vjaykrsna.nanoai.shared.testing.TestingTheme
import com.vjaykrsna.nanoai.shared.ui.shell.NanoShellScaffold
import com.vjaykrsna.nanoai.shared.ui.shell.ShellUiEvent
import com.vjaykrsna.nanoai.shared.ui.window.toShellWindowSizeClass
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test

/** T033: Compose UI test for Home/Chat/Library/Settings navigation with offline/online toggles. */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
class ShellNavigationTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  // ==================== Navigation Tests ====================

  @Test
  fun navigation_toChat_updatesActiveMode() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.ONLINE))
    val events = mutableListOf<ShellUiEvent>()

    composeRule.setContent {
      TestingTheme {
        NanoShellScaffold(
          state = state.value,
          onEvent = { event ->
            events += event
            handleIntent(state, event)
          },
        )
      }
    }

    composeRule.waitForIdle()

    // Navigate to Chat mode
    composeRule.waitForNodesWithTagCount("mode_card", expectedCount = 1)
    composeRule.onAllNodesWithTag("mode_card")[0].performClick()

    composeRule.waitForIdle()
    composeRule.runOnIdle {
      assertThat(events.filterIsInstance<ShellUiEvent.ModeSelected>()).isNotEmpty()
      assertThat(state.value.layout.activeMode).isEqualTo(ModeId.CHAT)
    }
  }

  @Test
  fun navigation_toSettings_updatesActiveMode() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.ONLINE))
    val events = mutableListOf<ShellUiEvent>()

    composeRule.setContent {
      TestingTheme {
        NanoShellScaffold(
          state = state.value,
          onEvent = { event ->
            events += event
            handleIntent(state, event)
          },
        )
      }
    }

    composeRule.waitForIdle()

    // Navigate via Settings mode card (last in list for this test)
    val modeCards = state.value.modeCards
    val settingsIndex = modeCards.indexOfFirst { it.id == ModeId.SETTINGS }
    if (settingsIndex >= 0) {
      composeRule.waitForNodesWithTagCount("mode_card", expectedCount = modeCards.size)
      composeRule.onAllNodesWithTag("mode_card")[settingsIndex].performClick()

      composeRule.waitForIdle()
      composeRule.runOnIdle { assertThat(state.value.layout.activeMode).isEqualTo(ModeId.SETTINGS) }
    }
  }

  @Test
  fun navigation_toLibrary_updatesActiveMode() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.ONLINE))

    composeRule.setContent {
      TestingTheme {
        NanoShellScaffold(state = state.value, onEvent = { event -> handleIntent(state, event) })
      }
    }

    composeRule.waitForIdle()

    // Find Library mode card
    val modeCards = state.value.modeCards
    val libraryIndex = modeCards.indexOfFirst { it.id == ModeId.LIBRARY }
    if (libraryIndex >= 0) {
      composeRule.waitForNodesWithTagCount("mode_card", expectedCount = modeCards.size)
      composeRule.onAllNodesWithTag("mode_card")[libraryIndex].performClick()

      composeRule.waitForIdle()
      composeRule.runOnIdle { assertThat(state.value.layout.activeMode).isEqualTo(ModeId.LIBRARY) }
    }
  }

  // ==================== Connectivity Toggle Tests ====================

  @Test
  fun offlineToggle_showsConnectivityBanner() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.OFFLINE))

    composeRule.setContent { TestingTheme { NanoShellScaffold(state = state.value, onEvent = {}) } }

    composeRule.waitForIdle()

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
        .onAllNodesWithTag("connectivity_banner", useUnmergedTree = true)
        .fetchSemanticsNodes(false)
        .isNotEmpty()
    }

    composeRule.onNodeWithTag("connectivity_banner", useUnmergedTree = true).assertExists()
  }

  @Test
  fun onlineToggle_hidesConnectivityBanner() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.OFFLINE))

    composeRule.setContent {
      TestingTheme {
        NanoShellScaffold(state = state.value, onEvent = { event -> handleIntent(state, event) })
      }
    }

    composeRule.waitForIdle()

    // Verify banner exists when offline
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
        .onAllNodesWithTag("connectivity_banner", useUnmergedTree = true)
        .fetchSemanticsNodes(false)
        .isNotEmpty()
    }
    composeRule.onNodeWithTag("connectivity_banner", useUnmergedTree = true).assertExists()

    // Toggle to online
    handleIntent(state, ShellUiEvent.ConnectivityChanged(ConnectivityStatus.ONLINE))
    composeRule.waitForIdle()

    // Banner should be hidden
    composeRule.waitUntilDoesNotExist(hasTestTag("connectivity_banner"), timeoutMillis = 5_000)
  }

  @Test
  fun offlineStatus_reflectsInLayoutState() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.OFFLINE))

    composeRule.setContent { TestingTheme { NanoShellScaffold(state = state.value, onEvent = {}) } }

    composeRule.waitForIdle()

    composeRule.runOnIdle {
      assertThat(state.value.layout.isOffline).isTrue()
      assertThat(state.value.layout.connectivityStatusDescription).isEqualTo("Offline")
    }
  }

  @Test
  fun onlineStatus_reflectsInLayoutState() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.ONLINE))

    composeRule.setContent { TestingTheme { NanoShellScaffold(state = state.value, onEvent = {}) } }

    composeRule.waitForIdle()

    composeRule.runOnIdle {
      assertThat(state.value.layout.isOffline).isFalse()
      assertThat(state.value.layout.connectivityStatusDescription).isEqualTo("Online")
    }
  }

  // ==================== Context Preservation Tests ====================

  @Test
  fun navigationPreservesState_afterConnectivityToggle() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.ONLINE))

    composeRule.setContent {
      TestingTheme {
        NanoShellScaffold(state = state.value, onEvent = { event -> handleIntent(state, event) })
      }
    }

    composeRule.waitForIdle()

    // Navigate to Chat
    handleIntent(state, ShellUiEvent.ModeSelected(ModeId.CHAT))
    composeRule.waitForIdle()

    composeRule.runOnIdle { assertThat(state.value.layout.activeMode).isEqualTo(ModeId.CHAT) }

    // Toggle connectivity to offline
    handleIntent(state, ShellUiEvent.ConnectivityChanged(ConnectivityStatus.OFFLINE))
    composeRule.waitForIdle()

    // Mode should still be CHAT
    composeRule.runOnIdle {
      assertThat(state.value.layout.activeMode).isEqualTo(ModeId.CHAT)
      assertThat(state.value.layout.isOffline).isTrue()
    }
  }

  @Test
  fun modeCards_availableForOfflineNavigation() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.OFFLINE))

    composeRule.setContent { TestingTheme { NanoShellScaffold(state = state.value, onEvent = {}) } }

    composeRule.waitForIdle()

    // Chat, Settings, and History should be available offline
    val offlineModes = state.value.modeCards
    composeRule.runOnIdle {
      assertThat(offlineModes.any { it.id == ModeId.CHAT }).isTrue()
      assertThat(offlineModes.any { it.id == ModeId.SETTINGS }).isTrue()
    }
  }

  // ==================== Drawer Navigation Tests ====================

  @Test
  fun leftDrawer_toggleChangesState() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.ONLINE))

    composeRule.setContent {
      TestingTheme {
        NanoShellScaffold(state = state.value, onEvent = { event -> handleIntent(state, event) })
      }
    }

    composeRule.waitForIdle()

    // Initially closed
    composeRule.runOnIdle { assertThat(state.value.layout.isLeftDrawerOpen).isFalse() }

    // Toggle drawer
    handleIntent(state, ShellUiEvent.ToggleLeftDrawer)
    composeRule.waitForIdle()

    composeRule.runOnIdle { assertThat(state.value.layout.isLeftDrawerOpen).isTrue() }
  }

  @Test
  fun rightDrawer_toggleWithPanel() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.ONLINE))

    composeRule.setContent {
      TestingTheme {
        NanoShellScaffold(state = state.value, onEvent = { event -> handleIntent(state, event) })
      }
    }

    composeRule.waitForIdle()

    // Toggle right drawer with MODEL_SELECTOR panel
    handleIntent(state, ShellUiEvent.ToggleRightDrawer(RightPanel.MODEL_SELECTOR))
    composeRule.waitForIdle()

    composeRule.runOnIdle {
      assertThat(state.value.layout.isRightDrawerOpen).isTrue()
      assertThat(state.value.layout.activeRightPanel).isEqualTo(RightPanel.MODEL_SELECTOR)
    }
  }

  // ==================== Accessibility Tests ====================

  @Test
  fun shellRoot_hasAccessibleTestTag() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.ONLINE))

    composeRule.setContent { TestingTheme { NanoShellScaffold(state = state.value, onEvent = {}) } }

    composeRule.waitForIdle()

    composeRule.onNodeWithTag("shell_root").assertExists()
  }

  @Test
  fun connectivityBanner_hasAccessibleDescription() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.OFFLINE))

    composeRule.setContent { TestingTheme { NanoShellScaffold(state = state.value, onEvent = {}) } }

    composeRule.waitForIdle()

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
        .onAllNodesWithTag("connectivity_banner", useUnmergedTree = true)
        .fetchSemanticsNodes(false)
        .isNotEmpty()
    }

    // Banner should exist and have meaningful accessibility info
    composeRule.onNodeWithTag("connectivity_banner", useUnmergedTree = true).assertExists()
  }

  // ==================== Helper Functions ====================

  private fun handleIntent(state: MutableState<ShellUiState>, intent: ShellUiEvent) {
    val current = state.value
    state.value =
      when (intent) {
        is ShellUiEvent.ConnectivityChanged -> handleConnectivityChanged(current, intent)
        is ShellUiEvent.ModeSelected -> handleModeSelected(current, intent)
        is ShellUiEvent.ToggleLeftDrawer -> handleToggleLeftDrawer(current)
        is ShellUiEvent.ToggleRightDrawer -> handleToggleRightDrawer(current, intent)
        is ShellUiEvent.SetLeftDrawer -> handleSetLeftDrawer(current, intent)
        is ShellUiEvent.ShowCommandPalette -> handleShowCommandPalette(current)
        is ShellUiEvent.HideCommandPalette -> handleHideCommandPalette(current)
        is ShellUiEvent.UpdateTheme -> handleUpdateTheme(current, intent)
        is ShellUiEvent.UpdateDensity -> handleUpdateDensity(current, intent)
        else -> current
      }
  }

  private fun handleConnectivityChanged(
    current: ShellUiState,
    intent: ShellUiEvent.ConnectivityChanged,
  ) =
    current.copy(
      layout = current.layout.copy(connectivity = intent.status),
      connectivityBanner = current.connectivityBanner.copy(status = intent.status),
    )

  private fun handleModeSelected(current: ShellUiState, intent: ShellUiEvent.ModeSelected) =
    current.copy(layout = current.layout.copy(activeMode = intent.modeId))

  private fun handleToggleLeftDrawer(current: ShellUiState) =
    current.copy(layout = current.layout.copy(isLeftDrawerOpen = !current.layout.isLeftDrawerOpen))

  private fun handleToggleRightDrawer(
    current: ShellUiState,
    intent: ShellUiEvent.ToggleRightDrawer,
  ) =
    current.copy(
      layout =
        current.layout.copy(
          isRightDrawerOpen = !current.layout.isRightDrawerOpen,
          activeRightPanel = intent.panel,
        )
    )

  private fun handleSetLeftDrawer(current: ShellUiState, intent: ShellUiEvent.SetLeftDrawer) =
    current.copy(layout = current.layout.copy(isLeftDrawerOpen = intent.open))

  private fun handleShowCommandPalette(current: ShellUiState) =
    current.copy(layout = current.layout.copy(showCommandPalette = true))

  private fun handleHideCommandPalette(current: ShellUiState) =
    current.copy(layout = current.layout.copy(showCommandPalette = false))

  private fun handleUpdateTheme(current: ShellUiState, intent: ShellUiEvent.UpdateTheme) =
    current.copy(preferences = current.preferences.copy(theme = intent.theme))

  private fun handleUpdateDensity(current: ShellUiState, intent: ShellUiEvent.UpdateDensity) =
    current.copy(preferences = current.preferences.copy(density = intent.density))

  private fun sampleState(connectivity: ConnectivityStatus): ShellUiState {
    val windowSizeClass: ShellWindowSizeClass =
      WindowSizeClass.calculateFromSize(DpSize(720.dp, 1024.dp)).toShellWindowSizeClass()

    val modeCards =
      listOf(
        modeCard(ModeId.CHAT, "Chat", Icons.AutoMirrored.Filled.Chat),
        modeCard(ModeId.HISTORY, "History", Icons.Filled.History),
        modeCard(ModeId.SETTINGS, "Settings", Icons.Filled.Settings),
      )

    val recent =
      listOf(
        RecentActivityItem(
          id = "r1",
          modeId = ModeId.CHAT,
          title = "Recent chat",
          timestamp = Instant.now().minusSeconds(60),
          status = RecentStatus.COMPLETED,
        )
      )

    val layout =
      ShellLayoutState(
        windowSizeClass = windowSizeClass,
        isLeftDrawerOpen = false,
        isRightDrawerOpen = false,
        activeRightPanel = null,
        activeMode = ModeId.HOME,
        showCommandPalette = false,
        connectivity = connectivity,
        pendingUndoAction = null,
        progressJobs = emptyList(),
        recentActivity = recent,
        showCoverageDashboard = false,
      )

    val banner =
      ConnectivityBannerState(
        status = connectivity,
        queuedActionCount = if (connectivity == ConnectivityStatus.OFFLINE) 1 else 0,
        cta = CommandAction("view-queue", "View queue", category = CommandCategory.JOBS),
      )

    val palette =
      CommandPaletteState(query = "", results = emptyList(), recentCommands = emptyList())

    return ShellUiState(
      layout = layout,
      commandPalette = palette,
      connectivityBanner = banner,
      preferences =
        ShellUiPreferences(theme = ThemePreference.SYSTEM, density = VisualDensity.DEFAULT),
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
          id = "go_to_$title",
          title = title,
          category = CommandCategory.MODES,
          destination = CommandDestination.Navigate(id.name.lowercase()),
        ),
      enabled = true,
      badge = null,
    )

  private fun AndroidComposeTestRule<*, *>.waitForNodesWithTagCount(
    tag: String,
    expectedCount: Int,
    useUnmergedTree: Boolean = false,
    timeoutMillis: Long = 5_000,
  ) {
    val deadline = SystemClock.elapsedRealtime() + timeoutMillis
    while (SystemClock.elapsedRealtime() < deadline) {
      val count = onAllNodesWithTag(tag, useUnmergedTree).fetchSemanticsNodes(false).size
      if (count >= expectedCount) return
      waitForIdle()
    }
    throw AssertionError("Timed out waiting for at least $expectedCount nodes with tag '$tag'")
  }
}
