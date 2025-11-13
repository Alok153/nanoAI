package com.vjaykrsna.nanoai.shared.ui.shell

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandAction
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandDestination
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandInvocationSource
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.PaletteDismissReason
import com.vjaykrsna.nanoai.core.domain.model.uiux.PaletteSource
import com.vjaykrsna.nanoai.core.domain.uiux.navigation.toModeIdOrNull

internal fun handleCommandAction(
  action: CommandAction,
  source: CommandInvocationSource,
  onEvent: (ShellUiEvent) -> Unit,
) {
  onEvent(ShellUiEvent.CommandInvoked(action, source))
  when (val destination = action.destination) {
    is CommandDestination.Navigate -> {
      val modeId = routeToMode(destination.route)
      if (modeId != null) {
        onEvent(ShellUiEvent.ModeSelected(modeId))
      }
    }
    is CommandDestination.OpenRightPanel ->
      onEvent(ShellUiEvent.ToggleRightDrawer(destination.panel))
    CommandDestination.None -> Unit
  }
}

internal fun handleShellShortcuts(
  event: KeyEvent,
  paletteVisible: Boolean,
  onEvent: (ShellUiEvent) -> Unit,
): Boolean {
  if (event.type != KeyEventType.KeyDown) return false
  return when {
    event.key == Key.K && event.isCtrlPressed -> {
      onEvent(ShellUiEvent.ShowCommandPalette(PaletteSource.KEYBOARD_SHORTCUT))
      true
    }
    event.key == Key.Escape && paletteVisible -> {
      onEvent(ShellUiEvent.HideCommandPalette(PaletteDismissReason.BACK_PRESSED))
      true
    }
    else -> false
  }
}

private fun routeToMode(route: String): ModeId? = route.substringBefore('/').toModeIdOrNull()
