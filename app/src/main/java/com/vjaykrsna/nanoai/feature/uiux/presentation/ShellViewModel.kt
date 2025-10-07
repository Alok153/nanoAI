@file:OptIn(
  androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class
)

package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.feature.uiux.data.ShellStateRepository
import com.vjaykrsna.nanoai.feature.uiux.domain.CommandPaletteActionProvider
import com.vjaykrsna.nanoai.feature.uiux.domain.ProgressCenterCoordinator
import com.vjaykrsna.nanoai.feature.uiux.state.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.state.CommandCategory
import com.vjaykrsna.nanoai.feature.uiux.state.CommandDestination
import com.vjaykrsna.nanoai.feature.uiux.state.CommandInvocationSource
import com.vjaykrsna.nanoai.feature.uiux.state.CommandPaletteState
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityBannerState
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.state.DrawerSide
import com.vjaykrsna.nanoai.feature.uiux.state.ModeCard
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.PaletteDismissReason
import com.vjaykrsna.nanoai.feature.uiux.state.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.state.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.state.UiPreferenceSnapshot
import com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload
import com.vjaykrsna.nanoai.feature.uiux.state.toRoute
import com.vjaykrsna.nanoai.telemetry.ShellTelemetry
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel coordinating shell layout state and user intents. */
private const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L

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
      subtitle = "Programming assistant",
      icon = Icons.Default.Code,
      actionId = "new_code",
      actionTitle = "New Session",
      actionCategory = CommandCategory.MODES,
    ),
    ModeCardDefinition(
      id = ModeId.TRANSLATE,
      title = "Translate",
      subtitle = "Language translation",
      icon = Icons.Default.Language,
      actionId = "new_translate",
      actionTitle = "Translate",
      actionCategory = CommandCategory.MODES,
    ),
    ModeCardDefinition(
      id = ModeId.HISTORY,
      title = "History",
      subtitle = "View recent activity",
      icon = Icons.Default.History,
      actionId = "view_history",
      actionTitle = "View",
      actionCategory = CommandCategory.MODES,
    ),
    ModeCardDefinition(
      id = ModeId.LIBRARY,
      title = "Library",
      subtitle = "Manage AI models",
      icon = Icons.AutoMirrored.Filled.LibraryBooks,
      actionId = "view_library",
      actionTitle = "Manage",
      actionCategory = CommandCategory.MODES,
    ),
    ModeCardDefinition(
      id = ModeId.SETTINGS,
      title = "Settings",
      subtitle = "App preferences",
      icon = Icons.Default.Settings,
      actionId = "open_settings",
      actionTitle = "Configure",
      actionCategory = CommandCategory.SETTINGS,
    ),
  )

@HiltViewModel
class ShellViewModel
@Inject
constructor(
  private val repository: ShellStateRepository,
  private val actionProvider: CommandPaletteActionProvider,
  private val progressCoordinator: ProgressCenterCoordinator,
  private val telemetry: ShellTelemetry,
  @MainImmediateDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {

  /**
   * Combined UI state exposing shell layout, command palette, connectivity banner, preferences,
   * mode cards, and quick actions.
   */
  val uiState: StateFlow<ShellUiState> =
    combine(
        repository.shellLayoutState,
        repository.commandPaletteState,
        repository.connectivityBannerState,
        repository.uiPreferenceSnapshot,
        progressCoordinator.progressJobs,
      ) { layout, palette, banner, prefs, jobs ->
        val mergedJobs = mergeProgressJobs(layout.progressJobs, jobs)
        ShellUiState(
          layout = layout.copy(progressJobs = mergedJobs),
          commandPalette = palette,
          connectivityBanner = banner,
          preferences = prefs,
          modeCards = buildModeCards(layout.connectivity),
          quickActions = buildQuickActions(layout.connectivity),
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
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
    viewModelScope.launch(dispatcher) { repository.openMode(modeId) }
  }

  /** Toggles the left navigation drawer. */
  fun toggleLeftDrawer() {
    val layout = uiState.value.layout
    val newOpen = !layout.isLeftDrawerOpen
    viewModelScope.launch(dispatcher) {
      repository.toggleLeftDrawer()
      telemetry.trackDrawerToggle(
        side = DrawerSide.LEFT,
        isOpen = newOpen,
        panel = null,
        activeMode = layout.activeMode,
      )
    }
  }

  /** Toggles the right contextual drawer for a specific panel. */
  fun toggleRightDrawer(panel: RightPanel) {
    val layout = uiState.value.layout
    val currentlyOpen = layout.isRightDrawerOpen && layout.activeRightPanel == panel
    val newOpen = !currentlyOpen
    val panelForTelemetry = if (newOpen) panel else layout.activeRightPanel
    viewModelScope.launch(dispatcher) {
      repository.toggleRightDrawer(panel)
      telemetry.trackDrawerToggle(
        side = DrawerSide.RIGHT,
        isOpen = newOpen,
        panel = panelForTelemetry,
        activeMode = layout.activeMode,
      )
    }
  }

  /** Shows the command palette overlay from a specific source. */
  fun showCommandPalette(source: PaletteSource) {
    val activeMode = uiState.value.layout.activeMode
    viewModelScope.launch(dispatcher) {
      repository.showCommandPalette(source)
      telemetry.trackCommandPaletteOpened(source, activeMode)
    }
  }

  /** Hides the command palette overlay. */
  fun hideCommandPalette(reason: PaletteDismissReason) {
    val activeMode = uiState.value.layout.activeMode
    viewModelScope.launch(dispatcher) {
      repository.hideCommandPalette()
      if (reason != PaletteDismissReason.EXECUTED) {
        telemetry.trackCommandPaletteDismissed(reason, activeMode)
      }
    }
  }

  /** Queues a generation job (e.g., when offline or model busy). */
  fun queueGeneration(job: ProgressJob) {
    val layout = uiState.value.layout
    viewModelScope.launch(dispatcher) {
      repository.queueJob(job)
      repository.recordUndoPayload(UndoPayload(actionId = "queue-${job.jobId}"))
      telemetry.trackProgressJobQueued(job, layout.isOffline, layout.activeMode)
    }
  }

  /** Completes a job, removing it from the progress center. */
  fun completeJob(jobId: UUID) {
    viewModelScope.launch(dispatcher) {
      repository.completeJob(jobId)
      repository.recordUndoPayload(null)
    }
  }

  /** Executes an undo action based on the provided payload. */
  fun undoAction(payload: UndoPayload) {
    viewModelScope.launch(dispatcher) {
      // Parse the action ID to determine what to undo
      when {
        payload.actionId.startsWith("queue-") -> {
          val jobIdString = payload.actionId.removePrefix("queue-")
          val jobId = runCatching { UUID.fromString(jobIdString) }.getOrNull()
          if (jobId != null) {
            repository.completeJob(jobId)
          }
        }
      // Add more undo action types as needed
      }
      repository.recordUndoPayload(null)
    }
  }

  /** Updates connectivity status and handles online/offline transitions. */
  fun updateConnectivity(status: ConnectivityStatus) {
    viewModelScope.launch(dispatcher) { repository.updateConnectivity(status) }
  }

  /** Updates persisted theme preference for the active user. */
  fun updateThemePreference(theme: ThemePreference) {
    viewModelScope.launch(dispatcher) { repository.updateThemePreference(theme) }
  }

  /** Updates persisted density preference for the active user. */
  fun updateVisualDensity(density: VisualDensity) {
    viewModelScope.launch(dispatcher) { repository.updateVisualDensity(density) }
  }

  /** Records telemetry for command invocations to understand palette usage. */
  fun onCommandInvoked(action: CommandAction, source: CommandInvocationSource) {
    val layout = uiState.value.layout
    telemetry.trackCommandInvocation(action, source, layout.activeMode)
    if (source == CommandInvocationSource.PALETTE && layout.isPaletteVisible) {
      telemetry.trackCommandPaletteDismissed(PaletteDismissReason.EXECUTED, layout.activeMode)
    }
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
    return merged.values.toList()
  }

  /** Builds the list of mode cards for the home hub grid. */
  private fun buildModeCards(connectivity: ConnectivityStatus): List<ModeCard> {
    val isOnline = connectivity == ConnectivityStatus.ONLINE
    return MODE_CARD_DEFINITIONS.map { definition ->
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
    viewModelScope.launch(dispatcher) { repository.updateWindowSizeClass(sizeClass) }
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
)
