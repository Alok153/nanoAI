package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.lifecycle.ViewModel
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.feature.uiux.data.ShellStateRepository
import com.vjaykrsna.nanoai.feature.uiux.state.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.state.CommandPaletteState
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityBannerState
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.ModeCard
import com.vjaykrsna.nanoai.feature.uiux.state.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.state.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.state.UiPreferenceSnapshot
import com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/** ViewModel coordinating shell layout state and user intents. */
@HiltViewModel
class ShellViewModel
@Inject
constructor(
  private val repository: ShellStateRepository,
  @MainImmediateDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
  init {
    @Suppress("UNUSED_PARAMETER")
    val unused = dispatcher
  }
  val uiState: StateFlow<ShellUiState>
    get() = TODO("Phase 3.3 will provide combined shell state")

  fun openMode(modeId: ModeId) {
    TODO("Phase 3.3 will implement shell intents")
  }

  fun toggleLeftDrawer() {
    TODO("Phase 3.3 will implement shell intents")
  }

  fun toggleRightDrawer(panel: RightPanel) {
    TODO("Phase 3.3 will implement shell intents")
  }

  fun showCommandPalette(source: PaletteSource) {
    TODO("Phase 3.3 will implement shell intents")
  }

  fun hideCommandPalette() {
    TODO("Phase 3.3 will implement shell intents")
  }

  fun queueGeneration(job: ProgressJob) {
    TODO("Phase 3.3 will implement shell intents")
  }

  fun completeJob(jobId: UUID) {
    TODO("Phase 3.3 will implement shell intents")
  }

  fun undoAction(payload: UndoPayload) {
    TODO("Phase 3.3 will implement shell intents")
  }

  fun updateConnectivity(status: ConnectivityStatus) {
    TODO("Phase 3.3 will implement shell intents")
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
