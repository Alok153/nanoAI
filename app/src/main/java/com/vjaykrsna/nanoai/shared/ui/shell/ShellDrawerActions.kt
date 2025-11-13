package com.vjaykrsna.nanoai.shared.ui.shell

import com.vjaykrsna.nanoai.core.domain.model.uiux.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellLayoutState

internal fun handleToggleLeftDrawer(layout: ShellLayoutState, onEvent: (ShellUiEvent) -> Unit) {
  if (!layout.canToggleLeftDrawer) return
  if (layout.isRightDrawerOpen) {
    handleCloseRightDrawer(layout, onEvent)
  }
  onEvent(ShellUiEvent.ToggleLeftDrawer)
}

internal fun handleToggleRightDrawer(
  layout: ShellLayoutState,
  panel: RightPanel,
  onEvent: (ShellUiEvent) -> Unit,
) {
  if (layout.isLeftDrawerOpen) {
    handleCloseLeftDrawer(layout, onEvent)
  }
  onEvent(ShellUiEvent.ToggleRightDrawer(panel))
}

internal fun handleCloseLeftDrawer(layout: ShellLayoutState, onEvent: (ShellUiEvent) -> Unit) {
  if (!layout.canToggleLeftDrawer) return
  if (layout.isLeftDrawerOpen) {
    onEvent(ShellUiEvent.ToggleLeftDrawer)
  }
}

internal fun handleCloseRightDrawer(layout: ShellLayoutState, onEvent: (ShellUiEvent) -> Unit) {
  if (!layout.isRightDrawerOpen) return
  val panel = layout.activeRightPanel ?: RightPanel.MODEL_SELECTOR
  onEvent(ShellUiEvent.ToggleRightDrawer(panel))
}
