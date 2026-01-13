@file:Suppress("MagicNumber")

package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandAction
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandCategory
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandPaletteState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityBannerState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.ProgressJob
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentActivityItem
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.RightPanel
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellWindowSizeClass
import com.vjaykrsna.nanoai.shared.ui.shell.NanoShellScaffold
import com.vjaykrsna.nanoai.shared.ui.theme.NanoAITheme
import io.github.takahirom.roborazzi.captureRoboImage
import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShellScreenshotsTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun shellHome_offlineBanner_baseline() {
    captureShell(state = sampleShellState(ConnectivityStatus.OFFLINE, showPalette = false),
      filePath = "shell/shell-home-offline.png")
  }

  @Test
  fun shellHome_commandPalette_baseline() {
    captureShell(state = sampleShellState(ConnectivityStatus.ONLINE, showPalette = true),
      filePath = "shell/shell-command-palette.png")
  }

  private fun captureShell(state: ShellUiState, filePath: String) {
    composeRule.setContent { ShellPreview(state) }
    composeRule.waitForIdle()
    composeRule.onRoot().captureRoboImage(filePath = filePath)
  }

  @Composable
  private fun ShellPreview(state: ShellUiState) {
    NanoAITheme { NanoShellScaffold(state = state, onEvent = {}) }
  }
}

private fun sampleShellState(connectivity: ConnectivityStatus, showPalette: Boolean): ShellUiState {
  val commands = sampleCommands()
  val paletteState =
    CommandPaletteState(
      query = if (showPalette) "chat" else "",
      results = commands,
      recentCommands = commands.take(2),
      selectedIndex = if (showPalette) 0 else -1,
      surfaceTarget = CommandCategory.MODES,
    )

  return ShellUiState(
    layout =
      ShellLayoutState(
        windowSizeClass = ShellWindowSizeClass.Default,
        isLeftDrawerOpen = false,
        isRightDrawerOpen = showPalette,
        activeRightPanel = if (showPalette) RightPanel.PROGRESS_CENTER else null,
        activeMode = ModeId.HOME,
        showCommandPalette = showPalette,
        showCoverageDashboard = false,
        connectivity = connectivity,
        pendingUndoAction = null,
        progressJobs = sampleJobs(connectivity),
        recentActivity = sampleRecents(),
      ),
    commandPalette = if (showPalette) paletteState else paletteState.cleared(),
    connectivityBanner =
      ConnectivityBannerState(
        status = connectivity,
        queuedActionCount = if (connectivity != ConnectivityStatus.ONLINE) 2 else 0,
        cta = CommandAction(id = "view-queue", title = "View queue", category = CommandCategory.JOBS),
      ),
    preferences = ShellUiPreferences(),
    modeCards = sampleModeCards(isOnline = connectivity == ConnectivityStatus.ONLINE),
    quickActions = commands,
    chatState = null,
  )
}

private fun sampleCommands(): List<CommandAction> =
  listOf(
    CommandAction(id = "new_chat", title = "New chat", category = CommandCategory.MODES),
    CommandAction(id = "open_library", title = "Library", category = CommandCategory.MODES),
    CommandAction(id = "open_settings", title = "Settings", category = CommandCategory.SETTINGS),
  )

private fun sampleModeCards(isOnline: Boolean): List<ModeCard> {
  val cards =
    listOf(
      modeCard(ModeId.CHAT, "Chat", Icons.AutoMirrored.Filled.Chat, subtitle = "Local-first"),
      modeCard(ModeId.LIBRARY, "Library", Icons.Filled.History, subtitle = "Models"),
      modeCard(ModeId.SETTINGS, "Settings", Icons.Filled.Settings, subtitle = "Privacy"),
    )
  return cards.map { card -> card.copy(enabled = isOnline || card.id != ModeId.LIBRARY) }
}

private fun modeCard(id: ModeId, title: String, icon: ImageVector, subtitle: String?): ModeCard =
  ModeCard(
    id = id,
    title = title,
    subtitle = subtitle,
    icon = icon,
    primaryAction = CommandAction(id = "go_$title", title = title, category = CommandCategory.MODES),
    enabled = true,
  )

private fun sampleJobs(connectivity: ConnectivityStatus): List<ProgressJob> {
  if (connectivity == ConnectivityStatus.ONLINE) return emptyList()
  return listOf(
    ProgressJob(
      jobId = UUID.fromString("00000000-0000-0000-0000-000000000123"),
      type = JobType.MODEL_DOWNLOAD,
      status = JobStatus.RUNNING,
      progress = 0.45f,
      eta = Duration.ofSeconds(25),
      canRetry = true,
      queuedAt = Instant.parse("2025-09-30T10:15:30Z"),
      subtitle = "Phoenix 3B download",
    )
  )
}

private fun sampleRecents(): List<RecentActivityItem> =
  listOf(
    RecentActivityItem(
      id = "recent-chat",
      modeId = ModeId.CHAT,
      title = "Trip planning with Phoenix",
      timestamp = Instant.parse("2025-09-30T09:55:00Z"),
      status = RecentStatus.COMPLETED,
    ),
    RecentActivityItem(
      id = "recent-library",
      modeId = ModeId.LIBRARY,
      title = "Queued local model",
      timestamp = Instant.parse("2025-09-30T09:58:00Z"),
      status = RecentStatus.IN_PROGRESS,
    ),
  )
}
