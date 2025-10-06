package com.vjaykrsna.nanoai.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vjaykrsna.nanoai.feature.chat.ui.ChatScreen
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryScreen
import com.vjaykrsna.nanoai.feature.settings.ui.SettingsScreen
import com.vjaykrsna.nanoai.feature.uiux.presentation.AppUiState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellViewModel
import com.vjaykrsna.nanoai.feature.uiux.presentation.WelcomeUiState
import com.vjaykrsna.nanoai.feature.uiux.presentation.WelcomeViewModel
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.ui.WelcomePrimaryActions
import com.vjaykrsna.nanoai.feature.uiux.ui.WelcomeScreen
import com.vjaykrsna.nanoai.feature.uiux.ui.WelcomeTooltipActions
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.NanoShellScaffold
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.ShellUiEvent

/** Entry point that connects app-wide state to the unified Compose shell. */
@Composable
fun NavigationScaffold(
  appState: AppUiState,
  windowSizeClass: WindowSizeClass,
  modifier: Modifier = Modifier,
  shellViewModel: ShellViewModel = hiltViewModel(),
  welcomeViewModel: WelcomeViewModel = hiltViewModel(),
) {
  val shellUiState by shellViewModel.uiState.collectAsStateWithLifecycle()
  val welcomeUiState by welcomeViewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(windowSizeClass) { shellViewModel.updateWindowSizeClass(windowSizeClass) }
  LaunchedEffect(appState.offline) {
    val status = if (appState.offline) ConnectivityStatus.OFFLINE else ConnectivityStatus.ONLINE
    shellViewModel.updateConnectivity(status)
  }

  val shellEventHandler = rememberShellEventHandler(shellViewModel)

  Box(modifier = modifier.fillMaxSize()) {
    NanoShellScaffold(
      state = shellUiState,
      onEvent = shellEventHandler,
      modifier = Modifier.fillMaxSize(),
      modeContent = { modeId -> ShellModeContent(modeId, Modifier.fillMaxSize()) },
    )

    if (appState.shouldShowWelcome) {
      WelcomeOverlay(
        state = welcomeUiState,
        actions =
          WelcomePrimaryActions(
            onGetStarted = welcomeViewModel::onGetStarted,
            onExplore = welcomeViewModel::onExploreFeatures,
            onSkip = welcomeViewModel::onSkip,
          ),
        tooltipActions =
          WelcomeTooltipActions(
            onHelp = welcomeViewModel::onTooltipHelp,
            onDismiss = welcomeViewModel::onTooltipDismiss,
            onDontShowAgain = welcomeViewModel::onTooltipDontShowAgain,
          ),
      )
    }
  }
}

@Composable
private fun rememberShellEventHandler(shellViewModel: ShellViewModel): (ShellUiEvent) -> Unit =
  remember(shellViewModel) {
    { event ->
      when (event) {
        is ShellUiEvent.ModeSelected -> shellViewModel.openMode(event.modeId)
        ShellUiEvent.ToggleLeftDrawer -> shellViewModel.toggleLeftDrawer()
        is ShellUiEvent.ToggleRightDrawer -> shellViewModel.toggleRightDrawer(event.panel)
        is ShellUiEvent.ShowCommandPalette -> shellViewModel.showCommandPalette(event.source)
        ShellUiEvent.HideCommandPalette -> shellViewModel.hideCommandPalette()
        is ShellUiEvent.QueueJob -> shellViewModel.queueGeneration(event.job)
        is ShellUiEvent.CompleteJob -> shellViewModel.completeJob(event.jobId)
        is ShellUiEvent.Undo -> shellViewModel.undoAction(event.payload)
        is ShellUiEvent.ConnectivityChanged -> shellViewModel.updateConnectivity(event.status)
        is ShellUiEvent.UpdateTheme -> shellViewModel.updateThemePreference(event.theme)
        is ShellUiEvent.UpdateDensity -> shellViewModel.updateVisualDensity(event.density)
      }
    }
  }

@Composable
private fun ShellModeContent(modeId: ModeId, modifier: Modifier) {
  when (modeId) {
    ModeId.HOME -> Unit
    ModeId.CHAT -> ChatScreen(modifier = modifier)
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
private fun ModePlaceholder(title: String, modifier: Modifier) {
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

@Composable
private fun WelcomeOverlay(
  state: WelcomeUiState,
  actions: WelcomePrimaryActions,
  tooltipActions: WelcomeTooltipActions,
) {
  Surface(
    modifier = Modifier.fillMaxSize(),
    tonalElevation = 8.dp,
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
  ) {
    WelcomeScreen(
      state = state,
      actions = actions,
      tooltipActions = tooltipActions,
    )
  }
}
