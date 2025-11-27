package com.vjaykrsna.nanoai.core.domain.uiux

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandAction
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandCategory
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandDestination
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.ProgressJob
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentActivityItem
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentStatus
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CommandPaletteActionProviderTest {

  private val provider = CommandPaletteActionProvider()

  @Test
  fun `mode actions disable online-only entries when offline`() {
    val offlineActions = provider.provideModeActions(ConnectivityStatus.OFFLINE)

    val imageAction = offlineActions.first { it.id == "mode_image" }
    val chatAction = offlineActions.first { it.id == "mode_chat" }

    assertThat(imageAction.enabled).isFalse()
    assertThat(chatAction.enabled).isTrue()
  }

  @Test
  fun `history actions limit results and include timestamp prefix`() {
    val recentItems =
      (0 until 6).map { index ->
        RecentActivityItem(
          id = "recent-$index",
          modeId = ModeId.values()[index % ModeId.values().size],
          title = "Item $index",
          timestamp = Instant.now(),
          status = RecentStatus.COMPLETED,
        )
      }

    val actions = provider.provideHistoryActions(recentItems)

    assertThat(actions).hasSize(5)
    assertThat(actions.first().subtitle).startsWith("${recentItems.first().modeId.name} • ")
  }

  @Test
  fun `job actions emit header and cap individual jobs`() {
    val jobs =
      (0 until 4).map { index ->
        ProgressJob(
          jobId = UUID.randomUUID(),
          type = JobType.MODEL_DOWNLOAD,
          status = if (index == 0) JobStatus.FAILED else JobStatus.RUNNING,
          progress = 0.25f * (index + 1),
          canRetry = index == 0,
        )
      }

    val actions = provider.provideJobActions(jobs)

    assertThat(actions.first().id).isEqualTo("jobs_view_all")
    assertThat(actions.drop(1)).hasSize(3)
    assertThat(actions[1].title).isEqualTo("Model Download")
    assertThat(actions[1].subtitle).contains("%")
  }

  @Test
  fun `filterActions matches title subtitle and category`() {
    val actions =
      listOf(
        CommandAction(
          id = "help_docs",
          title = "Documentation",
          subtitle = "Guides and help",
          category = CommandCategory.HELP,
          destination = CommandDestination.Navigate("help"),
        ),
        CommandAction(
          id = "settings_main",
          title = "Settings",
          subtitle = "App preferences",
          category = CommandCategory.SETTINGS,
          destination = CommandDestination.Navigate("settings"),
        ),
      )

    val docsResults = provider.filterActions(actions, "docs")
    assertThat(docsResults).containsExactly(actions.first())
    assertThat(provider.filterActions(actions, "preferences")).containsExactly(actions.last())
    assertThat(provider.filterActions(actions, "help")).containsExactly(actions.first())
  }

  @Test
  fun `provideActions combines flows into single stream`() = runTest {
    val recentFlow = MutableStateFlow(listOf(sampleRecentActivity()))
    val jobFlow = MutableStateFlow(listOf(sampleJob()))
    val connectivityFlow = MutableStateFlow(ConnectivityStatus.ONLINE)

    val actions = provider.provideActions(recentFlow, jobFlow, connectivityFlow).first()

    val categories = actions.map(CommandAction::category).toSet()
    assertThat(categories).containsAtLeast(CommandCategory.MODES, CommandCategory.JOBS)
    assertThat(actions.any { it.id.startsWith("recent_") }).isTrue()
  }

  private fun sampleRecentActivity(): RecentActivityItem =
    RecentActivityItem(
      id = "recent-1",
      modeId = ModeId.CHAT,
      title = "Chat with Nova",
      timestamp = Instant.now(),
      status = RecentStatus.COMPLETED,
    )

  private fun sampleJob(): ProgressJob =
    ProgressJob(
      jobId = UUID.randomUUID(),
      type = JobType.MODEL_DOWNLOAD,
      status = JobStatus.RUNNING,
      progress = 0.4f,
      canRetry = false,
    )
}
