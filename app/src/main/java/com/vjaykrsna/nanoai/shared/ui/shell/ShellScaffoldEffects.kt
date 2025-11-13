package com.vjaykrsna.nanoai.shared.ui.shell

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.focus.FocusRequester
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellLayoutState
import kotlinx.coroutines.flow.collect

internal fun createShellEventDispatcher(
  layout: ShellLayoutState,
  onEvent: (ShellUiEvent) -> Unit,
): (ShellUiEvent) -> Unit = { event ->
  when (event) {
    ShellUiEvent.ToggleLeftDrawer -> handleToggleLeftDrawer(layout, onEvent)
    is ShellUiEvent.ToggleRightDrawer -> handleToggleRightDrawer(layout, event.panel, onEvent)
    is ShellUiEvent.ShowCommandPalette -> {
      handleCloseLeftDrawer(layout, onEvent)
      handleCloseRightDrawer(layout, onEvent)
      onEvent(event)
    }
    is ShellUiEvent.ModeSelected -> {
      handleCloseLeftDrawer(layout, onEvent)
      handleCloseRightDrawer(layout, onEvent)
      onEvent(event)
    }
    else -> onEvent(event)
  }
}

@Composable
internal fun rememberShellScaffoldState(): DrawerState =
  rememberDrawerState(initialValue = DrawerValue.Closed)

@Composable
internal fun ShellScaffoldEffects(
  layout: ShellLayoutState,
  drawerState: DrawerState,
  snackbarHostState: SnackbarHostState,
  focusRequester: FocusRequester,
  onEvent: (ShellUiEvent) -> Unit,
) {
  val latestLeftDrawerOpen = rememberUpdatedState(layout.isLeftDrawerOpen)
  val latestOnEvent by rememberUpdatedState(onEvent)

  LaunchedEffect(layout.useModalNavigation, layout.isLeftDrawerOpen) {
    if (!layout.useModalNavigation) {
      drawerState.close()
      return@LaunchedEffect
    }
    if (layout.isLeftDrawerOpen) {
      drawerState.open()
    } else {
      drawerState.close()
    }
  }

  LaunchedEffect(drawerState, layout.useModalNavigation) {
    if (!layout.useModalNavigation) return@LaunchedEffect
    snapshotFlow { drawerState.currentValue }
      .collect { value ->
        val isOpen = value == DrawerValue.Open
        if (isOpen != latestLeftDrawerOpen.value) {
          latestOnEvent(ShellUiEvent.SetLeftDrawer(isOpen))
        }
      }
  }

  LaunchedEffect(layout.pendingUndoAction) {
    val payload = layout.pendingUndoAction ?: return@LaunchedEffect
    val message = payload.metadata["message"] as? String ?: "Action completed"
    val result = snackbarHostState.showSnackbar(message = message, actionLabel = "Undo")
    if (result == SnackbarResult.ActionPerformed) {
      latestOnEvent(ShellUiEvent.Undo(payload))
    }
  }

  LaunchedEffect(Unit) { focusRequester.requestFocus() }
  LaunchedEffect(layout.isPaletteVisible) {
    if (!layout.isPaletteVisible) {
      focusRequester.requestFocus()
    }
  }
}
