package com.vjaykrsna.nanoai.feature.uiux.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellUiState
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload
import java.util.UUID

/** Root scaffold Compose entry point for the unified shell experience. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun NanoShellScaffold(
  state: ShellUiState,
  onEvent: (ShellUiEvent) -> Unit,
  modifier: Modifier = Modifier,
  modeContent: @Composable (ModeId) -> Unit = {},
) {
  val unused = Triple(state, onEvent, modeContent)
  TODO("Phase 3.3 UI implementation pending")
}

/** Events emitted by [NanoShellScaffold] to interact with view models. */
sealed interface ShellUiEvent {
  data class ModeSelected(val modeId: ModeId) : ShellUiEvent
  data object ToggleLeftDrawer : ShellUiEvent
  data class ToggleRightDrawer(val panel: RightPanel) : ShellUiEvent
  data class ShowCommandPalette(val source: PaletteSource) : ShellUiEvent
  data object HideCommandPalette : ShellUiEvent
  data class QueueJob(val job: ProgressJob) : ShellUiEvent
  data class CompleteJob(val jobId: UUID) : ShellUiEvent
  data class Undo(val payload: UndoPayload) : ShellUiEvent
  data class ConnectivityChanged(val status: ConnectivityStatus) : ShellUiEvent
}
