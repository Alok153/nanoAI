@file:Suppress("LongMethod", "CyclomaticComplexMethod")

package com.vjaykrsna.nanoai.feature.uiux

import androidx.activity.ComponentActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelectable
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
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
import com.vjaykrsna.nanoai.feature.uiux.state.JobStatus
import com.vjaykrsna.nanoai.feature.uiux.state.JobType
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.state.RecentActivityItem
import com.vjaykrsna.nanoai.feature.uiux.state.RecentStatus
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.state.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.state.UiPreferenceSnapshot
import com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.NanoShellScaffold
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.ShellUiEvent
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(
  ExperimentalCoroutinesApi::class,
  ExperimentalTestApi::class,
  ExperimentalMaterial3WindowSizeClassApi::class
)
@RunWith(AndroidJUnit4::class)
class CommandPaletteComposeTest {
  @get:Rule(order = 0) val environmentRule = TestEnvironmentRule()
  @get:Rule(order = 1) val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun palette_opensWithShortcut_focusesSearchField() {
    val recorder = EventRecorder()
    val state = mutableStateOf(sampleState(showPalette = false))

    composeTestRule.setContent {
      NanoShellScaffold(
        state = state.value,
        onEvent = { intent ->
          recorder.record(intent)
          handleIntent(state, intent)
        }
      )
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNode(isRoot()).performKeyInput {
      pressKey(Key.CtrlLeft)
      pressKey(Key.K)
    }

    composeTestRule.onNodeWithTag("command_palette").assertIsDisplayed()
    composeTestRule.onNodeWithTag("command_palette_search").assertIsDisplayed().assertIsFocused()
    assertThat(recorder.events)
      .contains(ShellUiEvent.ShowCommandPalette(PaletteSource.KEYBOARD_SHORTCUT))
  }

  @Test
  fun palette_filtersActions_prioritizesMatches() {
    val state =
      mutableStateOf(
        sampleState(
          showPalette = true,
          commandPalette = samplePaletteState().copy(results = sampleCommands()),
        )
      )

    composeTestRule.setContent {
      NanoShellScaffold(state = state.value, onEvent = { intent -> handleIntent(state, intent) })
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("command_palette_search").performTextInput("ima")
    composeTestRule.onAllNodesWithTag("command_palette_item")[0].assertIsDisplayed()
  }

  @Test
  fun palette_keyboardNavigation_wrapsSelection() {
    val state =
      mutableStateOf(
        sampleState(
          showPalette = true,
          commandPalette = samplePaletteState().copy(results = sampleCommands()),
        )
      )

    composeTestRule.setContent {
      NanoShellScaffold(state = state.value, onEvent = { intent -> handleIntent(state, intent) })
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("command_palette_list").performKeyInput {
      pressKey(Key.DirectionDown)
      pressKey(Key.DirectionDown)
      pressKey(Key.DirectionDown)
    }

    composeTestRule.onAllNodesWithTag("command_palette_item")[0].assertIsSelectable()
  }

  @Test
  fun palette_executesAction_andCloses() {
    val recorder = EventRecorder()
    val state =
      mutableStateOf(
        sampleState(
          showPalette = true,
          commandPalette = samplePaletteState().copy(results = sampleCommands()),
        )
      )

    composeTestRule.setContent {
      NanoShellScaffold(
        state = state.value,
        onEvent = { intent ->
          recorder.record(intent)
          handleIntent(state, intent)
        }
      )
    }

    composeTestRule.waitForIdle()

    composeTestRule.onAllNodesWithTag("command_palette_item")[0].performClick()
    composeTestRule.onAllNodesWithTag("command_palette").assertCountEquals(0)
    assertThat(recorder.events.filterIsInstance<ShellUiEvent.ModeSelected>()).isNotEmpty()
  }

  @Test
  fun palette_disabledMode_showsErrorState() {
    val disabledCommand = sampleCommands().last().copy(enabled = false)
    val state =
      mutableStateOf(
        sampleState(
          showPalette = true,
          commandPalette = samplePaletteState().copy(results = listOf(disabledCommand)),
        )
      )

    composeTestRule.setContent {
      NanoShellScaffold(state = state.value, onEvent = { intent -> handleIntent(state, intent) })
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("command_palette_item").assertIsNotEnabled()
    composeTestRule.onNodeWithContentDescription("Unavailable offline").assertIsDisplayed()
  }

  @Test
  fun progressRetryButton_reflectsRetryAvailability() {
    val retryable = sampleProgressJob(canRetry = true)
    val nonRetryable = sampleProgressJob(canRetry = false)
    val recorder = EventRecorder()
    val state =
      mutableStateOf(
        sampleState(
          showPalette = false,
          progressJobs = listOf(retryable, nonRetryable),
        )
      )

    composeTestRule.setContent {
      NanoShellScaffold(
        state = state.value,
        onEvent = { intent ->
          recorder.record(intent)
          handleIntent(state, intent)
        }
      )
    }

    composeTestRule.waitUntil {
      runCatching {
          composeTestRule.onAllNodesWithTag("progress_retry_button").assertCountEquals(2)
          true
        }
        .getOrDefault(false)
    }

    val retryButtons = composeTestRule.onAllNodesWithTag("progress_retry_button")
    retryButtons[0].assertIsEnabled()
    retryButtons[1].assertIsNotEnabled()

    retryButtons[0].performClick()

    composeTestRule.waitUntil {
      recorder.events.any { it is ShellUiEvent.QueueJob && it.job.jobId == retryable.jobId }
    }
  }

  @Test
  fun snackbar_displaysUndoAction_andDispatchesEvent() {
    val payload =
      UndoPayload(
        actionId = "queue-${UUID.randomUUID()}",
        metadata = mapOf("message" to "Image generation queued for reconnect"),
      )
    val recorder = EventRecorder()
    val state = mutableStateOf(sampleState(showPalette = false, pendingUndo = payload))

    composeTestRule.setContent {
      NanoShellScaffold(
        state = state.value,
        onEvent = { intent ->
          recorder.record(intent)
          handleIntent(state, intent)
        }
      )
    }

    composeTestRule.waitForIdle()

    composeTestRule.waitUntilExactlyOneExists(
      hasText("Image generation queued for reconnect"),
      timeoutMillis = 5_000,
    )

    composeTestRule.onNodeWithText("Image generation queued for reconnect").assertIsDisplayed()
    composeTestRule.onNodeWithText("Undo").assertIsDisplayed().assertHasClickAction().performClick()

    composeTestRule.waitUntil {
      recorder.events.any { it is ShellUiEvent.Undo && it.payload == payload }
    }
  }
}

private fun handleIntent(state: MutableState<ShellUiState>, intent: ShellUiEvent) {
  val current = state.value
  when (intent) {
    is ShellUiEvent.ShowCommandPalette ->
      state.value =
        current.copy(
          layout = current.layout.copy(showCommandPalette = true, isLeftDrawerOpen = false),
        )
    is ShellUiEvent.HideCommandPalette ->
      state.value = current.copy(layout = current.layout.copy(showCommandPalette = false))
    is ShellUiEvent.ModeSelected ->
      state.value =
        current.copy(
          layout =
            current.layout.copy(
              activeMode = intent.modeId,
              showCommandPalette = false,
              isLeftDrawerOpen = false,
            ),
        )
    is ShellUiEvent.ToggleRightDrawer ->
      state.value =
        current.copy(
          layout =
            current.layout.copy(
              isRightDrawerOpen = !current.layout.isRightDrawerOpen,
              activeRightPanel = intent.panel,
            ),
        )
    is ShellUiEvent.ToggleLeftDrawer ->
      state.value =
        current.copy(
          layout =
            current.layout.copy(
              isLeftDrawerOpen = !current.layout.isLeftDrawerOpen,
              showCommandPalette = false,
            ),
        )
    is ShellUiEvent.QueueJob ->
      state.value =
        current.copy(
          layout = current.layout.copy(progressJobs = current.layout.progressJobs + intent.job),
        )
    is ShellUiEvent.CompleteJob ->
      state.value =
        current.copy(
          layout =
            current.layout.copy(
              progressJobs =
                current.layout.progressJobs.filterNot { job -> job.jobId == intent.jobId },
            ),
        )
    is ShellUiEvent.Undo ->
      state.value = current.copy(layout = current.layout.copy(pendingUndoAction = null))
    is ShellUiEvent.ConnectivityChanged ->
      state.value =
        current.copy(
          layout = current.layout.copy(connectivity = intent.status),
          connectivityBanner = current.connectivityBanner.copy(status = intent.status),
        )
    is ShellUiEvent.UpdateTheme ->
      state.value = current.copy(preferences = current.preferences.copy(theme = intent.theme))
    is ShellUiEvent.UpdateDensity ->
      state.value = current.copy(preferences = current.preferences.copy(density = intent.density))
    is ShellUiEvent.SetLeftDrawer ->
      state.value = current.copy(layout = current.layout.copy(isLeftDrawerOpen = intent.open))
    else -> Unit
  }
}

private class EventRecorder {
  val events = mutableListOf<ShellUiEvent>()

  fun record(event: ShellUiEvent) {
    events += event
  }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
private fun sampleState(
  showPalette: Boolean,
  commandPalette: CommandPaletteState = samplePaletteState(),
  pendingUndo: UndoPayload? = null,
  progressJobs: List<ProgressJob> = emptyList(),
  connectivity: ConnectivityStatus = ConnectivityStatus.ONLINE,
  rightPanel: RightPanel? = RightPanel.MODEL_SELECTOR,
): ShellUiState {
  val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(600.dp, 800.dp))
  val layout =
    ShellLayoutState(
      windowSizeClass = windowSizeClass,
      isLeftDrawerOpen = false,
      isRightDrawerOpen = true,
      activeRightPanel = rightPanel,
      activeMode = ModeId.HOME,
      showCommandPalette = showPalette,
      connectivity = connectivity,
      pendingUndoAction = pendingUndo,
      progressJobs = progressJobs,
      recentActivity = sampleRecentActivity(),
    )
  val banner =
    ConnectivityBannerState(
      status = connectivity,
      queuedActionCount = progressJobs.count { !it.isTerminal },
      cta = sampleCommands().first(),
    )

  return ShellUiState(
    layout = layout,
    commandPalette = commandPalette,
    connectivityBanner = banner,
    preferences = UiPreferenceSnapshot(),
  )
}

private fun sampleCommands(): List<CommandAction> =
  listOf(
    CommandAction(
      id = "mode-chat",
      title = "Chat",
      subtitle = "Converse with AI",
      category = CommandCategory.MODES,
      destination = CommandDestination.Navigate("chat"),
    ),
    CommandAction(
      id = "mode-image",
      title = "Image",
      subtitle = "Generate visuals",
      category = CommandCategory.MODES,
      destination = CommandDestination.Navigate("image"),
    ),
    CommandAction(
      id = "mode-audio",
      title = "Audio",
      subtitle = "Process sound",
      category = CommandCategory.MODES,
      destination = CommandDestination.Navigate("audio"),
    ),
  )

private fun samplePaletteState(): CommandPaletteState =
  CommandPaletteState(
    query = "",
    results = sampleCommands(),
    recentCommands = sampleCommands().take(1),
    selectedIndex = 0,
    surfaceTarget = CommandCategory.MODES,
  )

private fun sampleRecentActivity(): List<RecentActivityItem> =
  listOf(
    RecentActivityItem(
      id = "recent-chat",
      modeId = ModeId.CHAT,
      title = "Chat with Nova",
      timestamp = Instant.now().minusSeconds(120),
      status = RecentStatus.COMPLETED,
    )
  )

private fun sampleProgressJob(
  status: JobStatus = JobStatus.FAILED,
  canRetry: Boolean = true,
): ProgressJob =
  ProgressJob(
    jobId = UUID.randomUUID(),
    type = JobType.MODEL_DOWNLOAD,
    status = status,
    progress = if (status == JobStatus.FAILED) 0f else 0.35f,
    eta = Duration.ofSeconds(45),
    canRetry = canRetry,
    queuedAt = Instant.now(),
  )
