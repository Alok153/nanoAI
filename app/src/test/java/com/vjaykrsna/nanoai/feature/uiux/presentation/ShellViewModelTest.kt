package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.model.uiux.LayoutSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UiPreferencesSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
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
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.state.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.state.UiPreferenceSnapshot
import com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
class ShellViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun openMode_closesDrawersAndHidesPalette() =
    runTest(dispatcher) {
      val repository = FakeShellStateRepository()
      val actionProvider = FakeCommandPaletteActionProvider()
      val progressCoordinator = FakeProgressCenterCoordinator()
      val viewModel =
        ShellViewModel(
          repository,
          actionProvider,
          progressCoordinator,
          dispatcher,
        )

      viewModel.openMode(ModeId.CHAT)
      advanceUntilIdle()

      val uiState =
        viewModel.uiState.first { state ->
          repository.openModeCalls.isNotEmpty() && state.layout.activeMode == ModeId.CHAT
        }
      assertThat(uiState.layout.activeMode).isEqualTo(ModeId.CHAT)
      assertThat(uiState.layout.isLeftDrawerOpen).isFalse()
      assertThat(uiState.layout.showCommandPalette).isFalse()
      assertThat(repository.openModeCalls).containsExactly(ModeId.CHAT)
    }

  @Test
  fun toggleRightDrawer_setsPanelAndReflectsInState() =
    runTest(dispatcher) {
      val repository = FakeShellStateRepository()
      val actionProvider = FakeCommandPaletteActionProvider()
      val progressCoordinator = FakeProgressCenterCoordinator()
      val viewModel =
        ShellViewModel(
          repository,
          actionProvider,
          progressCoordinator,
          dispatcher,
        )

      viewModel.toggleRightDrawer(RightPanel.MODEL_SELECTOR)
      advanceUntilIdle()

      val uiState =
        viewModel.uiState.first { state ->
          repository.rightDrawerToggles.contains(RightPanel.MODEL_SELECTOR) &&
            state.layout.activeRightPanel == RightPanel.MODEL_SELECTOR
        }
      assertThat(uiState.layout.isRightDrawerOpen).isTrue()
      assertThat(uiState.layout.activeRightPanel).isEqualTo(RightPanel.MODEL_SELECTOR)
      assertThat(repository.rightDrawerToggles).containsExactly(RightPanel.MODEL_SELECTOR)
    }

  @Test
  fun queueGeneration_offline_jobQueuedWithPendingUndo() =
    runTest(dispatcher) {
      val repository = FakeShellStateRepository(initialConnectivity = ConnectivityStatus.OFFLINE)
      val actionProvider = FakeCommandPaletteActionProvider()
      val progressCoordinator = FakeProgressCenterCoordinator()
      val viewModel =
        ShellViewModel(
          repository,
          actionProvider,
          progressCoordinator,
          dispatcher,
        )

      val jobId = UUID.randomUUID()
      val job =
        ProgressJob(
          jobId = jobId,
          type = JobType.IMAGE_GENERATION,
          status = JobStatus.PENDING,
          progress = 0f,
          eta = Duration.ofSeconds(90),
          canRetry = true,
          queuedAt = Instant.parse("2025-10-06T00:00:00Z"),
        )

      viewModel.queueGeneration(job)
      advanceUntilIdle()

      val layout = repository.layoutSnapshot
      assertThat(layout.progressJobs.map { it.jobId }).contains(jobId)
      val undoPayload = layout.pendingUndoAction
      assertThat(undoPayload).isNotNull()
      val message = undoPayload?.metadata?.get("message") as? String
      assertThat(message).isEqualTo("Image generation queued for reconnect")
      assertThat(repository.queuedJobs.map { it.jobId }).contains(jobId)
    }

  @Test
  fun queueGeneration_retryableFailure_setsRetryMessage() =
    runTest(dispatcher) {
      val repository = FakeShellStateRepository()
      val actionProvider = FakeCommandPaletteActionProvider()
      val progressCoordinator = FakeProgressCenterCoordinator()
      val viewModel =
        ShellViewModel(
          repository,
          actionProvider,
          progressCoordinator,
          dispatcher,
        )

      val jobId = UUID.randomUUID()
      val job =
        ProgressJob(
          jobId = jobId,
          type = JobType.MODEL_DOWNLOAD,
          status = JobStatus.FAILED,
          progress = 0f,
          eta = Duration.ofSeconds(60),
          canRetry = true,
          queuedAt = Instant.parse("2025-10-06T01:00:00Z"),
        )

      viewModel.queueGeneration(job)
      advanceUntilIdle()

      val layout = repository.layoutSnapshot
      val message = layout.pendingUndoAction?.metadata?.get("message") as? String
      assertThat(message).isEqualTo("Model download retry scheduled")
      assertThat(layout.progressJobs.map { it.jobId }).contains(jobId)
    }

  @Test
  fun undoAction_clearsPendingJobAndUndoPayload() =
    runTest(dispatcher) {
      val repository = FakeShellStateRepository(initialConnectivity = ConnectivityStatus.OFFLINE)
      val actionProvider = FakeCommandPaletteActionProvider()
      val progressCoordinator = FakeProgressCenterCoordinator()
      val viewModel =
        ShellViewModel(
          repository,
          actionProvider,
          progressCoordinator,
          dispatcher,
        )

      val jobId = UUID.randomUUID()
      val job =
        ProgressJob(
          jobId = jobId,
          type = JobType.IMAGE_GENERATION,
          status = JobStatus.PENDING,
          progress = 0f,
          eta = Duration.ofSeconds(120),
          canRetry = true,
          queuedAt = Instant.parse("2025-10-06T02:00:00Z"),
        )

      viewModel.queueGeneration(job)
      advanceUntilIdle()

      val queuedLayout = repository.layoutSnapshot
      val payload = requireNotNull(queuedLayout.pendingUndoAction)

      viewModel.undoAction(payload)
      advanceUntilIdle()

      val clearedLayout = repository.layoutSnapshot
      assertThat(clearedLayout.pendingUndoAction).isNull()
      assertThat(clearedLayout.progressJobs).isEmpty()
      assertThat(repository.completedJobs).contains(jobId)
    }

  @Test
  fun completeJob_removesJobAndClearsUndo() =
    runTest(dispatcher) {
      val jobId = UUID.randomUUID()
      val repository =
        FakeShellStateRepository(
          initialJobs =
            listOf(
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
      val actionProvider = FakeCommandPaletteActionProvider()
      val progressCoordinator = FakeProgressCenterCoordinator()
      val viewModel =
        ShellViewModel(
          repository,
          actionProvider,
          progressCoordinator,
          dispatcher,
        )

      viewModel.completeJob(jobId)
      advanceUntilIdle()

      val uiState =
        viewModel.uiState.first { state ->
          repository.completedJobs.contains(jobId) &&
            state.layout.progressJobs.none { it.jobId == jobId }
        }
      assertThat(uiState.layout.progressJobs).isEmpty()
      assertThat(repository.completedJobs).contains(jobId)
    }

  @Test
  fun updateConnectivity_flushesQueuedJobsAndBanner() =
    runTest(dispatcher) {
      val repository = FakeShellStateRepository(initialConnectivity = ConnectivityStatus.OFFLINE)
      val actionProvider = FakeCommandPaletteActionProvider()
      val progressCoordinator = FakeProgressCenterCoordinator()
      val viewModel =
        ShellViewModel(
          repository,
          actionProvider,
          progressCoordinator,
          dispatcher,
        )

      viewModel.updateConnectivity(ConnectivityStatus.ONLINE)
      advanceUntilIdle()

      val uiState =
        viewModel.uiState.first { state ->
          repository.connectivityUpdates.contains(ConnectivityStatus.ONLINE) &&
            state.layout.connectivity == ConnectivityStatus.ONLINE
        }
      assertThat(uiState.layout.connectivity).isEqualTo(ConnectivityStatus.ONLINE)
      assertThat(uiState.connectivityBanner.status).isEqualTo(ConnectivityStatus.ONLINE)
      assertThat(repository.connectivityUpdates).containsExactly(ConnectivityStatus.ONLINE)
    }

  private class FakeShellStateRepository(
    initialMode: ModeId = ModeId.HOME,
    initialConnectivity: ConnectivityStatus = ConnectivityStatus.ONLINE,
    initialJobs: List<ProgressJob> = emptyList(),
  ) : ShellStateRepository(NoopUserProfileRepository()) {
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
        cta =
          CommandAction(
            id = "open-progress",
            title = "View queue",
            category = CommandCategory.JOBS,
            destination = CommandDestination.OpenRightPanel(RightPanel.MODEL_SELECTOR),
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

    val layoutSnapshot: ShellLayoutState
      get() = _layout.value

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
      _palette.value =
        _palette.value.copy(
          surfaceTarget =
            when (source) {
              PaletteSource.KEYBOARD_SHORTCUT -> CommandCategory.MODES
              PaletteSource.TOP_APP_BAR -> CommandCategory.SETTINGS
              PaletteSource.QUICK_ACTION -> CommandCategory.JOBS
              PaletteSource.UNKNOWN -> CommandCategory.MODES
            }
        )
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

private class NoopUserProfileRepository : UserProfileRepository {
  private val profileFlow = MutableStateFlow<UserProfile?>(null)
  private val preferencesFlow = MutableStateFlow(UiPreferencesSnapshot())
  private val uiStateFlow = MutableStateFlow<UIStateSnapshot?>(null)
  private val offlineFlow = MutableStateFlow(false)

  override fun observeUserProfile(userId: String): Flow<UserProfile?> = profileFlow

  override fun observeOfflineStatus(): Flow<Boolean> = offlineFlow

  override suspend fun getUserProfile(userId: String): UserProfile? = null

  override fun observePreferences(): Flow<UiPreferencesSnapshot> = preferencesFlow

  override suspend fun updateThemePreference(userId: String, themePreferenceName: String) = Unit

  override suspend fun updateVisualDensity(userId: String, visualDensityName: String) = Unit

  override suspend fun updateCompactMode(userId: String, enabled: Boolean) = Unit

  override suspend fun updatePinnedTools(userId: String, pinnedTools: List<String>) = Unit

  override suspend fun saveLayoutSnapshot(userId: String, layout: LayoutSnapshot, position: Int) =
    Unit

  override suspend fun deleteLayoutSnapshot(layoutId: String) = Unit

  override fun observeUIStateSnapshot(userId: String): Flow<UIStateSnapshot?> = uiStateFlow

  override suspend fun updateLeftDrawerOpen(userId: String, open: Boolean) = Unit

  override suspend fun updateRightDrawerState(userId: String, open: Boolean, panel: String?) = Unit

  override suspend fun updateActiveModeRoute(userId: String, route: String) = Unit

  override suspend fun updateCommandPaletteVisibility(userId: String, visible: Boolean) = Unit

  override suspend fun recordCommandPaletteRecent(commandId: String) = Unit

  override suspend fun setCommandPaletteRecents(commandIds: List<String>) = Unit

  override suspend fun setConnectivityBannerDismissed(dismissedAt: kotlinx.datetime.Instant?) = Unit

  override suspend fun setOfflineOverride(isOffline: Boolean) {
    offlineFlow.value = isOffline
  }
}

private fun FakeCommandPaletteActionProvider():
  com.vjaykrsna.nanoai.feature.uiux.domain.CommandPaletteActionProvider =
  com.vjaykrsna.nanoai.feature.uiux.domain.CommandPaletteActionProvider()

private fun FakeProgressCenterCoordinator():
  com.vjaykrsna.nanoai.feature.uiux.domain.ProgressCenterCoordinator {
  val downloadManager = FakeDownloadManager()
  return com.vjaykrsna.nanoai.feature.uiux.domain.ProgressCenterCoordinator(
    downloadManager = downloadManager,
  )
}

private class FakeDownloadManager : com.vjaykrsna.nanoai.feature.library.data.DownloadManager {
  override suspend fun startDownload(modelId: String): UUID = UUID.randomUUID()

  override suspend fun queueDownload(modelId: String): UUID = UUID.randomUUID()

  override suspend fun pauseDownload(taskId: UUID) = Unit

  override suspend fun resumeDownload(taskId: UUID) = Unit

  override suspend fun cancelDownload(taskId: UUID) = Unit

  override suspend fun retryDownload(taskId: UUID) = Unit

  override suspend fun resetTask(taskId: UUID) = Unit

  override suspend fun getDownloadStatus(
    taskId: UUID
  ): com.vjaykrsna.nanoai.core.domain.model.DownloadTask? = null

  override suspend fun getTaskById(
    taskId: UUID
  ): Flow<com.vjaykrsna.nanoai.core.domain.model.DownloadTask?> =
    kotlinx.coroutines.flow.flowOf(null)

  override suspend fun getActiveDownloads():
    Flow<List<com.vjaykrsna.nanoai.core.domain.model.DownloadTask>> =
    kotlinx.coroutines.flow.flowOf(emptyList())

  override fun getQueuedDownloads():
    Flow<List<com.vjaykrsna.nanoai.core.domain.model.DownloadTask>> =
    kotlinx.coroutines.flow.flowOf(emptyList())

  override fun observeManagedDownloads():
    Flow<List<com.vjaykrsna.nanoai.core.domain.model.DownloadTask>> =
    kotlinx.coroutines.flow.flowOf(emptyList())

  override fun observeProgress(taskId: UUID): Flow<Float> = kotlinx.coroutines.flow.flowOf(0f)

  override suspend fun getMaxConcurrentDownloads(): Int = 2

  override suspend fun updateTaskStatus(
    taskId: UUID,
    status: com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
  ) = Unit

  override suspend fun getModelIdForTask(taskId: UUID): String? = null

  override suspend fun getDownloadedChecksum(modelId: String): String? = null

  override suspend fun deletePartialFiles(modelId: String) = Unit
}
