package com.vjaykrsna.nanoai.shared.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.testTag
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandInvocationSource
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellUiState
import com.vjaykrsna.nanoai.feature.uiux.ui.commandpalette.CommandPaletteSheet

@Composable
internal fun ShellScaffoldLayout(
  state: ShellUiState,
  layout: ShellLayoutState,
  snackbarHostState: SnackbarHostState,
  focusRequester: FocusRequester,
  drawerState: DrawerState,
  dispatchEvent: (ShellUiEvent) -> Unit,
  onEvent: (ShellUiEvent) -> Unit,
  modeContent: @Composable (ModeId) -> Unit,
  modifier: Modifier = Modifier,
) {
  val containerModifier =
    modifier
      .fillMaxSize()
      .let {
        val thresholds = rememberShellDrawerThresholds()
        it.shellDrawerGestures(layout, thresholds, dispatchEvent)
      }
      .focusRequester(focusRequester)
      .onPreviewKeyEvent { event ->
        handleShellShortcuts(event, layout.isPaletteVisible, dispatchEvent)
      }
      .testTag("shell_root")

  Box(modifier = containerModifier) {
    ShellDrawerHost(
      state = state,
      layout = layout,
      drawerState = drawerState,
      snackbarHostState = snackbarHostState,
      dispatchEvent = dispatchEvent,
      onEvent = onEvent,
      modeContent = modeContent,
    )

    AnimatedVisibility(
      visible = layout.isPaletteVisible,
      enter = fadeIn(animationSpec = tween(durationMillis = 120, easing = FastOutLinearInEasing)),
      exit = fadeOut(animationSpec = tween(durationMillis = 100, easing = LinearOutSlowInEasing)),
    ) {
      CommandPaletteSheet(
        state = state.commandPalette,
        onDismissRequest = { reason -> dispatchEvent(ShellUiEvent.HideCommandPalette(reason)) },
        onCommandSelect = { action ->
          handleCommandAction(action, CommandInvocationSource.PALETTE, dispatchEvent)
        },
        modifier = Modifier.fillMaxSize(),
      )
    }

    AnimatedVisibility(
      visible = layout.showCoverageDashboard,
      enter = fadeIn(animationSpec = tween(durationMillis = 120, easing = FastOutLinearInEasing)),
      exit = fadeOut(animationSpec = tween(durationMillis = 100, easing = LinearOutSlowInEasing)),
    ) {
      CoverageDashboardOverlay(
        onDismiss = { dispatchEvent(ShellUiEvent.HideCoverageDashboard) },
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

@Composable
internal fun ShellDrawerHost(
  state: ShellUiState,
  layout: ShellLayoutState,
  drawerState: DrawerState,
  snackbarHostState: SnackbarHostState,
  dispatchEvent: (ShellUiEvent) -> Unit,
  onEvent: (ShellUiEvent) -> Unit,
  modeContent: @Composable (ModeId) -> Unit,
) {
  val layoutState = rememberUpdatedState(layout)
  val drawerEventHandler =
    remember(dispatchEvent, onEvent) {
      { drawerEvent: ShellDrawerEvent ->
        when (drawerEvent) {
          is ShellDrawerEvent.ModeSelected ->
            dispatchEvent(ShellUiEvent.ModeSelected(drawerEvent.modeId))
          is ShellDrawerEvent.ShowCommandPalette ->
            dispatchEvent(ShellUiEvent.ShowCommandPalette(drawerEvent.source))
          ShellDrawerEvent.CloseDrawer -> handleCloseLeftDrawer(layoutState.value, onEvent)
        }
      }
    }

  val rightRailContent: @Composable () -> Unit = {
    ShellRightRailHost(
      state = state,
      snackbarHostState = snackbarHostState,
      onEvent = dispatchEvent,
      modeContent = modeContent,
      originalOnEvent = onEvent,
    )
  }

  if (layout.usesPermanentLeftDrawer) {
    PermanentNavigationDrawer(
      drawerContent = {
        ShellDrawerContent(
          variant = DrawerVariant.Permanent,
          activeMode = layout.activeMode,
          onEvent = drawerEventHandler,
        )
      },
      modifier = Modifier.testTag("left_drawer_permanent"),
    ) {
      rightRailContent()
    }
  } else {
    ModalNavigationDrawer(
      drawerContent = {
        ShellDrawerContent(
          variant = DrawerVariant.Modal,
          activeMode = layout.activeMode,
          onEvent = drawerEventHandler,
        )
      },
      drawerState = drawerState,
      gesturesEnabled = layout.useModalNavigation && !layout.isRightDrawerOpen,
      modifier = Modifier.testTag("left_drawer_modal"),
    ) {
      rightRailContent()
    }
  }
}
