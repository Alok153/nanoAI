package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.library.DownloadManager
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandCategory
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandPaletteState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityBannerState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.DataStoreUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.LayoutSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.PaletteSource
import com.vjaykrsna.nanoai.core.domain.model.uiux.ProgressJob
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentActivityItem
import com.vjaykrsna.nanoai.core.domain.model.uiux.RightPanel
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UndoPayload
import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.core.domain.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.uiux.CommandPaletteActionProvider
import com.vjaykrsna.nanoai.core.domain.uiux.ConnectivityOperationsUseCase
import com.vjaykrsna.nanoai.core.domain.uiux.JobOperationsUseCase
import com.vjaykrsna.nanoai.core.domain.uiux.ProgressCenterCoordinator
import com.vjaykrsna.nanoai.core.domain.uiux.QueueJobUseCase
import com.vjaykrsna.nanoai.core.domain.uiux.ThemeOperationsUseCase
import com.vjaykrsna.nanoai.core.domain.uiux.UndoActionUseCase
import com.vjaykrsna.nanoai.core.domain.uiux.navigation.Screen
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant as KtxInstant

internal class NoopUserProfileRepository : UserProfileRepository {
  private val profileFlow =
    MutableStateFlow<UserProfile?>(
      UserProfile(
        id = "default",
        displayName = "User",
        themePreference = ThemePreference.SYSTEM,
        visualDensity = VisualDensity.DEFAULT,
        lastOpenedScreen = com.vjaykrsna.nanoai.core.domain.model.uiux.ScreenType.HOME,
        compactMode = false,
        pinnedTools = emptyList(),
        savedLayouts = emptyList(),
      )
    )
  private val preferencesFlow = MutableStateFlow(DataStoreUiPreferences())
  private val uiStateFlow = MutableStateFlow<UIStateSnapshot?>(null)
  private val offlineFlow = MutableStateFlow(false)

  override fun observeUserProfile(userId: String): Flow<UserProfile?> = profileFlow

  override fun observeOfflineStatus(): Flow<Boolean> = offlineFlow

  override suspend fun getUserProfile(userId: String): UserProfile? = profileFlow.value

  override fun observePreferences(): Flow<DataStoreUiPreferences> = preferencesFlow

  override suspend fun updateThemePreference(userId: String, themePreferenceName: String) {
    val theme = ThemePreference.valueOf(themePreferenceName)
    preferencesFlow.value = preferencesFlow.value.copy(themePreference = theme)
    profileFlow.value = profileFlow.value?.copy(themePreference = theme)
  }

  override suspend fun updateVisualDensity(userId: String, visualDensityName: String) {
    val density = VisualDensity.valueOf(visualDensityName)
    preferencesFlow.value = preferencesFlow.value.copy(visualDensity = density)
    profileFlow.value = profileFlow.value?.copy(visualDensity = density)
  }

  override suspend fun updateCompactMode(userId: String, enabled: Boolean) = Unit

  override suspend fun updatePinnedTools(userId: String, pinnedTools: List<String>) = Unit

  override suspend fun saveLayoutSnapshot(userId: String, layout: LayoutSnapshot, position: Int) =
    Unit

  override suspend fun deleteLayoutSnapshot(layoutId: String) = Unit

  override fun observeUIStateSnapshot(userId: String): Flow<UIStateSnapshot?> = uiStateFlow

  override suspend fun updateLeftDrawerOpen(userId: String, open: Boolean) {
    val current =
      uiStateFlow.value
        ?: UIStateSnapshot(
          userId = userId,
          expandedPanels = emptyList(),
          recentActions = emptyList(),
          isSidebarCollapsed = false,
        )
    uiStateFlow.value = current.copy(isLeftDrawerOpen = open)
  }

  override suspend fun updateRightDrawerState(userId: String, open: Boolean, panel: String?) {
    val current =
      uiStateFlow.value
        ?: UIStateSnapshot(
          userId = userId,
          expandedPanels = emptyList(),
          recentActions = emptyList(),
          isSidebarCollapsed = false,
        )
    uiStateFlow.value = current.copy(isRightDrawerOpen = open, activeRightPanel = panel)
  }

  override suspend fun updateActiveModeRoute(userId: String, route: String) {
    val current =
      uiStateFlow.value
        ?: UIStateSnapshot(
          userId = userId,
          expandedPanels = emptyList(),
          recentActions = emptyList(),
          isSidebarCollapsed = false,
        )
    uiStateFlow.value = current.copy(activeModeRoute = route)
  }

  override suspend fun updateCommandPaletteVisibility(userId: String, visible: Boolean) {
    val current =
      uiStateFlow.value
        ?: UIStateSnapshot(
          userId = userId,
          expandedPanels = emptyList(),
          recentActions = emptyList(),
          isSidebarCollapsed = false,
        )
    uiStateFlow.value = current.copy(isCommandPaletteVisible = visible)
  }

  override suspend fun recordCommandPaletteRecent(commandId: String) = Unit

  override suspend fun setCommandPaletteRecents(commandIds: List<String>) = Unit

  override suspend fun setConnectivityBannerDismissed(dismissedAt: KtxInstant?) = Unit

  override suspend fun setOfflineOverride(isOffline: Boolean) {
    offlineFlow.value = isOffline
  }
}

internal class FakeNavigationRepository(
  private val userProfileRepository:
    com.vjaykrsna.nanoai.core.domain.repository.UserProfileRepository
) : com.vjaykrsna.nanoai.core.domain.repository.NavigationRepository {
  override val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher =
    kotlinx.coroutines.Dispatchers.Unconfined

  private val commandPaletteStateFlow =
    kotlinx.coroutines.flow.MutableStateFlow(CommandPaletteState())
  override val commandPaletteState: kotlinx.coroutines.flow.Flow<CommandPaletteState> =
    commandPaletteStateFlow

  private val recentActivityFlow =
    kotlinx.coroutines.flow.MutableStateFlow<List<RecentActivityItem>>(emptyList())
  override val recentActivity: kotlinx.coroutines.flow.Flow<List<RecentActivityItem>> =
    recentActivityFlow

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  private val windowSizeClassFlow =
    kotlinx.coroutines.flow.MutableStateFlow(
      WindowSizeClass.calculateFromSize(DpSize(800.dp, 600.dp))
    )
  override val windowSizeClass: kotlinx.coroutines.flow.Flow<WindowSizeClass> = windowSizeClassFlow

  internal val undoPayloadFlow =
    kotlinx.coroutines.flow.MutableStateFlow<
      com.vjaykrsna.nanoai.core.domain.model.uiux.UndoPayload?
    >(
      null
    )
  override val undoPayload:
    kotlinx.coroutines.flow.Flow<com.vjaykrsna.nanoai.core.domain.model.uiux.UndoPayload?> =
    undoPayloadFlow

  override fun updateWindowSizeClass(sizeClass: WindowSizeClass) {
    windowSizeClassFlow.value = sizeClass
  }

  fun emitRecentActivity(items: List<RecentActivityItem>) {
    recentActivityFlow.value = items
  }

  fun emitUndoPayload(payload: UndoPayload?) {
    undoPayloadFlow.value = payload
  }

  override suspend fun openMode(modeId: ModeId) {
    val route = Screen.fromModeId(modeId).route
    userProfileRepository.updateActiveModeRoute("default", route)
    userProfileRepository.updateLeftDrawerOpen("default", false)
    userProfileRepository.updateCommandPaletteVisibility("default", false)
  }

  override suspend fun toggleLeftDrawer() = Unit

  override suspend fun setLeftDrawer(open: Boolean) = Unit

  override suspend fun toggleRightDrawer(panel: RightPanel) {
    userProfileRepository.updateRightDrawerState("default", true, panel.name)
  }

  override suspend fun showCommandPalette(
    source: com.vjaykrsna.nanoai.core.domain.model.uiux.PaletteSource
  ) {
    commandPaletteStateFlow.value = CommandPaletteState(surfaceTarget = CommandCategory.MODES)
  }

  override suspend fun hideCommandPalette() {
    commandPaletteStateFlow.value = CommandPaletteState()
  }

  override suspend fun recordUndoPayload(
    payload: com.vjaykrsna.nanoai.core.domain.model.uiux.UndoPayload?
  ) {
    undoPayloadFlow.value = payload
  }
}

internal class FakeConnectivityRepository(
  private val userProfileRepository: UserProfileRepository? = null
) : com.vjaykrsna.nanoai.core.domain.repository.ConnectivityRepository {
  override val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher =
    kotlinx.coroutines.Dispatchers.Unconfined

  private val connectivityBannerStateFlow =
    kotlinx.coroutines.flow.MutableStateFlow(
      ConnectivityBannerState(status = ConnectivityStatus.ONLINE)
    )
  override val connectivityBannerState: kotlinx.coroutines.flow.Flow<ConnectivityBannerState> =
    connectivityBannerStateFlow

  override suspend fun updateConnectivity(status: ConnectivityStatus) {
    connectivityBannerStateFlow.value = ConnectivityBannerState(status = status)
    userProfileRepository?.setOfflineOverride(status == ConnectivityStatus.OFFLINE)
  }
}

internal class FakeThemeRepository(
  private val userProfileRepository: UserProfileRepository? = null
) : com.vjaykrsna.nanoai.core.domain.repository.ThemeRepository {
  override val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher =
    kotlinx.coroutines.Dispatchers.Unconfined

  private val shellUiPreferencesFlow =
    kotlinx.coroutines.flow.MutableStateFlow(ShellUiPreferences())
  override val shellUiPreferences: kotlinx.coroutines.flow.Flow<ShellUiPreferences> =
    shellUiPreferencesFlow

  override suspend fun updateThemePreference(theme: ThemePreference) {
    shellUiPreferencesFlow.value = shellUiPreferencesFlow.value.copy(theme = theme)
    userProfileRepository?.updateThemePreference("default", theme.name)
  }

  override suspend fun updateVisualDensity(density: VisualDensity) {
    shellUiPreferencesFlow.value = shellUiPreferencesFlow.value.copy(density = density)
    userProfileRepository?.updateVisualDensity("default", density.name)
  }

  override suspend fun updateHighContrastEnabled(enabled: Boolean) {
    shellUiPreferencesFlow.value = shellUiPreferencesFlow.value.copy(highContrastEnabled = enabled)
  }
}

internal class FakeProgressRepository :
  com.vjaykrsna.nanoai.core.domain.repository.ProgressRepository {
  override val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher =
    kotlinx.coroutines.Dispatchers.Unconfined

  private val progressJobsFlow =
    kotlinx.coroutines.flow.MutableStateFlow<List<ProgressJob>>(emptyList())
  override val progressJobs: kotlinx.coroutines.flow.Flow<List<ProgressJob>> = progressJobsFlow

  override suspend fun queueJob(job: ProgressJob) {
    progressJobsFlow.value = progressJobsFlow.value + job
  }

  override suspend fun completeJob(jobId: java.util.UUID) {
    progressJobsFlow.value = progressJobsFlow.value.filterNot { it.jobId == jobId }
  }
}

internal fun createFakeCommandPaletteActionProvider(): CommandPaletteActionProvider =
  CommandPaletteActionProvider()

internal fun createFakeProgressCenterCoordinator(
  progressRepository: com.vjaykrsna.nanoai.core.domain.repository.ProgressRepository =
    FakeProgressRepository(),
  downloadManager: DownloadManager = FakeDownloadManager(),
  dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
): ProgressCenterCoordinator =
  ProgressCenterCoordinator(downloadManager = downloadManager, progressRepository, dispatcher)

internal fun createProgressCoordinator(
  repositories: FakeRepositories,
  dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
): ProgressCoordinator {
  val coordinator =
    createFakeProgressCenterCoordinator(
      progressRepository = repositories.progressRepository,
      dispatcher = dispatcher,
    )
  val queueJobUseCase =
    QueueJobUseCase(
      repositories.progressRepository,
      repositories.connectivityRepository,
      repositories.navigationRepository,
      dispatcher,
    )
  val jobOperationsUseCase =
    JobOperationsUseCase(
      repositories.progressRepository,
      repositories.navigationRepository,
      coordinator,
      dispatcher,
    )
  val undoActionUseCase =
    UndoActionUseCase(
      repositories.progressRepository,
      repositories.navigationRepository,
      dispatcher,
    )
  return ProgressCoordinator(
    coordinator,
    queueJobUseCase,
    jobOperationsUseCase,
    undoActionUseCase,
    dispatcher,
  )
}

internal fun createConnectivityCoordinator(
  repositories: FakeRepositories,
  dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
): ConnectivityCoordinator {
  val useCase = ConnectivityOperationsUseCase(repositories.connectivityRepository, dispatcher)
  return ConnectivityCoordinator(useCase, dispatcher)
}

internal fun createThemeCoordinator(
  repositories: FakeRepositories,
  dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
): ThemeCoordinator {
  val useCase = ThemeOperationsUseCase(repositories.themeRepository)
  return ThemeCoordinator(useCase, dispatcher)
}

internal class FakeDownloadManager : DownloadManager {
  override suspend fun startDownload(modelId: String): UUID = UUID.randomUUID()

  override suspend fun queueDownload(modelId: String): UUID = UUID.randomUUID()

  override suspend fun pauseDownload(taskId: UUID) = Unit

  override suspend fun resumeDownload(taskId: UUID) = Unit

  override suspend fun cancelDownload(taskId: UUID) = Unit

  override suspend fun retryDownload(taskId: UUID) = Unit

  override suspend fun resetTask(taskId: UUID) = Unit

  override suspend fun getDownloadStatus(taskId: UUID): DownloadTask? = null

  override suspend fun getTaskById(taskId: UUID): Flow<DownloadTask?> = flowOf(null)

  override suspend fun getActiveDownloads(): Flow<List<DownloadTask>> = flowOf(emptyList())

  override fun getQueuedDownloads(): Flow<List<DownloadTask>> = flowOf(emptyList())

  override fun observeManagedDownloads(): Flow<List<DownloadTask>> = flowOf(emptyList())

  override fun observeProgress(taskId: UUID): Flow<Float> = flowOf(0f)

  override suspend fun getMaxConcurrentDownloads(): Int = 2

  override suspend fun updateTaskStatus(taskId: UUID, status: DownloadStatus) = Unit

  override suspend fun getModelIdForTask(taskId: UUID): String? = null

  override suspend fun getDownloadedChecksum(modelId: String): String? = null

  override suspend fun deletePartialFiles(modelId: String) = Unit

  override suspend fun getActiveDownloadsSnapshot(): List<DownloadTask> = emptyList()
}

internal fun createFakeRepositories(): FakeRepositories {
  val userProfileRepository = NoopUserProfileRepository()
  return FakeRepositories(
    navigationRepository = FakeNavigationRepository(userProfileRepository),
    connectivityRepository = FakeConnectivityRepository(userProfileRepository),
    themeRepository = FakeThemeRepository(userProfileRepository),
    progressRepository = FakeProgressRepository(),
    userProfileRepository = userProfileRepository,
  )
}

internal data class FakeRepositories(
  val navigationRepository: com.vjaykrsna.nanoai.core.domain.repository.NavigationRepository,
  val connectivityRepository: com.vjaykrsna.nanoai.core.domain.repository.ConnectivityRepository,
  val themeRepository: com.vjaykrsna.nanoai.core.domain.repository.ThemeRepository,
  val progressRepository: com.vjaykrsna.nanoai.core.domain.repository.ProgressRepository,
  val userProfileRepository: com.vjaykrsna.nanoai.core.domain.repository.UserProfileRepository,
)
