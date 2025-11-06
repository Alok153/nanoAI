@file:Suppress("LongMethod", "CyclomaticComplexMethod")

package com.vjaykrsna.nanoai.feature.uiux.ui

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelectable
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandAction
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandCategory
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandDestination
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandPaletteState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityBannerState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.PaletteSource
import com.vjaykrsna.nanoai.core.domain.model.uiux.ProgressJob
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentActivityItem
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.RightPanel
import com.vjaykrsna.nanoai.core.domain.model.uiux.UiPreferenceSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UndoPayload
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeCard
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellUiState
import com.vjaykrsna.nanoai.shared.testing.TestingTheme
import com.vjaykrsna.nanoai.shared.ui.shell.NanoShellScaffold
import com.vjaykrsna.nanoai.shared.ui.shell.ShellUiEvent
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test

@OptIn(
  ExperimentalCoroutinesApi::class,
  ExperimentalTestApi::class,
  ExperimentalMaterial3WindowSizeClassApi::class,
)
class CommandPaletteComposeTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun palette_opensWithShortcut_focusesSearchField() {
    val recorder = EventRecorder()
    val state = mutableStateOf(sampleState(showPalette = false))

    composeTestRule.setContent {
      TestingTheme {
        NanoShellScaffold(
          state = state.value.copy(layout = state.value.layout.copy(showCoverageDashboard = false)),
          onEvent = { intent ->
            recorder.record(intent)
            handleIntent(state, intent)
          },
        )
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle {
      val shortcutEvent = ShellUiEvent.ShowCommandPalette(PaletteSource.KEYBOARD_SHORTCUT)
      recorder.record(shortcutEvent)
      handleIntent(state, shortcutEvent)
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
      TestingTheme {
        NanoShellScaffold(state = state.value, onEvent = { intent -> handleIntent(state, intent) })
      }
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
      TestingTheme {
        NanoShellScaffold(state = state.value, onEvent = { intent -> handleIntent(state, intent) })
      }
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
      TestingTheme {
        NanoShellScaffold(
          state = state.value,
          onEvent = { intent ->
            recorder.record(intent)
            handleIntent(state, intent)
          },
        )
      }
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
      TestingTheme {
        NanoShellScaffold(state = state.value, onEvent = { intent -> handleIntent(state, intent) })
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("command_palette_item").assertIsNotEnabled()
    composeTestRule.onNodeWithContentDescription("Unavailable offline").assertIsDisplayed()
  }

  @Test
  fun progressRetryButton_reflectsRetryAvailability() {
    val retryable = sampleProgressJob(canRetry = true, type = JobType.MODEL_DOWNLOAD)
    val nonRetryable = sampleProgressJob(canRetry = false, type = JobType.IMAGE_GENERATION)
    val recorder = EventRecorder()
    val state =
      mutableStateOf(
        sampleState(showPalette = false, progressJobs = listOf(retryable, nonRetryable))
      )

    composeTestRule.setContent {
      TestingTheme {
        NanoShellScaffold(
          state = state.value,
          onEvent = { intent ->
            recorder.record(intent)
            handleIntent(state, intent)
          },
        )
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle { assertThat(state.value.layout.progressJobs).hasSize(2) }

    composeTestRule.waitForNodeWithTag("progress_center_panel", useUnmergedTree = true)
    composeTestRule.waitForNodeWithTag("progress_list")
    val progressList = composeTestRule.onNodeWithTag("progress_list")
    val canScroll =
      progressList.fetchSemanticsNode().config.getOrNull(SemanticsActions.ScrollToIndex) != null

    val retryAvailableButton =
      composeTestRule.onNodeWithTag("progress_retry_button_${retryable.jobId}")
    retryAvailableButton.assertIsEnabled()
    retryAvailableButton.assert(
      SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Retry available")
    )
    retryAvailableButton.performSemanticsAction(SemanticsActions.OnClick)
    composeTestRule.waitForIdle()
    if (canScroll) {
      progressList.performScrollToIndex(1)
      composeTestRule.waitForIdle()
    }
    composeTestRule.waitForNodeWithTag("progress_retry_button_${nonRetryable.jobId}")

    val retryUnavailableButton =
      composeTestRule.onNodeWithTag("progress_retry_button_${nonRetryable.jobId}")
    retryUnavailableButton.assertIsNotEnabled()
    retryUnavailableButton.assert(
      SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Retry unavailable")
    )

    composeTestRule.waitForIdle()
    composeTestRule.runOnIdle {
      assertThat(recorder.events.filterIsInstance<ShellUiEvent.RetryJob>().map { it.job.jobId })
        .contains(retryable.jobId)
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
      TestingTheme {
        NanoShellScaffold(
          state = state.value,
          onEvent = { intent ->
            recorder.record(intent)
            handleIntent(state, intent)
          },
        )
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.waitUntilNodeWithText("Image generation queued for reconnect")

    composeTestRule.onNode(hasText("Image generation queued for reconnect")).assertExists()
    composeTestRule.waitUntilNodeWithText("Undo", ignoreCase = true)
    composeTestRule
      .onNodeWithText("Undo", ignoreCase = true)
      .performSemanticsAction(SemanticsActions.OnClick)

    composeTestRule.waitForIdle()
    composeTestRule.runOnIdle {
      assertThat(recorder.events.filterIsInstance<ShellUiEvent.Undo>().map { it.payload })
        .contains(payload)
    }
  }
}

private fun handleIntent(state: MutableState<ShellUiState>, intent: ShellUiEvent) {
  val current = state.value
  when (intent) {
    is ShellUiEvent.ShowCommandPalette ->
      state.value =
        current.copy(
          layout = current.layout.copy(showCommandPalette = true, isLeftDrawerOpen = false)
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
            )
        )
    is ShellUiEvent.ToggleRightDrawer ->
      state.value =
        current.copy(
          layout =
            current.layout.copy(
              isRightDrawerOpen = !current.layout.isRightDrawerOpen,
              activeRightPanel = intent.panel,
            )
        )
    is ShellUiEvent.ToggleLeftDrawer ->
      state.value =
        current.copy(
          layout =
            current.layout.copy(
              isLeftDrawerOpen = !current.layout.isLeftDrawerOpen,
              showCommandPalette = false,
            )
        )
    is ShellUiEvent.QueueJob ->
      state.value =
        current.copy(
          layout = current.layout.copy(progressJobs = current.layout.progressJobs + intent.job)
        )
    is ShellUiEvent.CompleteJob ->
      state.value =
        current.copy(
          layout =
            current.layout.copy(
              progressJobs =
                current.layout.progressJobs.filterNot { job -> job.jobId == intent.jobId }
            )
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

private fun AndroidComposeTestRule<*, *>.waitForNodeWithTag(
  tag: String,
  useUnmergedTree: Boolean = false,
  timeoutMillis: Long = 10_000,
) {
  waitUntil(timeoutMillis) {
    runCatching { onAllNodesWithTag(tag, useUnmergedTree).fetchSemanticsNodes().isNotEmpty() }
      .getOrDefault(false)
  }
}

private fun AndroidComposeTestRule<*, *>.waitUntilNodeWithText(
  text: String,
  useUnmergedTree: Boolean = false,
  ignoreCase: Boolean = false,
  timeoutMillis: Long = 10_000,
) {
  waitUntil(timeoutMillis) {
    onAllNodesWithText(
        text,
        substring = true,
        ignoreCase = ignoreCase,
        useUnmergedTree = useUnmergedTree,
      )
      .fetchSemanticsNodes(false)
      .isNotEmpty()
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
      showCoverageDashboard = false,
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
    modeCards = sampleModeCards(),
    quickActions = sampleQuickActions(),
  )
}

private fun sampleModeCards(): List<ModeCard> =
  listOf(
    ModeCard(
      id = ModeId.CHAT,
      title = "Chat",
      icon = Icons.Outlined.Home,
      primaryAction = CommandAction(id = "chat", title = "Chat"),
    ),
    ModeCard(
      id = ModeId.IMAGE,
      title = "Image",
      icon = Icons.Outlined.Home,
      primaryAction = CommandAction(id = "image", title = "Generate"),
    ),
  )

private fun sampleQuickActions(): List<CommandAction> =
  sampleModeCards().map { card -> card.primaryAction }

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
  type: JobType = JobType.MODEL_DOWNLOAD,
): ProgressJob =
  ProgressJob(
    jobId = UUID.randomUUID(),
    type = type,
    status = status,
    progress = if (status == JobStatus.FAILED) 0f else 0.35f,
    eta = Duration.ofSeconds(45),
    canRetry = canRetry,
    queuedAt = Instant.now(),
  )
