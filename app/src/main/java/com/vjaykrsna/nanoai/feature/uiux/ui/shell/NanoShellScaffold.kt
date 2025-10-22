package com.vjaykrsna.nanoai.feature.uiux.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.R
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellUiState
import com.vjaykrsna.nanoai.feature.uiux.state.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.state.CommandDestination
import com.vjaykrsna.nanoai.feature.uiux.state.CommandInvocationSource
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.PaletteDismissReason
import com.vjaykrsna.nanoai.feature.uiux.state.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload
import com.vjaykrsna.nanoai.feature.uiux.state.toModeIdOrNull
import com.vjaykrsna.nanoai.feature.uiux.ui.HomeScreen
import com.vjaykrsna.nanoai.feature.uiux.ui.commandpalette.CommandPaletteSheet
import com.vjaykrsna.nanoai.feature.uiux.ui.components.ConnectivityBanner
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoElevation
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
  /**
   * Architecture Overview: NanoShellScaffold is the root container for the unified shell
   * experience. It orchestrates:
   * 1. Left Navigation Drawer (modal or permanent based on layout)
   * 2. Right Panels (model selector, progress, etc.)
   * 3. Command Palette overlay
   * 4. Responsive layout adapting to window size class
   *
   * The main composable is focused on:
   * - State management and synchronization with Material3 drawer
   * - Keyboard shortcuts handling
   * - Event dispatch orchestration
   * - Conditional UI rendering based on layout
   *
   * Child composables handle specific UI sections:
   * - ShellDrawerContent: Navigation drawer UI
   * - ShellRightRailHost: Right panels and main content area
   * - CommandPaletteSheet: Command palette overlay
   */
  val layout = state.layout
  val snackbarHostState = remember { SnackbarHostState() }
  val focusRequester = remember { FocusRequester() }
  val drawerState =
    rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
  val currentOnEvent by rememberUpdatedState(newValue = onEvent)
  val latestLeftDrawerOpen by rememberUpdatedState(layout.isLeftDrawerOpen)

  fun closeLeftDrawerIfOpen() {
    if (!layout.canToggleLeftDrawer) return
    if (layout.isLeftDrawerOpen) {
      currentOnEvent(ShellUiEvent.ToggleLeftDrawer)
    }
  }

  fun closeRightDrawerIfOpen() {
    if (layout.isRightDrawerOpen) {
      val panel = layout.activeRightPanel ?: RightPanel.MODEL_SELECTOR
      currentOnEvent(ShellUiEvent.ToggleRightDrawer(panel))
    }
  }

  fun toggleLeftDrawerWithRules() {
    if (!layout.canToggleLeftDrawer) return
    if (layout.isRightDrawerOpen) {
      closeRightDrawerIfOpen()
    }
    currentOnEvent(ShellUiEvent.ToggleLeftDrawer)
  }

  fun toggleRightDrawerWithRules(panel: RightPanel) {
    if (layout.isLeftDrawerOpen) {
      closeLeftDrawerIfOpen()
    }
    currentOnEvent(ShellUiEvent.ToggleRightDrawer(panel))
  }

  val dispatchEvent: (ShellUiEvent) -> Unit = { event ->
    when (event) {
      ShellUiEvent.ToggleLeftDrawer -> toggleLeftDrawerWithRules()
      is ShellUiEvent.ToggleRightDrawer -> toggleRightDrawerWithRules(event.panel)
      is ShellUiEvent.ShowCommandPalette -> {
        closeLeftDrawerIfOpen()
        closeRightDrawerIfOpen()
        currentOnEvent(event)
      }
      is ShellUiEvent.ModeSelected -> {
        closeLeftDrawerIfOpen()
        closeRightDrawerIfOpen()
        currentOnEvent(event)
      }
      else -> currentOnEvent(event)
    }
  }

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
        val isOpen = value == androidx.compose.material3.DrawerValue.Open
        if (isOpen != latestLeftDrawerOpen) {
          currentOnEvent(ShellUiEvent.SetLeftDrawer(isOpen))
        }
      }
  }

  LaunchedEffect(layout.pendingUndoAction) {
    val payload = layout.pendingUndoAction ?: return@LaunchedEffect
    val message = payload.metadata["message"] as? String ?: "Action completed"
    val result = snackbarHostState.showSnackbar(message = message, actionLabel = "Undo")
    if (result == SnackbarResult.ActionPerformed) {
      currentOnEvent(ShellUiEvent.Undo(payload))
    }
  }

  LaunchedEffect(Unit) { focusRequester.requestFocus() }
  LaunchedEffect(layout.isPaletteVisible) {
    if (!layout.isPaletteVisible) {
      focusRequester.requestFocus()
    }
  }

  Box(
    modifier =
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
  ) {
    if (layout.usesPermanentLeftDrawer) {
      PermanentNavigationDrawer(
        drawerContent = {
          ShellDrawerContent(
            variant = DrawerVariant.Permanent,
            activeMode = layout.activeMode,
            onModeSelect = { modeId -> dispatchEvent(ShellUiEvent.ModeSelected(modeId)) },
            onOpenCommandPalette = {
              dispatchEvent(ShellUiEvent.ShowCommandPalette(PaletteSource.TOP_APP_BAR))
            },
          )
        },
        modifier = Modifier.testTag("left_drawer_permanent"),
      ) {
        ShellRightRailHost(
          state = state,
          snackbarHostState = snackbarHostState,
          onEvent = dispatchEvent,
          modeContent = modeContent,
          originalOnEvent = onEvent,
        )
      }
    } else {
      ModalNavigationDrawer(
        drawerContent = {
          ShellDrawerContent(
            variant = DrawerVariant.Modal,
            activeMode = layout.activeMode,
            onModeSelect = { modeId -> dispatchEvent(ShellUiEvent.ModeSelected(modeId)) },
            onOpenCommandPalette = {
              dispatchEvent(ShellUiEvent.ShowCommandPalette(PaletteSource.TOP_APP_BAR))
            },
            onCloseDrawer = { closeLeftDrawerIfOpen() },
          )
        },
        drawerState = drawerState,
        gesturesEnabled = layout.useModalNavigation && !layout.isRightDrawerOpen,
        modifier = Modifier.testTag("left_drawer_modal"),
      ) {
        ShellRightRailHost(
          state = state,
          snackbarHostState = snackbarHostState,
          onEvent = dispatchEvent,
          modeContent = modeContent,
          originalOnEvent = onEvent,
        )
      }
    }

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
  }
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

  data class ChatPersonaSelected(
    val personaId: java.util.UUID,
    val action: com.vjaykrsna.nanoai.core.model.PersonaSwitchAction,
  ) : ShellUiEvent

  data object ChatTitleClicked : ShellUiEvent
}

private enum class DrawerVariant {
  Modal,
  Permanent,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellDrawerContent(
  variant: DrawerVariant,
  activeMode: ModeId,
  onModeSelect: (ModeId) -> Unit,
  onOpenCommandPalette: () -> Unit,
  onCloseDrawer: (() -> Unit)? = null,
) {
  when (variant) {
    DrawerVariant.Modal ->
      ModalDrawerSheet {
        DrawerSheetContent(
          activeMode = activeMode,
          onModeSelect = onModeSelect,
          onOpenCommandPalette = onOpenCommandPalette,
          onCloseDrawer = onCloseDrawer,
        )
      }
    DrawerVariant.Permanent ->
      PermanentDrawerSheet {
        DrawerSheetContent(
          activeMode = activeMode,
          onModeSelect = onModeSelect,
          onOpenCommandPalette = onOpenCommandPalette,
          onCloseDrawer = onCloseDrawer,
        )
      }
  }
}

@Composable
private fun DrawerSheetContent(
  activeMode: ModeId,
  onModeSelect: (ModeId) -> Unit,
  onOpenCommandPalette: () -> Unit,
  onCloseDrawer: (() -> Unit)? = null,
) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Search/Command palette at the top
    Surface(
      tonalElevation = NanoElevation.level1,
      shape = RoundedCornerShape(12.dp),
      onClick = {
        onOpenCommandPalette()
        onCloseDrawer?.invoke()
      },
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("drawer_command_palette"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Outlined.Search, contentDescription = null)
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            stringResource(R.string.nano_shell_search),
            style = MaterialTheme.typography.titleSmall,
          )
          Text(
            "Ctrl+K",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
          )
        }
      }
    }

    // Navigation items
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      // Home button
      DrawerNavigationItem(
        icon = Icons.Outlined.Home,
        title = "Home",
        selected = activeMode == ModeId.HOME,
        onClick = {
          onModeSelect(ModeId.HOME)
          onCloseDrawer?.invoke()
        },
      )

      // Settings button
      DrawerNavigationItem(
        icon = Icons.Outlined.Settings,
        title = "Settings",
        selected = activeMode == ModeId.SETTINGS,
        onClick = {
          onModeSelect(ModeId.SETTINGS)
          onCloseDrawer?.invoke()
        },
      )

      // Chat History button
      DrawerNavigationItem(
        icon = Icons.Default.History,
        title = "Chat History",
        selected = activeMode == ModeId.HISTORY,
        onClick = {
          onModeSelect(ModeId.HISTORY)
          onCloseDrawer?.invoke()
        },
      )

      // Mode cards removed intentionally - only Home, Settings, and Chat History required
    }
  }
}

@Composable
private fun DrawerNavigationItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    tonalElevation = if (selected) NanoElevation.level3 else NanoElevation.level0,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth().testTag("drawer_nav_${title.lowercase()}"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun ShellRightRailHost(
  state: ShellUiState,
  snackbarHostState: SnackbarHostState,
  onEvent: (ShellUiEvent) -> Unit,
  modeContent: @Composable (ModeId) -> Unit,
  originalOnEvent: (ShellUiEvent) -> Unit = onEvent,
) {
  val layout = state.layout
  if (layout.supportsRightRail) {
    Row(modifier = Modifier.fillMaxSize()) {
      ShellMainSurface(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = onEvent,
        modeContent = modeContent,
        modifier = Modifier.weight(1f),
        originalOnEvent = originalOnEvent,
      )

      Surface(
        tonalElevation = NanoElevation.level2,
        modifier = Modifier.fillMaxHeight().width(320.dp).testTag("right_sidebar_permanent"),
      ) {
        RightSidebarPanels(state = state, onEvent = onEvent, modifier = Modifier.fillMaxSize())
      }
    }
  } else {
    val parentLayoutDirection = LocalLayoutDirection.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        CompositionLocalProvider(LocalLayoutDirection provides parentLayoutDirection) {
          ShellMainSurface(
            state = state,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent,
            modeContent = modeContent,
            modifier = Modifier.fillMaxSize().align(Alignment.TopStart),
            originalOnEvent = originalOnEvent,
          )
        }

        AnimatedVisibility(
          visible = layout.isRightDrawerOpen,
          enter =
            slideInHorizontally(
              animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
            ) { drawerWidth ->
              drawerWidth
            },
          exit =
            slideOutHorizontally(
              animationSpec = tween(durationMillis = 160, easing = LinearOutSlowInEasing)
            ) { drawerWidth ->
              drawerWidth
            },
        ) {
          Surface(
            tonalElevation = NanoElevation.level3,
            modifier =
              Modifier.align(Alignment.TopEnd)
                .fillMaxHeight()
                .width(320.dp)
                .testTag("right_sidebar_modal"),
          ) {
            RightSidebarPanels(state = state, onEvent = onEvent, modifier = Modifier.fillMaxSize())
          }
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
  originalOnEvent: (ShellUiEvent) -> Unit = onEvent,
) {
  val layout = state.layout
  Scaffold(
    modifier = modifier,
    topBar = {
      ShellTopAppBar(
        state = state,
        onToggleLeftDrawer = { originalOnEvent(ShellUiEvent.ToggleLeftDrawer) },
        onToggleRightDrawer = { panel -> originalOnEvent(ShellUiEvent.ToggleRightDrawer(panel)) },
        onEvent = onEvent,
      )
    },
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .testTag("shell_content")
          .semantics {
            contentDescription = "Main content area"
            stateDescription = layout.connectivityStatusDescription
          }
          .focusable(),
      verticalArrangement = Arrangement.Top,
    ) {
      val bannerState = state.connectivityBanner
      if (!layout.isPaletteVisible && bannerState.isVisible) {
        ConnectivityBanner(
          state = bannerState,
          onCtaClick = {
            bannerState.cta?.let { action ->
              handleCommandAction(action, CommandInvocationSource.BANNER, onEvent)
            }
          },
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
            progressJobs = layout.progressJobs,
            onModeSelect = { modeId -> onEvent(ShellUiEvent.ModeSelected(modeId)) },
            onQuickActionSelect = { action ->
              handleCommandAction(
                action = action,
                source = CommandInvocationSource.QUICK_ACTION,
                onEvent = onEvent,
              )
            },
            onRecentActivitySelect = { item -> onEvent(ShellUiEvent.ModeSelected(item.modeId)) },
            onProgressRetry = { job -> onEvent(ShellUiEvent.RetryJob(job)) },
            onProgressDismiss = { job -> onEvent(ShellUiEvent.CompleteJob(job.jobId)) },
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
  state: ShellUiState,
  onToggleLeftDrawer: () -> Unit,
  onToggleRightDrawer: (RightPanel) -> Unit,
  onEvent: (ShellUiEvent) -> Unit,
) {
  val layout = state.layout
  // var expanded by remember { mutableStateOf(false) }

  TopAppBar(
    title = {
      val titleText =
        if (layout.activeMode == ModeId.CHAT && state.chatState != null) {
          state.chatState.availablePersonas
            .find { it.personaId == state.chatState.currentPersonaId }
            ?.name ?: "Chat"
        } else {
          layout.activeMode.name.lowercase(Locale.ROOT).replaceFirstChar {
            it.titlecase(Locale.ROOT)
          }
        }
      Text(
        text = titleText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
          Modifier.clickable {
            if (layout.activeMode == ModeId.CHAT) {
              onEvent(ShellUiEvent.ChatTitleClicked)
            }
          },
      )
    },
    navigationIcon = {
      IconButton(onClick = onToggleLeftDrawer, modifier = Modifier.testTag("topbar_nav_icon")) {
        Icon(Icons.Outlined.Menu, contentDescription = "Toggle navigation drawer")
      }
    },
    actions = {
      IconButton(
        onClick = { onToggleRightDrawer(RightPanel.MODEL_SELECTOR) },
        modifier = Modifier.testTag("topbar_model_selector"),
      ) {
        Icon(Icons.Outlined.Tune, contentDescription = "Open model selector")
      }
      /*
      if (layout.activeMode == ModeId.CHAT) {
        IconButton(
          onClick = { expanded = true },
          modifier = Modifier.testTag("topbar_chat_menu"),
        ) {
          Icon(Icons.Outlined.MoreVert, contentDescription = "Chat menu")
        }
        DropdownMenu(
          expanded = expanded,
          onDismissRequest = { expanded = false },
        ) {
          DropdownMenuItem(
            text = { Text(stringResource(R.string.nano_shell_select_model)) },
            onClick = {
              onToggleRightDrawer(RightPanel.MODEL_SELECTOR)
              expanded = false
            },
          )
        }
      }
      */
    },
    colors = TopAppBarDefaults.topAppBarColors(),
  )
}

private fun handleCommandAction(
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
      onEvent(ShellUiEvent.HideCommandPalette(PaletteDismissReason.BACK_PRESSED))
      true
    }
    else -> false
  }
}
