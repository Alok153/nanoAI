package com.vjaykrsna.nanoai.feature.uiux.ui.shell

import com.vjaykrsna.nanoai.feature.uiux.presentation.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellLayoutState

internal fun ShellLayoutState.closeRightDrawerIfNeeded(onEvent: (ShellUiEvent) -> Unit) {
  if (isRightDrawerOpen) {
    onEvent(ShellUiEvent.ToggleRightDrawer(activePanelOrDefault()))
  }
}

internal fun ShellLayoutState.activePanelOrDefault(): RightPanel =
  activeRightPanel ?: RightPanel.MODEL_SELECTOR
