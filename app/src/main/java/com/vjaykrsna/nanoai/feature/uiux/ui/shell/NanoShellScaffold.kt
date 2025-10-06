package com.vjaykrsna.nanoai.feature.uiux.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellUiState
import com.vjaykrsna.nanoai.feature.uiux.state.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.state.CommandDestination
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.state.ModeCard
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.state.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload
import com.vjaykrsna.nanoai.feature.uiux.state.toModeIdOrNull
import com.vjaykrsna.nanoai.feature.uiux.ui.HomeScreen
import com.vjaykrsna.nanoai.feature.uiux.ui.commandpalette.CommandPaletteSheet
import com.vjaykrsna.nanoai.feature.uiux.ui.components.ConnectivityBanner
import com.vjaykrsna.nanoai.feature.uiux.ui.sidebar.RightSidebarPanels
import java.util.Locale
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
  val drawerState =
    rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
  val currentOnEvent by rememberUpdatedState(newValue = onEvent)

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

  LaunchedEffect(drawerState, layout.useModalNavigation, layout.isLeftDrawerOpen) {
    if (!layout.useModalNavigation) return@LaunchedEffect
    snapshotFlow { drawerState.currentValue }
      .collect { value ->
        val isOpen = value == androidx.compose.material3.DrawerValue.Open
        if (isOpen != layout.isLeftDrawerOpen) {
          currentOnEvent(ShellUiEvent.ToggleLeftDrawer)
        }
      }
  }

  LaunchedEffect(layout.pendingUndoAction) {
    val payload = layout.pendingUndoAction ?: return@LaunchedEffect
    val message = payload.metadata["message"] as? String ?: "Action completed"
    val result =
      snackbarHostState.showSnackbar(
        message = message,
        actionLabel = "Undo",
      )
    if (result == SnackbarResult.ActionPerformed) {
      currentOnEvent(ShellUiEvent.Undo(payload))
    }
  }

  LaunchedEffect(Unit) { focusRequester.requestFocus() }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .focusRequester(focusRequester)
        .onPreviewKeyEvent { event ->
          handleShellShortcuts(event, layout.isPaletteVisible, onEvent)
        }
        .testTag("shell_root"),
  ) {
    if (layout.usesPermanentLeftDrawer) {
      PermanentNavigationDrawer(
        drawerContent = {
          ShellDrawerContent(
            variant = DrawerVariant.Permanent,
            modeCards = state.modeCards,
            activeMode = layout.activeMode,
            onModeSelect = { modeId -> onEvent(ShellUiEvent.ModeSelected(modeId)) },
            onOpenCommandPalette = {
              onEvent(ShellUiEvent.ShowCommandPalette(PaletteSource.TOP_APP_BAR))
            },
          )
        },
        modifier = Modifier.testTag("left_drawer_permanent"),
      ) {
        ShellRightRailHost(
          state = state,
          snackbarHostState = snackbarHostState,
          onEvent = onEvent,
          modeContent = modeContent,
        )
      }
    } else {
      ModalNavigationDrawer(
        drawerContent = {
          ShellDrawerContent(
            variant = DrawerVariant.Modal,
            modeCards = state.modeCards,
            activeMode = layout.activeMode,
            onModeSelect = { modeId -> onEvent(ShellUiEvent.ModeSelected(modeId)) },
            onOpenCommandPalette = {
              onEvent(ShellUiEvent.ShowCommandPalette(PaletteSource.TOP_APP_BAR))
            },
          )
        },
        drawerState = drawerState,
        gesturesEnabled = layout.useModalNavigation,
        modifier = Modifier.testTag("left_drawer_modal"),
      ) {
        ShellRightRailHost(
          state = state,
          snackbarHostState = snackbarHostState,
          onEvent = onEvent,
          modeContent = modeContent,
        )
      }
    }

    AnimatedVisibility(
      visible = layout.isPaletteVisible,
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      CommandPaletteSheet(
        state = state.commandPalette,
        onDismissRequest = { onEvent(ShellUiEvent.HideCommandPalette) },
        onCommandSelect = { action -> handleCommandAction(action, onEvent) },
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
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

  data class UpdateTheme(val theme: ThemePreference) : ShellUiEvent

  data class UpdateDensity(val density: VisualDensity) : ShellUiEvent
}

private enum class DrawerVariant {
  Modal,
  Permanent
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellDrawerContent(
  variant: DrawerVariant,
  modeCards: List<ModeCard>,
  activeMode: ModeId,
  onModeSelect: (ModeId) -> Unit,
  onOpenCommandPalette: () -> Unit,
) {
  when (variant) {
    DrawerVariant.Modal ->
      ModalDrawerSheet {
        DrawerSheetContent(
          modeCards = modeCards,
          activeMode = activeMode,
          onModeSelect = onModeSelect,
          onOpenCommandPalette = onOpenCommandPalette,
        )
      }
    DrawerVariant.Permanent ->
      PermanentDrawerSheet {
        DrawerSheetContent(
          modeCards = modeCards,
          activeMode = activeMode,
          onModeSelect = onModeSelect,
          onOpenCommandPalette = onOpenCommandPalette,
        )
      }
  }
}

@Composable
private fun DrawerSheetContent(
  modeCards: List<ModeCard>,
  activeMode: ModeId,
  onModeSelect: (ModeId) -> Unit,
  onOpenCommandPalette: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text = "Navigate",
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.testTag("drawer_header"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      modeCards.forEach { card ->
        DrawerModeItem(
          modeCard = card,
          selected = card.id == activeMode,
          onClick = { onModeSelect(card.id) },
        )
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Surface(tonalElevation = 1.dp) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("drawer_command_palette"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Outlined.Search, contentDescription = null)
        Column(modifier = Modifier.weight(1f)) {
          Text("Command palette", style = MaterialTheme.typography.titleSmall)
          Text("Ctrl+K", style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onOpenCommandPalette) {
          Icon(Icons.Outlined.MoreVert, contentDescription = "Open command palette")
        }
      }
    }
  }
}

@Composable
private fun DrawerModeItem(
  modeCard: ModeCard,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    tonalElevation = if (selected) 6.dp else 0.dp,
    modifier =
      Modifier.fillMaxWidth().testTag("drawer_mode_${modeCard.id.name.lowercase(Locale.ROOT)}"),
  ) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
      Text(text = modeCard.title, style = MaterialTheme.typography.titleSmall)
      modeCard.subtitle?.let { subtitle ->
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}

@Composable
private fun ShellRightRailHost(
  state: ShellUiState,
  snackbarHostState: SnackbarHostState,
  onEvent: (ShellUiEvent) -> Unit,
  modeContent: @Composable (ModeId) -> Unit,
) {
  Row(modifier = Modifier.fillMaxSize()) {
    ShellMainSurface(
      state = state,
      snackbarHostState = snackbarHostState,
      onEvent = onEvent,
      modeContent = modeContent,
      modifier = Modifier.weight(1f),
    )

    val layout = state.layout
    val showPermanentRail = layout.supportsRightRail
    val showFloatingDrawer = !showPermanentRail && layout.isRightDrawerOpen

    if (showPermanentRail) {
      Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxHeight().width(320.dp).testTag("right_sidebar_permanent"),
      ) {
        RightSidebarPanels(
          state = state,
          onEvent = onEvent,
          modifier = Modifier.fillMaxSize(),
        )
      }
    } else {
      AnimatedVisibility(
        visible = showFloatingDrawer,
        enter = slideInHorizontally { fullWidth -> fullWidth / 2 },
        exit = slideOutHorizontally { fullWidth -> fullWidth / 2 },
      ) {
        Surface(
          tonalElevation = 6.dp,
          modifier = Modifier.fillMaxHeight().width(320.dp).testTag("right_sidebar_modal"),
        ) {
          RightSidebarPanels(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellMainSurface(
  state: ShellUiState,
  snackbarHostState: SnackbarHostState,
  onEvent: (ShellUiEvent) -> Unit,
  modeContent: @Composable (ModeId) -> Unit,
  modifier: Modifier = Modifier,
) {
  val layout = state.layout
  Scaffold(
    modifier = modifier,
    topBar = {
      ShellTopAppBar(
        layout = layout,
        onToggleLeftDrawer = { onEvent(ShellUiEvent.ToggleLeftDrawer) },
        onToggleRightDrawer = { panel -> onEvent(ShellUiEvent.ToggleRightDrawer(panel)) },
        onShowCommandPalette = { source -> onEvent(ShellUiEvent.ShowCommandPalette(source)) },
      )
    },
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("shell_content"),
      verticalArrangement = Arrangement.Top,
    ) {
      val bannerState = state.connectivityBanner
      if (!layout.isPaletteVisible && bannerState.isVisible) {
        ConnectivityBanner(
          state = bannerState,
          onCtaClick = { bannerState.cta?.let { action -> handleCommandAction(action, onEvent) } },
          onDismiss = {
            // Persist dismissal once the repository exposes the corresponding event.
          },
          modifier = Modifier.fillMaxWidth().testTag("connectivity_banner"),
        )
      }

      when (layout.activeMode) {
        ModeId.HOME ->
          HomeScreen(
            layout = layout,
            modeCards = state.modeCards,
            quickActions = state.quickActions,
            recentActivity = layout.recentActivity,
            onModeSelect = { modeId -> onEvent(ShellUiEvent.ModeSelected(modeId)) },
            onQuickActionSelect = { action -> handleCommandAction(action, onEvent) },
            onRecentActivitySelect = { item -> onEvent(ShellUiEvent.ModeSelected(item.modeId)) },
            modifier = Modifier.fillMaxSize(),
          )
        else -> modeContent(layout.activeMode)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellTopAppBar(
  layout: ShellLayoutState,
  onToggleLeftDrawer: () -> Unit,
  onToggleRightDrawer: (RightPanel) -> Unit,
  onShowCommandPalette: (PaletteSource) -> Unit,
) {
  TopAppBar(
    title = {
      Text(
        text =
          layout.activeMode.name.lowercase(Locale.ROOT).replaceFirstChar {
            it.titlecase(Locale.ROOT)
          },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    },
    navigationIcon = {
      if (layout.useModalNavigation) {
        IconButton(onClick = onToggleLeftDrawer, modifier = Modifier.testTag("topbar_nav_icon")) {
          Icon(Icons.Outlined.Menu, contentDescription = "Toggle navigation drawer")
        }
      }
    },
    actions = {
      IconButton(
        onClick = { onShowCommandPalette(PaletteSource.TOP_APP_BAR) },
        modifier = Modifier.testTag("topbar_command_palette"),
      ) {
        Icon(Icons.Outlined.Search, contentDescription = "Open command palette")
      }
      val activeJobs = layout.progressJobs.count { !it.isTerminal }
      IconButton(
        onClick = { onToggleRightDrawer(RightPanel.PROGRESS_CENTER) },
        modifier = Modifier.testTag("topbar_progress_center"),
      ) {
        if (activeJobs > 0) {
          BadgedBox(badge = { Badge { Text(activeJobs.toString()) } }) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Open progress center")
          }
        } else {
          Icon(Icons.Rounded.Download, contentDescription = "Open progress center")
        }
      }
      IconButton(
        onClick = { onToggleRightDrawer(RightPanel.MODEL_SELECTOR) },
        modifier = Modifier.testTag("topbar_model_selector"),
      ) {
        Icon(Icons.Outlined.Tune, contentDescription = "Open model selector")
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(),
  )
}

private fun handleCommandAction(action: CommandAction, onEvent: (ShellUiEvent) -> Unit) {
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

private fun routeToMode(route: String): ModeId? = route.substringBefore('/').toModeIdOrNull()

private fun handleShellShortcuts(
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
      onEvent(ShellUiEvent.HideCommandPalette)
      true
    }
    else -> false
  }
}
