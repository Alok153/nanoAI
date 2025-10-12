package com.vjaykrsna.nanoai.feature.uiux.data

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UiPreferencesSnapshot as DomainUiPreferencesSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.feature.uiux.domain.UIUX_DEFAULT_USER_ID
import com.vjaykrsna.nanoai.feature.uiux.state.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.state.CommandCategory
import com.vjaykrsna.nanoai.feature.uiux.state.CommandDestination
import com.vjaykrsna.nanoai.feature.uiux.state.CommandPaletteState
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityBannerState
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.state.RecentActivityItem
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.state.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.state.UiPreferenceSnapshot
import com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload
import com.vjaykrsna.nanoai.feature.uiux.state.toModeIdOrDefault
import com.vjaykrsna.nanoai.feature.uiux.state.toRoute
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.toJavaInstant

/** Repository surface that coordinates shell state persistence and observations. */
@Singleton
open class ShellStateRepository
@Inject
constructor(
  private val userProfileRepository: UserProfileRepository,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
  private val userId: String = UIUX_DEFAULT_USER_ID

  private val windowSizeClass = MutableStateFlow(defaultWindowSizeClass())
  private val undoPayload = MutableStateFlow<UndoPayload?>(null)
  private val progressJobs = MutableStateFlow<List<ProgressJob>>(emptyList())
  private val _recentActivity = MutableStateFlow<List<RecentActivityItem>>(emptyList())
  private val commandPalette = MutableStateFlow(CommandPaletteState.Empty)
  private val connectivity = MutableStateFlow(ConnectivityStatus.ONLINE)
  private val hasAppliedHomeStartup = AtomicBoolean(false)

  private val preferences: StateFlow<DomainUiPreferencesSnapshot> =
    userProfileRepository
      .observePreferences()
      .stateIn(scope, SharingStarted.Eagerly, DomainUiPreferencesSnapshot())

  private val uiSnapshot: StateFlow<UIStateSnapshot> =
    userProfileRepository
      .observeUIStateSnapshot(userId)
      .map { snapshot -> snapshot ?: defaultSnapshot(userId) }
      .map { snapshot -> coerceInitialActiveMode(snapshot) }
      .stateIn(scope, SharingStarted.Eagerly, defaultSnapshot(userId))

  private val shellLayout: StateFlow<ShellLayoutState> =
    combine(
        windowSizeClass,
        uiSnapshot,
        connectivity,
        undoPayload,
        progressJobs,
      ) { window, snapshot, connectivityStatus, undo, jobs ->
        ShellLayoutInputs(
          window = window,
          snapshot = snapshot,
          connectivityStatus = connectivityStatus,
          undo = undo,
          jobs = jobs,
          activity = emptyList(),
        )
      }
      .combine(_recentActivity) { inputs, activity ->
        buildShellLayoutState(inputs.copy(activity = activity))
      }
      .stateIn(
        scope,
        SharingStarted.Eagerly,
        buildShellLayoutState(
          ShellLayoutInputs(
            window = windowSizeClass.value,
            snapshot = uiSnapshot.value,
            connectivityStatus = connectivity.value,
            undo = undoPayload.value,
            jobs = progressJobs.value,
            activity = _recentActivity.value,
          ),
        ),
      )

  private val preferencesSnapshot: StateFlow<UiPreferenceSnapshot> =
    preferences
      .map { snapshot -> snapshot.toUiPreferenceSnapshot() }
      .stateIn(scope, SharingStarted.Eagerly, UiPreferenceSnapshot())

  private val connectivityBanner: StateFlow<ConnectivityBannerState> =
    combine(connectivity, progressJobs, preferences) { status, jobs, prefs ->
        ConnectivityBannerState(
          status = status,
          lastDismissedAt = prefs.connectivityBannerLastDismissed?.toJavaInstant(),
          queuedActionCount = jobs.count { !it.isTerminal },
          cta = modelLibraryCta(status),
        )
      }
      .stateIn(
        scope,
        SharingStarted.Eagerly,
        ConnectivityBannerState(status = connectivity.value),
      )

  open val shellLayoutState: Flow<ShellLayoutState> = shellLayout

  open val commandPaletteState: Flow<CommandPaletteState> = commandPalette.asStateFlow()

  open val connectivityBannerState: Flow<ConnectivityBannerState> = connectivityBanner

  open val uiPreferenceSnapshot: Flow<UiPreferenceSnapshot> = preferencesSnapshot

  open val recentActivity: Flow<List<RecentActivityItem>> = _recentActivity.asStateFlow()

  open fun updateWindowSizeClass(sizeClass: WindowSizeClass) {
    windowSizeClass.value = sizeClass
  }

  open suspend fun openMode(modeId: ModeId) {
    val route = modeId.toRoute()
    withContext(ioDispatcher) {
      userProfileRepository.updateActiveModeRoute(userId, route)
      userProfileRepository.updateLeftDrawerOpen(userId, false)
      userProfileRepository.updateCommandPaletteVisibility(userId, false)
    }
    commandPalette.update { state -> state.cleared() }
  }

  open suspend fun toggleLeftDrawer() {
    val current = uiSnapshot.value
    setLeftDrawer(!current.isLeftDrawerOpen)
  }

  open suspend fun setLeftDrawer(open: Boolean) {
    val current = uiSnapshot.value
    if (current.isLeftDrawerOpen == open && !(open && current.isCommandPaletteVisible)) {
      return
    }
    withContext(ioDispatcher) {
      userProfileRepository.updateLeftDrawerOpen(userId, open)
      if (open && current.isCommandPaletteVisible) {
        userProfileRepository.updateCommandPaletteVisibility(userId, false)
      }
    }
  }

  open suspend fun toggleRightDrawer(panel: RightPanel) {
    val snapshot = uiSnapshot.value
    val activePanel = snapshot.activeRightPanel.toRightPanel()
    val currentlyOpen = snapshot.isRightDrawerOpen && activePanel == panel
    val newOpen = !currentlyOpen
    val panelValue = if (newOpen) panel.toStorageValue() else null
    withContext(ioDispatcher) {
      userProfileRepository.updateRightDrawerState(userId, newOpen, panelValue)
      if (newOpen && snapshot.isCommandPaletteVisible) {
        userProfileRepository.updateCommandPaletteVisibility(userId, false)
      }
    }
  }

  open suspend fun showCommandPalette(source: PaletteSource) {
    commandPalette.update { state ->
      state.copy(surfaceTarget = source.toCategory()).clearSelection()
    }
    withContext(ioDispatcher) {
      userProfileRepository.updateLeftDrawerOpen(userId, false)
      userProfileRepository.updateCommandPaletteVisibility(userId, true)
    }
  }

  open suspend fun hideCommandPalette() {
    commandPalette.update { state -> state.cleared() }
    withContext(ioDispatcher) {
      userProfileRepository.updateCommandPaletteVisibility(userId, false)
    }
  }

  open suspend fun queueJob(job: ProgressJob) {
    progressJobs.update { jobs -> (jobs + job).sortedBy(ProgressJob::queuedAt) }
  }

  open suspend fun completeJob(jobId: UUID) {
    progressJobs.update { jobs -> jobs.filterNot { it.jobId == jobId } }
  }

  open suspend fun updateConnectivity(status: ConnectivityStatus) {
    connectivity.value = status
    userProfileRepository.setOfflineOverride(status != ConnectivityStatus.ONLINE)
  }

  open suspend fun updateThemePreference(theme: ThemePreference) {
    withContext(ioDispatcher) { userProfileRepository.updateThemePreference(userId, theme.name) }
  }

  open suspend fun updateVisualDensity(density: VisualDensity) {
    withContext(ioDispatcher) { userProfileRepository.updateVisualDensity(userId, density.name) }
  }

  open suspend fun recordUndoPayload(payload: UndoPayload?) {
    undoPayload.value = payload
  }

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  private fun defaultWindowSizeClass(): WindowSizeClass =
    WindowSizeClass.calculateFromSize(DpSize(width = 640.dp, height = 360.dp))

  private data class ShellLayoutInputs(
    val window: WindowSizeClass,
    val snapshot: UIStateSnapshot,
    val connectivityStatus: ConnectivityStatus,
    val undo: UndoPayload?,
    val jobs: List<ProgressJob>,
    val activity: List<RecentActivityItem>,
  )

  private fun buildShellLayoutState(inputs: ShellLayoutInputs): ShellLayoutState {
    val window = inputs.window
    val snapshot = inputs.snapshot
    val connectivityStatus = inputs.connectivityStatus
    val undo = inputs.undo
    val jobs = inputs.jobs
    val activity = inputs.activity
    return ShellLayoutState(
      windowSizeClass = window,
      isLeftDrawerOpen = snapshot.isLeftDrawerOpen,
      isRightDrawerOpen = snapshot.isRightDrawerOpen,
      activeRightPanel = snapshot.activeRightPanel.toRightPanel(),
      activeMode = snapshot.activeModeRoute.toModeIdOrDefault(),
      showCommandPalette = snapshot.isCommandPaletteVisible,
      connectivity = connectivityStatus,
      pendingUndoAction = undo,
      progressJobs = jobs,
      recentActivity = activity,
    )
  }

  private fun coerceInitialActiveMode(snapshot: UIStateSnapshot): UIStateSnapshot {
    if (hasAppliedHomeStartup.compareAndSet(false, true)) {
      val resetSnapshot =
        snapshot
          .updateActiveMode(UIStateSnapshot.DEFAULT_MODE_ROUTE)
          .toggleLeftDrawer(open = false)
          .toggleRightDrawer(open = false, panelId = null)
          .updatePaletteVisibility(visible = false)

      scope.launch {
        if (
          !snapshot.activeModeRoute.equals(UIStateSnapshot.DEFAULT_MODE_ROUTE, ignoreCase = true)
        ) {
          userProfileRepository.updateActiveModeRoute(userId, ModeId.HOME.toRoute())
        }
        if (snapshot.isLeftDrawerOpen) {
          userProfileRepository.updateLeftDrawerOpen(userId, false)
        }
        if (snapshot.isRightDrawerOpen || snapshot.activeRightPanel != null) {
          userProfileRepository.updateRightDrawerState(userId, false, null)
        }
        if (snapshot.isCommandPaletteVisible) {
          userProfileRepository.updateCommandPaletteVisibility(userId, false)
        }
      }

      return resetSnapshot
    }

    return snapshot
  }

  private fun DomainUiPreferencesSnapshot.toUiPreferenceSnapshot(): UiPreferenceSnapshot =
    UiPreferenceSnapshot(
      theme = themePreference,
      density = visualDensity,
      fontScale = 1f,
      dismissedTooltips = emptySet(),
    )

  private fun String?.toRightPanel(): RightPanel? {
    val value = this ?: return null
    return RightPanel.entries.firstOrNull { panel -> panel.name.equals(value, ignoreCase = true) }
  }

  private fun RightPanel.toStorageValue(): String = name.lowercase()

  private fun PaletteSource.toCategory(): CommandCategory =
    when (this) {
      PaletteSource.KEYBOARD_SHORTCUT -> CommandCategory.MODES
      PaletteSource.TOP_APP_BAR -> CommandCategory.SETTINGS
      PaletteSource.QUICK_ACTION -> CommandCategory.JOBS
      PaletteSource.UNKNOWN -> CommandCategory.MODES
    }

  private fun defaultSnapshot(userId: String): UIStateSnapshot =
    UIStateSnapshot(
      userId = userId,
      expandedPanels = emptyList(),
      recentActions = emptyList(),
      isSidebarCollapsed = false,
    )

  private fun modelLibraryCta(status: ConnectivityStatus): CommandAction? =
    when (status) {
      ConnectivityStatus.OFFLINE,
      ConnectivityStatus.LIMITED -> MODEL_LIBRARY_CTA
      ConnectivityStatus.ONLINE -> null
    }

  private companion object {
    private val MODEL_LIBRARY_CTA =
      CommandAction(
        id = "open-model-library",
        title = "Manage downloads",
        category = CommandCategory.JOBS,
        destination = CommandDestination.Navigate(ModeId.LIBRARY.toRoute()),
      )
  }
}
