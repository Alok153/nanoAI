package com.vjaykrsna.nanoai.feature.uiux.domain

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.uiux.state.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.state.CommandCategory
import com.vjaykrsna.nanoai.feature.uiux.state.CommandDestination
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.state.JobStatus
import com.vjaykrsna.nanoai.feature.uiux.state.JobType
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.state.RecentActivityItem
import com.vjaykrsna.nanoai.feature.uiux.state.RecentStatus
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CommandPaletteActionProviderTest {
  private val subject = CommandPaletteActionProvider()

  @Test
  fun provideModeActions_disablesOnlineOnlyDefinitionWhenOffline() {
    val offlineActions = subject.provideModeActions(ConnectivityStatus.OFFLINE)

    val imageAction = offlineActions.first { it.id == "mode_image" }
    val chatAction = offlineActions.first { it.id == "mode_chat" }

    assertThat(imageAction.enabled).isFalse()
    assertThat(chatAction.enabled).isTrue()

    val onlineActions = subject.provideModeActions(ConnectivityStatus.ONLINE)
    val imageOnline = onlineActions.first { it.id == "mode_image" }
    assertThat(imageOnline.enabled).isTrue()
  }

  @Test
  fun provideHistoryActions_limitsAndFormatsEntries() {
    val timestamp = Instant.now().minusSeconds(60L * 60L * 24L * 8L)
    val recents =
      (0 until 6).map { index ->
        RecentActivityItem(
          id = "item-$index",
          modeId = ModeId.CHAT,
          title = "Conversation $index",
          timestamp = timestamp.plusSeconds(index.toLong()),
          status = RecentStatus.COMPLETED,
        )
      }

    val actions = subject.provideHistoryActions(recents)

    assertThat(actions).hasSize(5)
    assertThat(actions.map(CommandAction::id))
      .containsExactlyElementsIn(recents.take(5).map { "recent_${it.id}" })
      .inOrder()

    val firstSubtitle = actions.first().subtitle
    assertThat(firstSubtitle).isNotNull()
    assertThat(firstSubtitle).startsWith("${ModeId.CHAT.name} • ")
    assertThat(firstSubtitle).endsWith("Over a week ago")
  }

  @Test
  fun filterActions_matchesTitleSubtitleAndCategory() {
    val actions =
      listOf(
        CommandAction(id = "mode_home", title = "Home", category = CommandCategory.MODES),
        CommandAction(
          id = "help_docs",
          title = "Documentation",
          subtitle = "Open help center",
          category = CommandCategory.HELP,
        ),
        CommandAction(
          id = "settings_main",
          title = "Settings",
          category = CommandCategory.SETTINGS,
        ),
      )

    assertThat(subject.filterActions(actions, "   ")).containsExactlyElementsIn(actions)
    assertThat(subject.filterActions(actions, "doc")).containsExactly(actions[1])
    assertThat(subject.filterActions(actions, "HELP")).containsExactly(actions[1])
    assertThat(subject.filterActions(actions, "settings")).containsExactly(actions[2])
    assertThat(subject.filterActions(actions, "open")).containsExactly(actions[1])
    assertThat(subject.filterActions(actions, "missing")).isEmpty()
  }

  @Test
  fun provideActions_combinesFlowsIntoUnifiedPalette() = runTest {
    val recent =
      RecentActivityItem(
        id = "42",
        modeId = ModeId.HISTORY,
        title = "Viewed history",
        timestamp = Instant.now(),
        status = RecentStatus.COMPLETED,
      )
    val progressJob =
      ProgressJob(
        jobId = UUID.randomUUID(),
        type = JobType.TEXT_GENERATION,
        status = JobStatus.RUNNING,
        progress = 0.4f,
        canRetry = true,
      )

    val actions =
      subject
        .provideActions(
          recentActivity = flowOf(listOf(recent)),
          progressJobs = flowOf(listOf(progressJob)),
          connectivity = flowOf(ConnectivityStatus.ONLINE),
        )
        .first()

    val ids = actions.map(CommandAction::id)
    assertThat(ids).containsAtLeast("mode_home", "jobs_view_all", "job_${progressJob.jobId}")
    assertThat(ids).contains("recent_${recent.id}")

    val jobAction = actions.first { it.id == "job_${progressJob.jobId}" }
    assertThat(jobAction.subtitle).contains("40%")
    assertThat(jobAction.subtitle).contains("In progress")
    assertThat(jobAction.enabled).isTrue()
    assertThat(jobAction.destination).isInstanceOf(CommandDestination.Navigate::class.java)
  }
}
