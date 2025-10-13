package com.vjaykrsna.nanoai.ui.navigation

// Welcome / onboarding UI removed — onboarding is no longer part of the shell flow.
// Welcome UI imports removed
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.metrics.performance.PerformanceMetricsState
import com.vjaykrsna.nanoai.feature.chat.presentation.ChatViewModel
import com.vjaykrsna.nanoai.feature.chat.ui.ChatScreen
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryScreen
import com.vjaykrsna.nanoai.feature.settings.ui.SettingsScreen
import com.vjaykrsna.nanoai.feature.uiux.presentation.AppUiState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellViewModel
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.NanoShellScaffold
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.ShellUiEvent
import com.vjaykrsna.nanoai.ui.components.DisclaimerDialog

/** Entry point that connects app-wide state to the unified Compose shell. */
@Composable
fun NavigationScaffold(
  appState: AppUiState,
  windowSizeClass: WindowSizeClass,
  modifier: Modifier = Modifier,
  shellViewModel: ShellViewModel = hiltViewModel(),
  chatViewModel: ChatViewModel = hiltViewModel(),
  onDisclaimerShown: () -> Unit = {},
  onDisclaimerAccepted: () -> Unit = {},
  onDisclaimerDeclined: () -> Unit = {},
) {
  val shellUiState by shellViewModel.uiState.collectAsStateWithLifecycle()

  val view = LocalView.current
  val metricsStateHolder = remember(view) { PerformanceMetricsState.getHolderForHierarchy(view) }

  LaunchedEffect(windowSizeClass) { shellViewModel.updateWindowSizeClass(windowSizeClass) }
  LaunchedEffect(appState.offline) {
    val status = if (appState.offline) ConnectivityStatus.OFFLINE else ConnectivityStatus.ONLINE
    shellViewModel.updateConnectivity(status)
  }

  LaunchedEffect(shellUiState.layout.activeMode) {
    metricsStateHolder.state?.putState("shell_mode", shellUiState.layout.activeMode.name)
  }
  LaunchedEffect(shellUiState.layout.progressJobs) {
    val activeJobs = shellUiState.layout.progressJobs.count { !it.isTerminal }
    metricsStateHolder.state?.putState("active_jobs", activeJobs.toString())
  }

  DisposableEffect(metricsStateHolder) {
    onDispose {
      metricsStateHolder.state?.removeState("shell_mode")
      metricsStateHolder.state?.removeState("active_jobs")
    }
  }

  val shellEventHandler = rememberShellEventHandler(shellViewModel, chatViewModel)

  var disclaimerDismissedForSession by rememberSaveable { mutableStateOf(false) }
  val shouldShowDisclaimer = appState.disclaimer.shouldShow && !disclaimerDismissedForSession

  LaunchedEffect(appState.disclaimer.shouldShow) {
    if (!appState.disclaimer.shouldShow) {
      disclaimerDismissedForSession = false
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    NanoShellScaffold(
      state = shellUiState,
      onEvent = shellEventHandler,
      modifier = Modifier.fillMaxSize(),
      modeContent = { modeId ->
        ShellModeContent(modeId, Modifier.fillMaxSize(), shellViewModel::updateChatState)
      },
    )

    // Onboarding / welcome removed — main shell is shown directly.

    if (shouldShowDisclaimer) {
      DisclaimerDialog(
        onAccept = {
          onDisclaimerAccepted()
          disclaimerDismissedForSession = true
        },
        onDecline = {
          disclaimerDismissedForSession = true
          onDisclaimerDeclined()
        },
        onDismissRequest = {
          disclaimerDismissedForSession = true
          onDisclaimerDeclined()
        },
        onDialogShown = onDisclaimerShown,
      )
    }
  }
}

@Composable
private fun rememberShellEventHandler(
  shellViewModel: ShellViewModel,
  chatViewModel: ChatViewModel
): (ShellUiEvent) -> Unit =
  remember(shellViewModel, chatViewModel) {
    { event ->
      when (event) {
        is ShellUiEvent.ModeSelected -> shellViewModel.openMode(event.modeId)
        ShellUiEvent.ToggleLeftDrawer -> shellViewModel.toggleLeftDrawer()
        is ShellUiEvent.SetLeftDrawer -> shellViewModel.setLeftDrawer(event.open)
        is ShellUiEvent.ToggleRightDrawer -> shellViewModel.toggleRightDrawer(event.panel)
        is ShellUiEvent.ShowCommandPalette -> shellViewModel.showCommandPalette(event.source)
        is ShellUiEvent.HideCommandPalette -> shellViewModel.hideCommandPalette(event.reason)
        is ShellUiEvent.CommandInvoked ->
          shellViewModel.onCommandInvoked(event.action, event.source)
        is ShellUiEvent.QueueJob -> shellViewModel.queueGeneration(event.job)
        is ShellUiEvent.RetryJob -> shellViewModel.retryJob(event.job)
        is ShellUiEvent.CompleteJob -> shellViewModel.completeJob(event.jobId)
        is ShellUiEvent.Undo -> shellViewModel.undoAction(event.payload)
        is ShellUiEvent.ConnectivityChanged -> shellViewModel.updateConnectivity(event.status)
        is ShellUiEvent.UpdateTheme -> shellViewModel.updateThemePreference(event.theme)
        is ShellUiEvent.UpdateDensity -> shellViewModel.updateVisualDensity(event.density)
        is ShellUiEvent.ChatPersonaSelected ->
          chatViewModel.switchPersona(event.personaId, event.action)
      }
    }
  }

@Composable
private fun ShellModeContent(
  modeId: ModeId,
  modifier: Modifier = Modifier,
  onUpdateChatState: (com.vjaykrsna.nanoai.feature.uiux.presentation.ChatState?) -> Unit,
) {
  when (modeId) {
    // HOME is handled directly by NanoShellScaffold - never called here
    ModeId.HOME -> error("HOME mode should be handled by NanoShellScaffold, not NavigationScaffold")
    ModeId.CHAT -> ChatScreen(modifier = modifier, onUpdateChatState = onUpdateChatState)
    ModeId.LIBRARY -> ModelLibraryScreen(modifier = modifier)
    ModeId.SETTINGS -> SettingsScreen(modifier = modifier)
    ModeId.IMAGE -> ModePlaceholder("Image generation", modifier)
    ModeId.AUDIO -> ModePlaceholder("Audio / Live voice", modifier)
    ModeId.CODE -> ModePlaceholder("Code workspace", modifier)
    ModeId.TRANSLATE -> ModePlaceholder("Translation", modifier)
    ModeId.HISTORY -> ModePlaceholder("History", modifier)
    ModeId.TOOLS -> ModePlaceholder("Tools", modifier)
  }
}

@Composable
private fun ModePlaceholder(title: String, modifier: Modifier = Modifier) {
  Box(
    modifier =
      modifier.fillMaxSize().semantics { contentDescription = "$title surface placeholder" },
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = "$title is coming soon",
      style = MaterialTheme.typography.bodyLarge,
      fontWeight = FontWeight.Medium,
      textAlign = TextAlign.Center,
    )
  }
}

// WelcomeOverlay and WelcomeScreen removed from the app. See specs/003-UI-UX for rationale.
