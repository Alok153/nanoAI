@file:Suppress("CyclomaticComplexMethod")

package com.vjaykrsna.nanoai.feature.uiux

import androidx.activity.ComponentActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
import com.vjaykrsna.nanoai.feature.uiux.state.JobStatus
import com.vjaykrsna.nanoai.feature.uiux.state.JobType
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.state.RecentActivityItem
import com.vjaykrsna.nanoai.feature.uiux.state.RecentStatus
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.state.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.state.UiPreferenceSnapshot
import com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.NanoShellScaffold
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.ShellUiEvent
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(AndroidJUnit4::class)
class OfflineProgressTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun offlineBanner_showsQueuedCount() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.OFFLINE, queuedJobs = 2))
    composeRule.setContent { NanoShellScaffold(state = state.value, onEvent = {}) }

    composeRule.onNodeWithTag("connectivity_banner").assertIsDisplayed()
    composeRule.onNodeWithTag("connectivity_banner_cta").assertIsDisplayed()
  }

  @Test
  fun progressList_displaysQueuedJobs() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.OFFLINE, queuedJobs = 3))
    composeRule.setContent { NanoShellScaffold(state = state.value, onEvent = {}) }

    composeRule.onAllNodesWithTag("progress_list_item").assertCountEquals(3)
  }

  @Test
  fun reconnect_flushesQueue_andHidesBanner() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.OFFLINE, queuedJobs = 1))
    composeRule.setContent {
      NanoShellScaffold(state = state.value, onEvent = { intent -> handleIntent(state, intent) })
    }

    handleIntent(state, ShellUiEvent.ConnectivityChanged(ConnectivityStatus.ONLINE))

    composeRule.onNodeWithTag("connectivity_banner").assertDoesNotExist()
    assertThat(state.value.layout.connectivity).isEqualTo(ConnectivityStatus.ONLINE)
  }

  @Test
  fun retryFailedJob_emitsQueueIntent() {
    val jobId = UUID.randomUUID()
    val failingJob =
      ProgressJob(
        jobId = jobId,
        type = JobType.MODEL_DOWNLOAD,
        status = JobStatus.FAILED,
        progress = 0.2f,
        eta = Duration.ofSeconds(30),
        canRetry = true,
        queuedAt = Instant.now(),
      )
    val state =
      mutableStateOf(
        sampleState(ConnectivityStatus.ONLINE, queuedJobs = 0, jobs = listOf(failingJob))
      )
    val events = mutableListOf<ShellUiEvent>()

    composeRule.setContent {
      NanoShellScaffold(
        state = state.value,
        onEvent = { intent ->
          events += intent
          handleIntent(state, intent)
        }
      )
    }

    composeRule.onNodeWithTag("progress_retry_button").assertIsEnabled().performClick()
    assertThat(events.filterIsInstance<ShellUiEvent.QueueJob>()).isNotEmpty()
  }

  private fun handleIntent(state: MutableState<ShellUiState>, intent: ShellUiEvent) {
    val current = state.value
    when (intent) {
      is ShellUiEvent.ConnectivityChanged ->
        state.value =
          current.copy(
            layout = current.layout.copy(connectivity = intent.status),
            connectivityBanner =
              current.connectivityBanner.copy(status = intent.status, queuedActionCount = 0),
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
      is ShellUiEvent.ModeSelected ->
        state.value = current.copy(layout = current.layout.copy(activeMode = intent.modeId))
      ShellUiEvent.ToggleLeftDrawer ->
        state.value =
          current.copy(
            layout = current.layout.copy(isLeftDrawerOpen = !current.layout.isLeftDrawerOpen)
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
      is ShellUiEvent.ShowCommandPalette ->
        state.value = current.copy(layout = current.layout.copy(showCommandPalette = true))
      ShellUiEvent.HideCommandPalette ->
        state.value = current.copy(layout = current.layout.copy(showCommandPalette = false))
      is ShellUiEvent.Undo ->
        state.value = current.copy(layout = current.layout.copy(pendingUndoAction = null))
      is ShellUiEvent.UpdateTheme ->
        state.value = current.copy(preferences = current.preferences.copy(theme = intent.theme))
      is ShellUiEvent.UpdateDensity ->
        state.value = current.copy(preferences = current.preferences.copy(density = intent.density))
    }
  }

  private fun sampleState(
    connectivity: ConnectivityStatus,
    queuedJobs: Int,
    jobs: List<ProgressJob> =
      List(queuedJobs) { index ->
        ProgressJob(
          jobId = UUID.randomUUID(),
          type = JobType.IMAGE_GENERATION,
          status = JobStatus.PENDING,
          progress = 0f,
          eta = Duration.ofSeconds(60),
          canRetry = true,
          queuedAt = Instant.now().minusSeconds(index.toLong()),
        )
      },
  ): ShellUiState {
    val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(600.dp, 800.dp))
    val layout =
      ShellLayoutState(
        windowSizeClass = windowSizeClass,
        isLeftDrawerOpen = false,
        isRightDrawerOpen = false,
        activeRightPanel = RightPanel.PROGRESS_CENTER,
        activeMode = ModeId.HOME,
        showCommandPalette = false,
        connectivity = connectivity,
        pendingUndoAction = UndoPayload(actionId = "pending"),
        progressJobs = jobs,
        recentActivity = sampleRecentActivity(),
      )
    val banner =
      ConnectivityBannerState(
        status = connectivity,
        queuedActionCount = jobs.size,
        cta =
          CommandAction(
            id = "view-queue",
            title = "View queue (${jobs.size})",
            category = CommandCategory.JOBS,
            destination = CommandDestination.OpenRightPanel(RightPanel.PROGRESS_CENTER),
          ),
      )
    val palette =
      CommandPaletteState(query = "", results = emptyList(), recentCommands = emptyList())
    return ShellUiState(
      layout = layout,
      commandPalette = palette,
      connectivityBanner = banner,
      preferences = UiPreferenceSnapshot(),
    )
  }

  private fun sampleRecentActivity(): List<RecentActivityItem> =
    listOf(
      RecentActivityItem(
        id = "recent1",
        modeId = ModeId.CHAT,
        title = "Chat thread",
        timestamp = Instant.now().minusSeconds(200),
        status = RecentStatus.IN_PROGRESS,
      )
    )
}
