package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.uiux.data.ShellStateRepository
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
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
class ShellViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Test
  fun openMode_closesDrawersAndHidesPalette() = runTest(dispatcher) {
    val repository = FakeShellStateRepository()
    val viewModel = ShellViewModel(repository, dispatcher)

    viewModel.openMode(ModeId.CHAT)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertThat(uiState.layout.activeMode).isEqualTo(ModeId.CHAT)
    assertThat(uiState.layout.isLeftDrawerOpen).isFalse()
    assertThat(uiState.layout.showCommandPalette).isFalse()
    assertThat(repository.openModeCalls).containsExactly(ModeId.CHAT)
  }

  @Test
  fun toggleRightDrawer_setsPanelAndReflectsInState() = runTest(dispatcher) {
    val repository = FakeShellStateRepository()
    val viewModel = ShellViewModel(repository, dispatcher)

    viewModel.toggleRightDrawer(RightPanel.PROGRESS_CENTER)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertThat(uiState.layout.isRightDrawerOpen).isTrue()
    assertThat(uiState.layout.activeRightPanel).isEqualTo(RightPanel.PROGRESS_CENTER)
    assertThat(repository.rightDrawerToggles).containsExactly(RightPanel.PROGRESS_CENTER)
  }

  @Test
  fun queueGeneration_offline_jobQueuedWithPendingUndo() = runTest(dispatcher) {
    val repository = FakeShellStateRepository(initialConnectivity = ConnectivityStatus.OFFLINE)
    val viewModel = ShellViewModel(repository, dispatcher)

    val job = ProgressJob(
      jobId = UUID.randomUUID(),
      type = JobType.IMAGE_GENERATION,
      status = JobStatus.PENDING,
      progress = 0f,
      eta = Duration.ofSeconds(90),
      canRetry = true,
      queuedAt = Instant.parse("2025-10-06T00:00:00Z"),
    )

    viewModel.queueGeneration(job)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertThat(uiState.layout.progressJobs).contains(job)
    assertThat(uiState.layout.pendingUndoAction).isNotNull()
    assertThat(repository.queuedJobs).contains(job)
  }

  @Test
  fun completeJob_removesJobAndClearsUndo() = runTest(dispatcher) {
    val jobId = UUID.randomUUID()
    val repository =
      FakeShellStateRepository(
        initialJobs = listOf(
          ProgressJob(
            jobId = jobId,
            type = JobType.MODEL_DOWNLOAD,
            status = JobStatus.RUNNING,
            progress = 0.5f,
            eta = Duration.ofSeconds(30),
            canRetry = false,
            queuedAt = Instant.parse("2025-10-06T00:00:00Z"),
          )
        ),
      )
    val viewModel = ShellViewModel(repository, dispatcher)

    viewModel.completeJob(jobId)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertThat(uiState.layout.progressJobs).isEmpty()
    assertThat(repository.completedJobs).contains(jobId)
  }

  @Test
  fun updateConnectivity_flushesQueuedJobsAndBanner() = runTest(dispatcher) {
    val repository = FakeShellStateRepository(initialConnectivity = ConnectivityStatus.OFFLINE)
    val viewModel = ShellViewModel(repository, dispatcher)

    viewModel.updateConnectivity(ConnectivityStatus.ONLINE)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertThat(uiState.layout.connectivity).isEqualTo(ConnectivityStatus.ONLINE)
    assertThat(uiState.connectivityBanner.status).isEqualTo(ConnectivityStatus.ONLINE)
    assertThat(repository.connectivityUpdates).containsExactly(ConnectivityStatus.ONLINE)
  }

  private class FakeShellStateRepository(
    initialMode: ModeId = ModeId.HOME,
    initialConnectivity: ConnectivityStatus = ConnectivityStatus.ONLINE,
    initialJobs: List<ProgressJob> = emptyList(),
  ) : ShellStateRepository() {
    private val testWindowSizeClass: WindowSizeClass =
      WindowSizeClass.calculateFromSize(DpSize(width = 600.dp, height = 800.dp))
    private val initialLayout =
      ShellLayoutState(
        windowSizeClass = testWindowSizeClass,
        isLeftDrawerOpen = true,
        isRightDrawerOpen = false,
        activeRightPanel = null,
        activeMode = initialMode,
        showCommandPalette = true,
        connectivity = initialConnectivity,
        pendingUndoAction = null,
        progressJobs = initialJobs,
        recentActivity = emptyList(),
      )
    private val initialPaletteState =
      CommandPaletteState(
        query = "",
        results = emptyList(),
        recentCommands = emptyList(),
        selectedIndex = -1,
        surfaceTarget = CommandCategory.MODES,
      )
    private val initialBanner =
      ConnectivityBannerState(
        status = initialConnectivity,
        queuedActionCount = initialJobs.size,
        cta = CommandAction(
          id = "open-progress",
          title = "View queue",
          category = CommandCategory.JOBS,
          destination = CommandDestination.OpenRightPanel(RightPanel.PROGRESS_CENTER),
        ),
      )
    private val initialPreferences = UiPreferenceSnapshot()

    private val _layout = MutableStateFlow(initialLayout)
    override val shellLayoutState: StateFlow<ShellLayoutState> = _layout
    private val _palette = MutableStateFlow(initialPaletteState)
    override val commandPaletteState: StateFlow<CommandPaletteState> = _palette
    private val _banner = MutableStateFlow(initialBanner)
    override val connectivityBannerState: StateFlow<ConnectivityBannerState> = _banner
    private val _preferences = MutableStateFlow(initialPreferences)
    override val uiPreferenceSnapshot: StateFlow<UiPreferenceSnapshot> = _preferences
    private val _recent = MutableStateFlow<List<RecentActivityItem>>(emptyList())
    override val recentActivity: StateFlow<List<RecentActivityItem>> = _recent

    val openModeCalls = mutableListOf<ModeId>()
    val rightDrawerToggles = mutableListOf<RightPanel>()
    val queuedJobs = mutableListOf<ProgressJob>()
    val completedJobs = mutableListOf<UUID>()
    val connectivityUpdates = mutableListOf<ConnectivityStatus>()

    override suspend fun openMode(modeId: ModeId) {
      openModeCalls += modeId
      _layout.value =
        _layout.value.copy(
          activeMode = modeId,
          isLeftDrawerOpen = false,
          showCommandPalette = false,
        )
    }

    override suspend fun toggleLeftDrawer() {
      _layout.value =
        _layout.value.copy(
          isLeftDrawerOpen = !_layout.value.isLeftDrawerOpen,
          showCommandPalette = false,
        )
    }

    override suspend fun toggleRightDrawer(panel: RightPanel) {
      rightDrawerToggles += panel
      val currentlyOpen = _layout.value.isRightDrawerOpen && _layout.value.activeRightPanel == panel
      _layout.value =
        _layout.value.copy(
          isRightDrawerOpen = !currentlyOpen,
          activeRightPanel = if (currentlyOpen) null else panel,
        )
    }

    override suspend fun showCommandPalette(source: PaletteSource) {
      _palette.value = _palette.value.copy(surfaceTarget = when (source) {
        PaletteSource.KEYBOARD_SHORTCUT -> CommandCategory.MODES
        PaletteSource.TOP_APP_BAR -> CommandCategory.SETTINGS
        PaletteSource.QUICK_ACTION -> CommandCategory.JOBS
        PaletteSource.UNKNOWN -> CommandCategory.MODES
      })
      _layout.value = _layout.value.copy(showCommandPalette = true, isLeftDrawerOpen = false)
    }

    override suspend fun hideCommandPalette() {
      _layout.value = _layout.value.copy(showCommandPalette = false)
    }

    override suspend fun queueJob(job: ProgressJob) {
      queuedJobs += job
      _layout.value = _layout.value.copy(progressJobs = _layout.value.progressJobs + job)
      _layout.value =
        _layout.value.copy(
          pendingUndoAction = UndoPayload(actionId = "queue-${job.jobId}"),
        )
      _banner.value =
        _banner.value.copy(
          queuedActionCount = _layout.value.progressJobs.size,
          status = ConnectivityStatus.OFFLINE,
        )
    }

    override suspend fun completeJob(jobId: UUID) {
      completedJobs += jobId
      _layout.value =
        _layout.value.copy(
          progressJobs = _layout.value.progressJobs.filterNot { it.jobId == jobId },
          pendingUndoAction = null,
        )
      _banner.value = _banner.value.copy(queuedActionCount = _layout.value.progressJobs.size)
    }

    override suspend fun updateConnectivity(status: ConnectivityStatus) {
      connectivityUpdates += status
      _layout.value = _layout.value.copy(connectivity = status)
      _banner.value =
        _banner.value.copy(
          status = status,
          queuedActionCount = _layout.value.progressJobs.size,
        )
    }

    override suspend fun recordUndoPayload(payload: UndoPayload?) {
      _layout.value = _layout.value.copy(pendingUndoAction = payload)
    }
  }
}
