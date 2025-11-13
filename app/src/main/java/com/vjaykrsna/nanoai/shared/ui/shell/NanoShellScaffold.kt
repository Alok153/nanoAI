package com.vjaykrsna.nanoai.shared.ui.shell

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandAction
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandInvocationSource
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.PaletteDismissReason
import com.vjaykrsna.nanoai.core.domain.model.uiux.PaletteSource
import com.vjaykrsna.nanoai.core.domain.model.uiux.ProgressJob
import com.vjaykrsna.nanoai.core.domain.model.uiux.RightPanel
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UndoPayload
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellUiState
import java.util.UUID

/** Root scaffold Compose entry point for the unified shell experience. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun NanoShellScaffold(
  state: ShellUiState,
  onEvent: (ShellUiEvent) -> Unit,
  modifier: Modifier = Modifier,
  modeContent: @Composable (ModeId) -> Unit = {},
) {
  val layout = state.layout
  val snackbarHostState = remember { SnackbarHostState() }
  val focusRequester = remember { FocusRequester() }
  val drawerState = rememberShellScaffoldState()
  val currentOnEvent = rememberUpdatedState(onEvent)

  val dispatchEvent =
    remember(layout, currentOnEvent) { createShellEventDispatcher(layout, currentOnEvent.value) }

  ShellScaffoldEffects(
    layout = layout,
    drawerState = drawerState,
    snackbarHostState = snackbarHostState,
    focusRequester = focusRequester,
    onEvent = currentOnEvent.value,
  )

  ShellScaffoldLayout(
    state = state,
    layout = layout,
    snackbarHostState = snackbarHostState,
    focusRequester = focusRequester,
    drawerState = drawerState,
    dispatchEvent = dispatchEvent,
    onEvent = currentOnEvent.value,
    modeContent = modeContent,
    modifier = modifier,
  )
}

/** Events emitted by [NanoShellScaffold] to interact with view models. */
sealed interface ShellUiEvent {
  data class ModeSelected(val modeId: ModeId) : ShellUiEvent

  data object ToggleLeftDrawer : ShellUiEvent

  data class SetLeftDrawer(val open: Boolean) : ShellUiEvent

  data class ToggleRightDrawer(val panel: RightPanel) : ShellUiEvent

  data class ShowCommandPalette(val source: PaletteSource) : ShellUiEvent

  data class HideCommandPalette(val reason: PaletteDismissReason) : ShellUiEvent

  data class CommandInvoked(val action: CommandAction, val source: CommandInvocationSource) :
    ShellUiEvent

  data class QueueJob(val job: ProgressJob) : ShellUiEvent

  data class RetryJob(val job: ProgressJob) : ShellUiEvent

  data class CompleteJob(val jobId: UUID) : ShellUiEvent

  data class Undo(val payload: UndoPayload) : ShellUiEvent

  data class ConnectivityChanged(val status: ConnectivityStatus) : ShellUiEvent

  data class UpdateTheme(val theme: ThemePreference) : ShellUiEvent

  data class UpdateDensity(val density: VisualDensity) : ShellUiEvent

  data class ChatPersonaSelected(val personaId: UUID, val action: PersonaSwitchAction) :
    ShellUiEvent

  data object ChatTitleClicked : ShellUiEvent

  data object ShowCoverageDashboard : ShellUiEvent

  data object HideCoverageDashboard : ShellUiEvent
}

internal enum class DrawerVariant {
  Modal,
  Permanent,
}
