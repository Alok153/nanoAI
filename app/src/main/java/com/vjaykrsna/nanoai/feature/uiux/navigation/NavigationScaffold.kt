package com.vjaykrsna.nanoai.feature.uiux.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.metrics.performance.PerformanceMetricsState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.feature.audio.ui.AudioScreen
import com.vjaykrsna.nanoai.feature.chat.presentation.ChatViewModel
import com.vjaykrsna.nanoai.feature.chat.ui.ChatScreen
import com.vjaykrsna.nanoai.feature.image.ui.ImageFeatureContainer
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryScreen
import com.vjaykrsna.nanoai.feature.settings.ui.SettingsScreen
import com.vjaykrsna.nanoai.feature.uiux.presentation.AppUiState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ChatState
import com.vjaykrsna.nanoai.feature.uiux.presentation.DisclaimerUiState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellViewModel
import com.vjaykrsna.nanoai.shared.ui.components.DisclaimerDialog
import com.vjaykrsna.nanoai.shared.ui.shell.NanoShellScaffold
import com.vjaykrsna.nanoai.shared.ui.shell.ShellUiEvent

// Welcome / onboarding UI removed — onboarding is no longer part of the shell flow.
// Welcome UI imports removed

/** Entry point that connects app-wide state to the unified Compose shell. */
@Composable
fun NavigationScaffold(
  appState: AppUiState,
  windowSizeClass: WindowSizeClass,
  modifier: Modifier = Modifier,
  shellViewModel: ShellViewModel = hiltViewModel(),
  chatViewModel: ChatViewModel = hiltViewModel(),
  onDisclaimerShow: () -> Unit = {},
  onDisclaimerAccept: () -> Unit = {},
  onDisclaimerDecline: () -> Unit = {},
) {
  val shellUiState by shellViewModel.uiState.collectAsStateWithLifecycle()
  val view = LocalView.current
  val metricsHolder = remember(view) { PerformanceMetricsState.getHolderForHierarchy(view) }

  LaunchedEffect(windowSizeClass) { shellViewModel.updateWindowSizeClass(windowSizeClass) }

  LaunchedEffect(appState.offline) {
    val status = if (appState.offline) ConnectivityStatus.OFFLINE else ConnectivityStatus.ONLINE
    shellViewModel.onEvent(ShellUiEvent.ConnectivityChanged(status))
  }

  LaunchedEffect(shellUiState.layout.activeMode, metricsHolder) {
    metricsHolder.state?.putState("shell_mode", shellUiState.layout.activeMode.name)
  }

  LaunchedEffect(shellUiState.layout.progressJobs, metricsHolder) {
    val activeJobs = shellUiState.layout.progressJobs.count { !it.isTerminal }
    metricsHolder.state?.putState("active_jobs", activeJobs.toString())
  }

  DisposableEffect(metricsHolder) {
    onDispose {
      metricsHolder.state?.removeState("shell_mode")
      metricsHolder.state?.removeState("active_jobs")
    }
  }

  val disclaimerState =
    rememberDisclaimerState(
      disclaimer = appState.disclaimer,
      onAccept = onDisclaimerAccept,
      onDecline = onDisclaimerDecline,
    )

  val onNavigateToCoverageDashboard =
    remember(shellViewModel) { { shellViewModel.onEvent(ShellUiEvent.ShowCoverageDashboard) } }

  Box(modifier = modifier.fillMaxSize()) {
    NanoShellScaffold(
      state = shellUiState,
      onEvent = { event -> handleShellEvent(event, shellViewModel, chatViewModel) },
      modifier = Modifier.fillMaxSize(),
      modeContent = { modeId ->
        ShellModeContent(
          modeId = modeId,
          onNavigateToCoverageDashboard = onNavigateToCoverageDashboard,
          onUpdateChatState = shellViewModel::updateChatState,
          modifier = Modifier.fillMaxSize(),
          onNavigate = { mode -> shellViewModel.onEvent(ShellUiEvent.ModeSelected(mode)) },
        )
      },
    )

    DisclaimerOverlay(controller = disclaimerState, onDialogShow = onDisclaimerShow)

    SkipLinksNavigation(
      onSkipToContent = { shellViewModel.onEvent(ShellUiEvent.ModeSelected(ModeId.HOME)) },
      onSkipToNavigation = { shellViewModel.onEvent(ShellUiEvent.ToggleLeftDrawer) },
      modifier = Modifier.align(Alignment.TopStart),
    )
  }
}

@Composable
private fun ShellModeContent(
  modeId: ModeId,
  onNavigateToCoverageDashboard: () -> Unit,
  onUpdateChatState: (ChatState?) -> Unit,
  modifier: Modifier = Modifier,
  onNavigate: (ModeId) -> Unit = {},
) {
  if (modeId == ModeId.HOME) {
    error("HOME mode should be handled by NanoShellScaffold, not NavigationScaffold")
  }

  val providers =
    rememberModeContentProviders(
      onNavigateToCoverageDashboard = onNavigateToCoverageDashboard,
      onUpdateChatState = onUpdateChatState,
      onNavigate = onNavigate,
    )

  val content = providers[modeId]

  if (content == null) {
    ErrorBoundary(
      modifier = modifier,
      title = "Navigation Error",
      description = "Unsupported shell mode: ${modeId.name}",
    )
  } else {
    content(modifier)
  }
}

private data class DisclaimerState(
  val shouldShow: Boolean,
  val onAccept: () -> Unit,
  val onDecline: () -> Unit,
  val onDismiss: () -> Unit,
)

@Composable
private fun rememberDisclaimerState(
  disclaimer: DisclaimerUiState,
  onAccept: () -> Unit,
  onDecline: () -> Unit,
): DisclaimerState {
  var dismissedForSession by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(disclaimer.shouldShow) {
    if (!disclaimer.shouldShow) {
      dismissedForSession = false
    }
  }

  val shouldShow = disclaimer.shouldShow && !dismissedForSession

  val accept: () -> Unit = {
    onAccept()
    dismissedForSession = true
  }

  val decline: () -> Unit = {
    dismissedForSession = true
    onDecline()
  }

  val dismiss: () -> Unit = {
    dismissedForSession = true
    onDecline()
  }

  return DisclaimerState(
    shouldShow = shouldShow,
    onAccept = accept,
    onDecline = decline,
    onDismiss = dismiss,
  )
}

@Composable
private fun DisclaimerOverlay(controller: DisclaimerState, onDialogShow: () -> Unit) {
  if (!controller.shouldShow) return

  DisclaimerDialog(
    onAccept = controller.onAccept,
    onDecline = controller.onDecline,
    onDismissRequest = controller.onDismiss,
    onDialogShow = onDialogShow,
  )
}

private fun handleShellEvent(
  event: ShellUiEvent,
  shellViewModel: ShellViewModel,
  chatViewModel: ChatViewModel,
) {
  when (event) {
    is ShellUiEvent.ChatPersonaSelected ->
      chatViewModel.switchPersona(event.personaId, event.action)
    is ShellUiEvent.ChatTitleClicked -> chatViewModel.showModelPicker()
    else -> shellViewModel.onEvent(event)
  }
}

private typealias ModeContentProvider = @Composable (Modifier) -> Unit

@Composable
private fun rememberModeContentProviders(
  onNavigateToCoverageDashboard: () -> Unit,
  onUpdateChatState: (ChatState?) -> Unit,
  onNavigate: (ModeId) -> Unit,
): Map<ModeId, ModeContentProvider> {
  return remember(onNavigateToCoverageDashboard, onUpdateChatState, onNavigate) {
    mapOf(
      ModeId.CHAT to
        { innerModifier ->
          ChatScreen(
            modifier = innerModifier,
            onUpdateChatState = onUpdateChatState,
            onNavigate = onNavigate,
          )
        },
      ModeId.LIBRARY to { innerModifier -> ModelLibraryScreen(modifier = innerModifier) },
      ModeId.SETTINGS to
        { innerModifier ->
          SettingsScreen(
            modifier = innerModifier,
            onNavigateToCoverageDashboard = onNavigateToCoverageDashboard,
          )
        },
      ModeId.IMAGE to { innerModifier -> ImageFeatureContainer(modifier = innerModifier) },
      ModeId.AUDIO to { innerModifier -> AudioScreen(modifier = innerModifier) },
      ModeId.CODE to { innerModifier -> ModePlaceholder("Code workspace", innerModifier) },
      ModeId.TRANSLATE to { innerModifier -> ModePlaceholder("Translation", innerModifier) },
      ModeId.HISTORY to { innerModifier -> ModePlaceholder("History", innerModifier) },
      ModeId.TOOLS to { innerModifier -> ModePlaceholder("Tools", innerModifier) },
    )
  }
}

@Composable
private fun ErrorBoundary(
  modifier: Modifier = Modifier,
  title: String = "Error",
  description: String = "An unexpected error occurred",
  onRetry: () -> Unit = {},
) {
  Box(
    modifier =
      modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).semantics {
        contentDescription = "Error boundary: $title"
      },
    contentAlignment = Alignment.Center,
  ) {
    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = null,
        modifier = Modifier.padding(bottom = 16.dp),
        tint = MaterialTheme.colorScheme.error,
      )

      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 8.dp),
      )

      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 24.dp),
      )

      TextButton(onClick = onRetry) { Text("Try Again") }
    }
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

/**
 * Skip links navigation component for keyboard accessibility. Provides keyboard shortcuts to skip
 * directly to main content or navigation. These links are visually hidden but accessible to screen
 * readers and keyboard users.
 */
@Composable
internal fun SkipLinksNavigation(
  onSkipToContent: () -> Unit,
  onSkipToNavigation: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // NOTE: Skip links are typically visually hidden but keyboard-accessible.
  // They provide quick navigation for keyboard users and screen reader users.
  // Implementation uses standard accessibility patterns:
  // - Alt+1: Skip to main content
  // - Alt+2: Skip to navigation
  Column(
    modifier =
      modifier.fillMaxSize().semantics {
        contentDescription =
          "Skip links: Press Alt+1 to jump to main content, Alt+2 to jump to navigation"
      },
    verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.Start,
  ) {
    SkipLinkButton(
      label = "Skip to main content",
      onClick = onSkipToContent,
      testTag = "skip_link_content",
    )
    Spacer(modifier = Modifier.height(4.dp))
    SkipLinkButton(
      label = "Skip to navigation menu",
      onClick = onSkipToNavigation,
      testTag = "skip_link_navigation",
    )
  }
}

@Composable
private fun SkipLinkButton(label: String, onClick: () -> Unit, testTag: String) {
  var hasFocus by remember { mutableStateOf(false) }

  TextButton(
    onClick = onClick,
    modifier =
      Modifier.testTag(testTag)
        .onFocusChanged { hasFocus = it.isFocused }
        .alpha(if (hasFocus) 1f else 0f)
        .semantics {
          contentDescription = label
          role = androidx.compose.ui.semantics.Role.Button
        },
  ) {
    Text(text = label)
  }
}

// WelcomeOverlay and WelcomeScreen removed from the app. See specs/003-UI-UX for rationale.
