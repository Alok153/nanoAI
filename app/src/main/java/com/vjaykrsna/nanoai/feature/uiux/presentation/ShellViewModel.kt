@file:OptIn(
  androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class
)

package com.vjaykrsna.nanoai.feature.uiux.presentation

// import com.vjaykrsna.telemetry.ShellTelemetry
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.data.repository.ConnectivityRepository
import com.vjaykrsna.nanoai.core.data.repository.NavigationRepository
import com.vjaykrsna.nanoai.core.data.repository.ProgressRepository
import com.vjaykrsna.nanoai.core.data.repository.ThemeRepository
import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.feature.uiux.domain.CommandPaletteActionProvider
import com.vjaykrsna.nanoai.feature.uiux.domain.ConnectivityOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.JobOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.NavigationOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.ProgressCenterCoordinator
import com.vjaykrsna.nanoai.feature.uiux.domain.QueueJobUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.SettingsOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.UndoActionUseCase
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandCategory
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandDestination
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandInvocationSource
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandPaletteState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ConnectivityBannerState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeCard
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
import com.vjaykrsna.nanoai.feature.uiux.presentation.PaletteDismissReason
import com.vjaykrsna.nanoai.feature.uiux.presentation.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.presentation.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.presentation.RecentActivityItem
import com.vjaykrsna.nanoai.feature.uiux.presentation.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.presentation.UiPreferenceSnapshot
import com.vjaykrsna.nanoai.feature.uiux.presentation.UndoPayload
import com.vjaykrsna.nanoai.feature.uiux.presentation.toModeIdOrDefault
import com.vjaykrsna.nanoai.feature.uiux.presentation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel coordinating shell layout state and user intents. */
private const val LAYOUT_INDEX = 0
private const val PALETTE_INDEX = 1
private const val BANNER_INDEX = 2
private const val PREFS_INDEX = 3
private const val JOBS_INDEX = 4
private const val CHAT_STATE_INDEX = 5

private data class ModeCardDefinition(
  val id: ModeId,
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val actionId: String,
  val actionTitle: String,
  val actionCategory: CommandCategory,
  val requiresOnline: Boolean = false,
)

private val MODE_CARD_DEFINITIONS =
  listOf(
    ModeCardDefinition(
      id = ModeId.CHAT,
      title = "Chat",
      subtitle = "Conversational AI assistant",
      icon = Icons.AutoMirrored.Filled.Chat,
      actionId = "new_chat",
      actionTitle = "New Chat",
      actionCategory = CommandCategory.MODES,
    ),
    ModeCardDefinition(
      id = ModeId.IMAGE,
      title = "Image",
      subtitle = "Generate images from text",
      icon = Icons.Default.Image,
      actionId = "new_image",
      actionTitle = "Generate",
      actionCategory = CommandCategory.MODES,
      requiresOnline = true,
    ),
    ModeCardDefinition(
      id = ModeId.AUDIO,
      title = "Audio",
      subtitle = "Voice and audio processing",
      icon = Icons.Default.Mic,
      actionId = "new_audio",
      actionTitle = "Record",
      actionCategory = CommandCategory.MODES,
    ),
    ModeCardDefinition(
      id = ModeId.CODE,
      title = "Code",
      subtitle = "Code generation and analysis",
      icon = Icons.Default.Code,
      actionId = "new_code",
      actionTitle = "Code",
      actionCategory = CommandCategory.MODES,
    ),
    ModeCardDefinition(
      id = ModeId.HISTORY,
      title = "History",
      subtitle = "View past conversations",
      icon = Icons.Default.History,
      actionId = "view_history",
      actionTitle = "History",
      actionCategory = CommandCategory.MODES,
    ),
    ModeCardDefinition(
      id = ModeId.SETTINGS,
      title = "Settings",
      subtitle = "Configure your experience",
      icon = Icons.Default.Settings,
      actionId = "open_settings",
      actionTitle = "Settings",
      actionCategory = CommandCategory.SETTINGS,
    ),
  )

private data class ShellLayoutInputs(
  val window: WindowSizeClass,
  val snapshot: UIStateSnapshot,
  val connectivityStatus: ConnectivityStatus,
  val undo: UndoPayload?,
  val jobs: List<ProgressJob>,
  val activity: List<RecentActivityItem>,
)

@HiltViewModel
class ShellViewModel
@Inject
constructor(
  private val navigationRepository: NavigationRepository,
  private val connectivityRepository: ConnectivityRepository,
  private val themeRepository: ThemeRepository,
  private val progressRepository: ProgressRepository,
  private val userProfileRepository: UserProfileRepository,
  private val actionProvider: CommandPaletteActionProvider,
  private val progressCoordinator: ProgressCenterCoordinator,
  // Consolidated UseCases for domain operations
  private val navigationOperationsUseCase: NavigationOperationsUseCase,
  private val connectivityOperationsUseCase: ConnectivityOperationsUseCase,
  private val queueJobUseCase: QueueJobUseCase,
  private val jobOperationsUseCase: JobOperationsUseCase,
  private val undoActionUseCase: UndoActionUseCase,
  private val settingsOperationsUseCase: SettingsOperationsUseCase,
  // private val telemetry: ShellTelemetry,
  @Suppress("UnusedPrivateProperty")
  @MainImmediateDispatcher
  private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {

  private val userId: String = "default" // TODO: get from somewhere

  private val uiSnapshot: StateFlow<UIStateSnapshot> =
    userProfileRepository
      .observeUIStateSnapshot(userId)
      .map { snapshot -> snapshot ?: defaultSnapshot(userId) }
      .map { snapshot -> coerceInitialActiveMode(snapshot) }
      .stateIn(viewModelScope, SharingStarted.Eagerly, defaultSnapshot(userId))

  private val shellLayout: StateFlow<ShellLayoutState> =
    combine(
        navigationRepository.windowSizeClass,
        uiSnapshot,
        connectivityRepository.connectivityBannerState.map { it.status },
        navigationRepository.undoPayload,
        progressRepository.progressJobs,
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
      .combine(navigationRepository.recentActivity) { inputs, activity ->
        buildShellLayoutState(inputs.copy(activity = activity))
      }
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        buildShellLayoutState(
          ShellLayoutInputs(
            window =
              androidx.compose.material3.windowsizeclass.WindowSizeClass.calculateFromSize(
                DpSize(width = 640.dp, height = 360.dp)
              ),
            snapshot = uiSnapshot.value,
            connectivityStatus = ConnectivityStatus.ONLINE,
            undo = null,
            jobs = emptyList(),
            activity = emptyList(),
          )
        ),
      )

  private val _chatState = MutableStateFlow<ChatState?>(null)

  /**
   * Combined UI state exposing shell layout, command palette, connectivity banner, preferences,
   * mode cards, and quick actions.
   */
  val uiState: StateFlow<ShellUiState> =
    combine(
        shellLayout,
        navigationRepository.commandPaletteState,
        connectivityRepository.connectivityBannerState,
        themeRepository.uiPreferenceSnapshot,
        progressCoordinator.progressJobs,
        _chatState,
      ) { values ->
        val layout = values[LAYOUT_INDEX] as ShellLayoutState
        val palette = values[PALETTE_INDEX] as CommandPaletteState
        val banner = values[BANNER_INDEX] as ConnectivityBannerState
        val prefs = values[PREFS_INDEX] as UiPreferenceSnapshot
        @Suppress("UNCHECKED_CAST") val jobs = values[JOBS_INDEX] as List<ProgressJob>
        val chatState = values[CHAT_STATE_INDEX] as ChatState?

        val mergedJobs = mergeProgressJobs(layout.progressJobs, jobs)
        val sanitizedUndo = sanitizeUndoPayload(layout.pendingUndoAction, mergedJobs)
        val normalizedLayout =
          layout.copy(progressJobs = mergedJobs, pendingUndoAction = sanitizedUndo)
        val normalizedBanner =
          banner.copy(
            status = normalizedLayout.connectivity,
            queuedActionCount = mergedJobs.count { it.isPending || it.isActive },
          )

        ShellUiState(
          layout = normalizedLayout,
          commandPalette = palette,
          connectivityBanner = normalizedBanner,
          preferences = prefs,
          modeCards = buildModeCards(normalizedLayout.connectivity),
          quickActions = buildQuickActions(normalizedLayout.connectivity),
          chatState = chatState,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = buildInitialState(),
      )

  private fun buildInitialState(): ShellUiState {
    val defaultWindowSize =
      androidx.compose.material3.windowsizeclass.WindowSizeClass.calculateFromSize(
        androidx.compose.ui.unit.DpSize(width = 640.dp, height = 360.dp)
      )
    return ShellUiState(
      layout =
        ShellLayoutState(
          windowSizeClass = defaultWindowSize,
          isLeftDrawerOpen = false,
          isRightDrawerOpen = false,
          activeRightPanel = null,
          activeMode = ModeId.HOME,
          showCommandPalette = false,
          connectivity = ConnectivityStatus.ONLINE,
          pendingUndoAction = null,
          progressJobs = emptyList(),
          recentActivity = emptyList(),
        ),
      commandPalette = CommandPaletteState.Empty,
      connectivityBanner = ConnectivityBannerState(status = ConnectivityStatus.ONLINE),
      preferences = UiPreferenceSnapshot(),
    )
  }

  /** Opens a specific mode, closing drawers and hiding the command palette. */
  fun openMode(modeId: ModeId) {
    navigationOperationsUseCase.openMode(modeId)
  }

  /** Toggles the left navigation drawer. */
  fun toggleLeftDrawer() {
    navigationOperationsUseCase.toggleLeftDrawer()
  }

  /** Sets the left drawer to a specific open/closed state. */
  fun setLeftDrawer(open: Boolean) {
    navigationOperationsUseCase.setLeftDrawer(open)
  }

  /** Toggles the right contextual drawer for a specific panel. */
  fun toggleRightDrawer(panel: RightPanel) {
    navigationOperationsUseCase.toggleRightDrawer(panel)
  }

  /** Shows the command palette overlay from a specific source. */
  fun showCommandPalette(source: PaletteSource) {
    navigationOperationsUseCase.showCommandPalette(source)
  }

  /** Hides the command palette overlay. */
  @Suppress("UnusedParameter")
  fun hideCommandPalette(reason: PaletteDismissReason) {
    navigationOperationsUseCase.hideCommandPalette()
  }

  /** Queues a generation job (e.g., when offline or model busy). */
  fun queueGeneration(job: ProgressJob) {
    queueJobUseCase.execute(job)
  }

  /** Completes a job, removing it from the progress center. */
  fun completeJob(jobId: UUID) {
    jobOperationsUseCase.completeJob(jobId)
  }

  /** Attempts to retry a failed job via the progress coordinator. */
  fun retryJob(job: ProgressJob) {
    jobOperationsUseCase.retryJob(job.jobId)
  }

  /** Executes an undo action based on the provided payload. */
  fun undoAction(payload: UndoPayload) {
    undoActionUseCase.execute(payload)
  }

  /** Updates connectivity status and handles online/offline transitions. */
  fun updateConnectivity(status: ConnectivityStatus) {
    connectivityOperationsUseCase.updateConnectivity(status)
  }

  /** Updates persisted theme preference for the active user. */
  fun updateThemePreference(theme: ThemePreference) {
    viewModelScope.launch { settingsOperationsUseCase.updateTheme(theme) }
  }

  /** Updates persisted density preference for the active user. */
  fun updateVisualDensity(density: VisualDensity) {
    viewModelScope.launch { settingsOperationsUseCase.updateVisualDensity(density) }
  }

  /** Records telemetry for command invocations to understand palette usage. */
  @Suppress("UnusedParameter")
  fun onCommandInvoked(action: CommandAction, source: CommandInvocationSource) {
    val layout = uiState.value.layout
    /*
    // // telemetry.trackCommandInvocation(action, source, layout.activeMode)
    if (source == CommandInvocationSource.PALETTE && layout.isPaletteVisible) {
      // // telemetry.trackCommandPaletteDismissed(PaletteDismissReason.EXECUTED, layout.activeMode)
    }
    */
  }

  private fun mergeProgressJobs(
    repositoryJobs: List<ProgressJob>,
    coordinatorJobs: List<ProgressJob>,
  ): List<ProgressJob> {
    if (coordinatorJobs.isEmpty() || repositoryJobs.isEmpty()) {
      return when {
        coordinatorJobs.isEmpty() -> repositoryJobs
        repositoryJobs.isEmpty() -> coordinatorJobs
        else -> emptyList()
      }
    }

    val merged = linkedMapOf<UUID, ProgressJob>()
    repositoryJobs.forEach { job -> merged[job.jobId] = job }
    coordinatorJobs.forEach { job -> merged[job.jobId] = job }
    return merged.values.sortedBy(ProgressJob::queuedAt)
  }

  private fun sanitizeUndoPayload(payload: UndoPayload?, jobs: List<ProgressJob>): UndoPayload? {
    val currentPayload = payload ?: return null
    val jobId = currentPayload.extractJobId()
    val activeJob = jobId?.let { id -> jobs.firstOrNull { it.jobId == id } }
    return when {
      jobId == null -> currentPayload
      activeJob == null -> null
      activeJob.isTerminal -> null
      else -> currentPayload
    }
  }

  private fun UndoPayload.extractJobId(): UUID? {
    val metadataId = metadata["jobId"] as? String
    val metadataUuid = metadataId?.let(::parseUuid)
    val actionUuid =
      if (actionId.startsWith(JOB_QUEUE_PREFIX)) {
        parseUuid(actionId.removePrefix(JOB_QUEUE_PREFIX))
      } else {
        null
      }
    return metadataUuid ?: actionUuid
  }

  /** Builds the list of mode cards for the home hub grid. */
  private fun buildModeCards(connectivity: ConnectivityStatus): List<ModeCard> {
    val isOnline = connectivity == ConnectivityStatus.ONLINE
    return MODE_CARD_DEFINITIONS.filter {
        it.id != ModeId.SETTINGS
      } // Exclude settings from home page
      .map { definition ->
        val enabled = if (definition.requiresOnline) isOnline else true
        ModeCard(
          id = definition.id,
          title = definition.title,
          subtitle = definition.subtitle,
          icon = definition.icon,
          enabled = enabled,
          primaryAction =
            CommandAction(
              id = definition.actionId,
              title = definition.actionTitle,
              category = definition.actionCategory,
              destination = CommandDestination.Navigate(definition.id.toRoute()),
            ),
        )
      }
  }

  /** Builds quick action commands shown on the home screen. */
  private fun buildQuickActions(connectivity: ConnectivityStatus): List<CommandAction> {
    val modeActionsById = actionProvider.provideModeActions(connectivity).associateBy { it.id }
    return listOfNotNull(
      modeActionsById["mode_chat"]?.copy(id = "quick_new_chat"),
      modeActionsById["mode_image"]?.copy(id = "quick_generate_image"),
      modeActionsById["mode_audio"]?.copy(id = "quick_voice", title = "Voice Session"),
    )
  }

  /** Updates the current window size class so adaptive layouts respond to device changes. */
  fun updateWindowSizeClass(sizeClass: WindowSizeClass) {
    navigationOperationsUseCase.updateWindowSizeClass(sizeClass)
  }

  /** Updates the chat-specific state for contextual UI. */
  fun updateChatState(chatState: ChatState?) {
    _chatState.value = chatState
  }

  private fun parseUuid(raw: String): UUID? = runCatching { UUID.fromString(raw) }.getOrNull()

  private fun coerceInitialActiveMode(snapshot: UIStateSnapshot): UIStateSnapshot {
    // For ShellViewModel, we don't need to coerce the initial mode
    // as navigation state comes from focused repositories
    return snapshot
  }

  private fun defaultSnapshot(userId: String): UIStateSnapshot {
    return UIStateSnapshot(
      userId = userId,
      expandedPanels = emptyList(),
      recentActions = emptyList(),
      isSidebarCollapsed = false,
    )
  }

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

  private fun String?.toRightPanel(): RightPanel? {
    val value = this ?: return null
    return RightPanel.entries.firstOrNull { panel -> panel.name.equals(value, ignoreCase = true) }
  }

  private companion object {
    private const val JOB_QUEUE_PREFIX = "queue-"
  }
}

/** Aggregated UI state exposed by [ShellViewModel]. */
data class ShellUiState(
  val layout: ShellLayoutState,
  val commandPalette: CommandPaletteState,
  val connectivityBanner: ConnectivityBannerState,
  val preferences: UiPreferenceSnapshot,
  val modeCards: List<ModeCard> = emptyList(),
  val quickActions: List<CommandAction> = emptyList(),
  val chatState: ChatState? = null,
)
