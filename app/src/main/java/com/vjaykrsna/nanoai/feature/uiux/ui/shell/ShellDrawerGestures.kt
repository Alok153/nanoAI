package com.vjaykrsna.nanoai.feature.uiux.ui.shell

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.state.ShellLayoutState

private enum class DrawerSwipeAction {
  OpenLeft,
  CloseLeft,
  OpenRight,
  CloseRight,
  CloseRightOpenLeft,
}

internal fun Modifier.shellDrawerGestures(
  layout: ShellLayoutState,
  thresholds: DrawerGestureThresholds,
  onEvent: (ShellUiEvent) -> Unit,
): Modifier {
  val allowLeftGestures = layout.useModalNavigation
  val allowRightGestures = !layout.supportsRightRail
  if (!allowLeftGestures && !allowRightGestures) return this

  return pointerInput(
    allowLeftGestures,
    allowRightGestures,
    layout,
    thresholds,
    onEvent,
  ) {
    handleShellDrawerGestures(
      allowLeftGestures = allowLeftGestures,
      allowRightGestures = allowRightGestures,
      thresholds = thresholds,
      layoutProvider = { layout },
      onEvent = onEvent,
    )
  }
}

@Composable
internal fun rememberShellDrawerThresholds(): DrawerGestureThresholds {
  val density = LocalDensity.current
  return remember(density) {
    with(density) {
      DrawerGestureThresholds(
        edge = 36.dp.toPx(),
        panelWidth = 320.dp.toPx(),
        open = 56.dp.toPx(),
        close = 32.dp.toPx(),
      )
    }
  }
}

private suspend fun PointerInputScope.handleShellDrawerGestures(
  allowLeftGestures: Boolean,
  allowRightGestures: Boolean,
  thresholds: DrawerGestureThresholds,
  layoutProvider: () -> ShellLayoutState,
  onEvent: (ShellUiEvent) -> Unit,
) {
  var gesture: DrawerSwipeAction? = null
  var totalDrag = 0f

  detectHorizontalDragGestures(
    onDragStart = { offset: Offset ->
      val snapshot = layoutProvider()
      gesture =
        determineGesture(
          snapshot = snapshot,
          allowLeftGestures = allowLeftGestures,
          allowRightGestures = allowRightGestures,
          thresholds = thresholds,
          startX = offset.x,
          containerWidth = size.width.toFloat(),
        )
      totalDrag = 0f
    },
    onHorizontalDrag = { change, dragAmount ->
      val activeGesture = gesture
      if (activeGesture != null) {
        totalDrag += dragAmount
        @Suppress("DEPRECATION") change.consumePositionChange()
      }
    },
    onDragCancel = { gesture = null },
    onDragEnd = {
      gesture?.handleDragEnd(
        totalDrag = totalDrag,
        snapshot = layoutProvider(),
        thresholds = thresholds,
        onEvent = onEvent,
      )
      gesture = null
    },
  )
}

private fun determineGesture(
  snapshot: ShellLayoutState,
  allowLeftGestures: Boolean,
  allowRightGestures: Boolean,
  thresholds: DrawerGestureThresholds,
  startX: Float,
  containerWidth: Float,
): DrawerSwipeAction? {
  val leftEdge = startX <= thresholds.edge
  val rightEdge = startX >= containerWidth - thresholds.edge
  val touchesRightPanel = startX >= containerWidth - thresholds.panelWidth
  val touchesLeftPanel = startX <= thresholds.panelWidth

  if (allowRightGestures && snapshot.isRightDrawerOpen) {
    if (leftEdge) return DrawerSwipeAction.CloseRightOpenLeft
    if (rightEdge || touchesRightPanel) return DrawerSwipeAction.CloseRight
  }
  if (allowRightGestures && !snapshot.isRightDrawerOpen && rightEdge) {
    return DrawerSwipeAction.OpenRight
  }
  if (allowLeftGestures && snapshot.isLeftDrawerOpen && (leftEdge || touchesLeftPanel)) {
    return DrawerSwipeAction.CloseLeft
  }
  if (allowLeftGestures && !snapshot.isLeftDrawerOpen && leftEdge) {
    return DrawerSwipeAction.OpenLeft
  }
  return null
}

private fun DrawerSwipeAction.handleDragEnd(
  totalDrag: Float,
  snapshot: ShellLayoutState,
  thresholds: DrawerGestureThresholds,
  onEvent: (ShellUiEvent) -> Unit,
) {
  when (this) {
    DrawerSwipeAction.OpenLeft -> handleOpenLeft(totalDrag, snapshot, thresholds, onEvent)
    DrawerSwipeAction.CloseLeft -> handleCloseLeft(totalDrag, snapshot, thresholds, onEvent)
    DrawerSwipeAction.OpenRight -> handleOpenRight(totalDrag, snapshot, thresholds, onEvent)
    DrawerSwipeAction.CloseRight -> handleCloseRight(totalDrag, snapshot, thresholds, onEvent)
    DrawerSwipeAction.CloseRightOpenLeft ->
      handleCloseRightOpenLeft(totalDrag, snapshot, thresholds, onEvent)
  }
}

private fun ShellLayoutState.closeRightDrawerIfNeeded(onEvent: (ShellUiEvent) -> Unit) {
  if (isRightDrawerOpen) {
    onEvent(ShellUiEvent.ToggleRightDrawer(activePanelOrDefault()))
  }
}

private fun ShellLayoutState.activePanelOrDefault(): RightPanel =
  activeRightPanel ?: RightPanel.MODEL_SELECTOR

private fun handleOpenLeft(
  totalDrag: Float,
  snapshot: ShellLayoutState,
  thresholds: DrawerGestureThresholds,
  onEvent: (ShellUiEvent) -> Unit,
) {
  if (totalDrag > thresholds.open && !snapshot.isLeftDrawerOpen) {
    snapshot.closeRightDrawerIfNeeded(onEvent)
    onEvent(ShellUiEvent.ToggleLeftDrawer)
  }
}

private fun handleCloseLeft(
  totalDrag: Float,
  snapshot: ShellLayoutState,
  thresholds: DrawerGestureThresholds,
  onEvent: (ShellUiEvent) -> Unit,
) {
  if (totalDrag < -thresholds.close && snapshot.isLeftDrawerOpen) {
    onEvent(ShellUiEvent.ToggleLeftDrawer)
  }
}

private fun handleOpenRight(
  totalDrag: Float,
  snapshot: ShellLayoutState,
  thresholds: DrawerGestureThresholds,
  onEvent: (ShellUiEvent) -> Unit,
) {
  if (totalDrag < -thresholds.open) {
    if (snapshot.isLeftDrawerOpen) {
      onEvent(ShellUiEvent.ToggleLeftDrawer)
    }
    if (!snapshot.isRightDrawerOpen) {
      onEvent(ShellUiEvent.ToggleRightDrawer(snapshot.activePanelOrDefault()))
    }
  }
}

private fun handleCloseRight(
  totalDrag: Float,
  snapshot: ShellLayoutState,
  thresholds: DrawerGestureThresholds,
  onEvent: (ShellUiEvent) -> Unit,
) {
  if (totalDrag > thresholds.close && snapshot.isRightDrawerOpen) {
    onEvent(ShellUiEvent.ToggleRightDrawer(snapshot.activePanelOrDefault()))
  }
}

private fun handleCloseRightOpenLeft(
  totalDrag: Float,
  snapshot: ShellLayoutState,
  thresholds: DrawerGestureThresholds,
  onEvent: (ShellUiEvent) -> Unit,
) {
  snapshot.closeRightDrawerIfNeeded(onEvent)
  if (totalDrag > thresholds.open && !snapshot.isLeftDrawerOpen) {
    onEvent(ShellUiEvent.ToggleLeftDrawer)
  }
}

internal data class DrawerGestureThresholds(
  val edge: Float,
  val panelWidth: Float,
  val open: Float,
  val close: Float,
)
