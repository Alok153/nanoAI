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
import com.vjaykrsna.nanoai.feature.uiux.state.ShellLayoutState

private enum class DrawerSwipeAction {
  OpenLeft,
  CloseLeft,
  OpenRight,
  CloseRight,
  CloseRightOpenLeft,
}

private data class DrawerGestureContext(
  val snapshot: ShellLayoutState,
  val allowLeftGestures: Boolean,
  val allowRightGestures: Boolean,
  val thresholds: DrawerGestureThresholds,
  val startX: Float,
  val containerWidth: Float,
)

internal fun Modifier.shellDrawerGestures(
  layout: ShellLayoutState,
  thresholds: DrawerGestureThresholds,
  onEvent: (ShellUiEvent) -> Unit,
): Modifier {
  val allowLeftGestures = layout.useModalNavigation
  val allowRightGestures = !layout.supportsRightRail
  if (!allowLeftGestures && !allowRightGestures) return this

  return pointerInput(allowLeftGestures, allowRightGestures, layout, thresholds, onEvent) {
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
          DrawerGestureContext(
            snapshot = snapshot,
            allowLeftGestures = allowLeftGestures,
            allowRightGestures = allowRightGestures,
            thresholds = thresholds,
            startX = offset.x,
            containerWidth = size.width.toFloat(),
          )
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

private fun determineGesture(context: DrawerGestureContext): DrawerSwipeAction? {
  val metrics =
    DrawerGestureMetrics(
      leftEdge = context.startX <= context.thresholds.edge,
      rightEdge = context.startX >= context.containerWidth - context.thresholds.edge,
      touchesRightPanel = context.startX >= context.containerWidth - context.thresholds.panelWidth,
      touchesLeftPanel = context.startX <= context.thresholds.panelWidth,
    )

  return context.detectRightDrawerGesture(metrics) ?: context.detectLeftDrawerGesture(metrics)
}

private data class DrawerGestureMetrics(
  val leftEdge: Boolean,
  val rightEdge: Boolean,
  val touchesRightPanel: Boolean,
  val touchesLeftPanel: Boolean,
)

private fun DrawerGestureContext.detectRightDrawerGesture(
  metrics: DrawerGestureMetrics
): DrawerSwipeAction? {
  if (!allowRightGestures) return null

  val closesFromLeft = snapshot.isRightDrawerOpen && metrics.leftEdge
  val closesFromRightEdge =
    snapshot.isRightDrawerOpen && (metrics.rightEdge || metrics.touchesRightPanel)
  val opensRight = !snapshot.isRightDrawerOpen && metrics.rightEdge

  return sequenceOf(
      closesFromLeft to DrawerSwipeAction.CloseRightOpenLeft,
      closesFromRightEdge to DrawerSwipeAction.CloseRight,
      opensRight to DrawerSwipeAction.OpenRight,
    )
    .firstOrNull { it.first }
    ?.second
}

private fun DrawerGestureContext.detectLeftDrawerGesture(
  metrics: DrawerGestureMetrics
): DrawerSwipeAction? {
  if (!allowLeftGestures) return null

  return when {
    snapshot.isLeftDrawerOpen && (metrics.leftEdge || metrics.touchesLeftPanel) ->
      DrawerSwipeAction.CloseLeft
    !snapshot.isLeftDrawerOpen && metrics.leftEdge -> DrawerSwipeAction.OpenLeft
    else -> null
  }
}

private fun handleLeftAction(
  action: DrawerSwipeAction,
  totalDrag: Float,
  snapshot: ShellLayoutState,
  thresholds: DrawerGestureThresholds,
  onEvent: (ShellUiEvent) -> Unit,
) {
  val shouldOpenLeft = totalDrag > thresholds.open && !snapshot.isLeftDrawerOpen
  val shouldCloseLeft = totalDrag < -thresholds.close && snapshot.isLeftDrawerOpen

  if (action == DrawerSwipeAction.CloseRightOpenLeft) {
    snapshot.closeRightDrawerIfNeeded(onEvent)
    if (shouldOpenLeft) {
      onEvent(ShellUiEvent.ToggleLeftDrawer)
    }
    return
  }

  if (action == DrawerSwipeAction.OpenLeft) {
    if (shouldOpenLeft) {
      snapshot.closeRightDrawerIfNeeded(onEvent)
      onEvent(ShellUiEvent.ToggleLeftDrawer)
    }
    return
  }

  if (action == DrawerSwipeAction.CloseLeft && shouldCloseLeft) {
    onEvent(ShellUiEvent.ToggleLeftDrawer)
  }
}

private fun handleRightAction(
  action: DrawerSwipeAction,
  totalDrag: Float,
  snapshot: ShellLayoutState,
  thresholds: DrawerGestureThresholds,
  onEvent: (ShellUiEvent) -> Unit,
) {
  when (action) {
    DrawerSwipeAction.OpenRight -> {
      if (totalDrag < -thresholds.open) {
        if (snapshot.isLeftDrawerOpen) {
          onEvent(ShellUiEvent.ToggleLeftDrawer)
        }
        if (!snapshot.isRightDrawerOpen) {
          onEvent(ShellUiEvent.ToggleRightDrawer(snapshot.activePanelOrDefault()))
        }
      }
    }
    DrawerSwipeAction.CloseRight -> {
      if (totalDrag > thresholds.close && snapshot.isRightDrawerOpen) {
        onEvent(ShellUiEvent.ToggleRightDrawer(snapshot.activePanelOrDefault()))
      }
    }
    else -> Unit
  }
}

private fun DrawerSwipeAction.handleDragEnd(
  totalDrag: Float,
  snapshot: ShellLayoutState,
  thresholds: DrawerGestureThresholds,
  onEvent: (ShellUiEvent) -> Unit,
) {
  when (this) {
    DrawerSwipeAction.OpenLeft,
    DrawerSwipeAction.CloseLeft,
    DrawerSwipeAction.CloseRightOpenLeft ->
      handleLeftAction(this, totalDrag, snapshot, thresholds, onEvent)
    DrawerSwipeAction.OpenRight,
    DrawerSwipeAction.CloseRight ->
      handleRightAction(this, totalDrag, snapshot, thresholds, onEvent)
  }
}

internal data class DrawerGestureThresholds(
  val edge: Float,
  val panelWidth: Float,
  val open: Float,
  val close: Float,
)
