package com.vjaykrsna.nanoai.feature.uiux.domain

import com.vjaykrsna.nanoai.feature.uiux.navigation.Screen
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandCategory
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandDestination
import com.vjaykrsna.nanoai.feature.uiux.presentation.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.presentation.JobType
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
import com.vjaykrsna.nanoai.feature.uiux.presentation.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.presentation.RecentActivityItem
import com.vjaykrsna.nanoai.feature.uiux.presentation.toRoute
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Aggregates navigation targets, quick actions, recent activity, and progress jobs into command
 * palette actions grouped by category.
 */
private const val RECENT_ACTIVITY_LIMIT = 5
private const val MAX_PROGRESS_ACTIONS = 3
private const val PERCENT_MULTIPLIER = 100
private const val MINUTES_PER_HOUR = 60
private const val HOURS_PER_DAY = 24
private const val DAYS_PER_WEEK = 7

private data class ModeActionDefinition(
  val idSuffix: String,
  val title: String,
  val subtitle: String,
  val shortcut: String?,
  val modeId: ModeId,
  val requiresOnline: Boolean = false,
)

private val MODE_ACTION_DEFINITIONS =
  listOf(
    ModeActionDefinition("home", "Home", "Return to home hub", "Ctrl+H", ModeId.HOME),
    ModeActionDefinition("chat", "New Chat", "Start a conversation", "Ctrl+N", ModeId.CHAT),
    ModeActionDefinition(
      idSuffix = "image",
      title = "Generate Image",
      subtitle = "Create images from text",
      shortcut = "Ctrl+I",
      modeId = ModeId.IMAGE,
      requiresOnline = true,
    ),
    ModeActionDefinition(
      idSuffix = "audio",
      title = "Audio Session",
      subtitle = "Voice and audio processing",
      shortcut = "Ctrl+A",
      modeId = ModeId.AUDIO,
    ),
    ModeActionDefinition(
      idSuffix = "code",
      title = "Code Assistant",
      subtitle = "Programming help",
      shortcut = "Ctrl+Shift+C",
      modeId = ModeId.CODE,
    ),
    ModeActionDefinition(
      idSuffix = "translate",
      title = "Translation",
      subtitle = "Language translation",
      shortcut = "Ctrl+T",
      modeId = ModeId.TRANSLATE,
    ),
    ModeActionDefinition(
      idSuffix = "history",
      title = "History",
      subtitle = "View recent activity",
      shortcut = null,
      modeId = ModeId.HISTORY,
    ),
    ModeActionDefinition(
      idSuffix = "library",
      title = "Model Library",
      subtitle = "Manage AI models",
      shortcut = null,
      modeId = ModeId.LIBRARY,
    ),
    ModeActionDefinition(
      idSuffix = "tools",
      title = "Tools",
      subtitle = "Utilities and extensions",
      shortcut = null,
      modeId = ModeId.TOOLS,
    ),
  )

private val JOB_TYPE_LABELS =
  mapOf(
    JobType.IMAGE_GENERATION to "Image Generation",
    JobType.AUDIO_RECORDING to "Audio Recording",
    JobType.MODEL_DOWNLOAD to "Model Download",
    JobType.TEXT_GENERATION to "Text Generation",
    JobType.TRANSLATION to "Translation",
    JobType.OTHER to "Background Task",
  )

@Singleton
class CommandPaletteActionProvider @Inject constructor() {

  /**
   * Provides a combined flow of all available command actions, grouped by category.
   *
   * @param recentActivity Flow of recent user activity items
   * @param progressJobs Flow of current progress jobs
   * @param connectivity Current connectivity status
   * @return Flow of all command actions available in the palette
   */
  fun provideActions(
    recentActivity: Flow<List<RecentActivityItem>>,
    progressJobs: Flow<List<ProgressJob>>,
    connectivity: Flow<ConnectivityStatus>,
  ): Flow<List<CommandAction>> =
    combine(recentActivity, progressJobs, connectivity) { recent, jobs, status ->
      buildList {
        addAll(provideModeActions(status))
        addAll(provideHistoryActions(recent))
        addAll(provideJobActions(jobs))
        addAll(provideSettingsActions())
        addAll(provideHelpActions())
      }
    }

  /** Provides mode navigation actions. */
  fun provideModeActions(connectivity: ConnectivityStatus): List<CommandAction> {
    val isOnline = connectivity == ConnectivityStatus.ONLINE
    return MODE_ACTION_DEFINITIONS.map { definition ->
      CommandAction(
        id = "mode_${definition.idSuffix}",
        title = definition.title,
        subtitle = definition.subtitle,
        shortcut = definition.shortcut,
        enabled = if (definition.requiresOnline) isOnline else true,
        category = CommandCategory.MODES,
        destination = CommandDestination.Navigate(definition.modeId.toRoute()),
      )
    }
  }

  /** Provides history/recent activity actions. */
  fun provideHistoryActions(recentActivity: List<RecentActivityItem>): List<CommandAction> =
    recentActivity.take(RECENT_ACTIVITY_LIMIT).map { item ->
      val timestampKotlinx =
        kotlinx.datetime.Instant.fromEpochSeconds(item.timestamp.epochSecond, item.timestamp.nano)
      val timestampText = formatTimestamp(timestampKotlinx)
      CommandAction(
        id = "recent_${item.id}",
        title = item.title,
        subtitle = "${item.modeId.name} • $timestampText",
        category = CommandCategory.HISTORY,
        destination = CommandDestination.Navigate(item.toRoute()),
      )
    }

  /** Provides progress/job-related actions. */
  fun provideJobActions(progressJobs: List<ProgressJob>): List<CommandAction> = buildList {
    if (progressJobs.isNotEmpty()) {
      add(
        CommandAction(
          id = "jobs_view_all",
          title = "Open Model Library",
          subtitle = "${progressJobs.size} active job${if (progressJobs.size != 1) "s" else ""}",
          category = CommandCategory.JOBS,
          destination = CommandDestination.Navigate(ModeId.LIBRARY.toRoute()),
        )
      )
    }
    progressJobs.take(MAX_PROGRESS_ACTIONS).forEach { job ->
      val jobTypeDisplay = JOB_TYPE_LABELS[job.type] ?: "Background Task"
      val jobStatusDisplay = job.statusLabel
      add(
        CommandAction(
          id = "job_${job.jobId}",
          title = jobTypeDisplay,
          subtitle = "${(job.progress * PERCENT_MULTIPLIER).toInt()}% • $jobStatusDisplay",
          enabled = job.canRetry,
          category = CommandCategory.JOBS,
          destination = CommandDestination.Navigate(ModeId.LIBRARY.toRoute()),
        )
      )
    }
  }

  /** Provides settings-related actions. */
  fun provideSettingsActions(): List<CommandAction> =
    listOf(
      CommandAction(
        id = "settings_main",
        title = "Settings",
        subtitle = "App preferences and configuration",
        shortcut = "Ctrl+,",
        category = CommandCategory.SETTINGS,
        destination = CommandDestination.Navigate(Screen.Settings.route),
      ),
      CommandAction(
        id = "settings_appearance",
        title = "Appearance",
        subtitle = "Theme and visual settings",
        category = CommandCategory.SETTINGS,
        destination = CommandDestination.Navigate(Screen.SettingsAppearance.route),
      ),
      CommandAction(
        id = "settings_models",
        title = "Model Settings",
        subtitle = "Configure AI models",
        category = CommandCategory.SETTINGS,
        destination = CommandDestination.Navigate(Screen.SettingsModels.route),
      ),
    )

  /** Provides help-related actions. */
  fun provideHelpActions(): List<CommandAction> =
    listOf(
      CommandAction(
        id = "help_docs",
        title = "Documentation",
        subtitle = "View help and guides",
        category = CommandCategory.HELP,
        destination = CommandDestination.Navigate(Screen.HelpDocs.route),
      ),
      CommandAction(
        id = "help_shortcuts",
        title = "Keyboard Shortcuts",
        subtitle = "View all shortcuts",
        shortcut = "Ctrl+/",
        category = CommandCategory.HELP,
        destination = CommandDestination.Navigate(Screen.HelpShortcuts.route),
      ),
    )

  /** Filters actions based on a search query. */
  fun filterActions(actions: List<CommandAction>, query: String): List<CommandAction> {
    if (query.isBlank()) return actions
    val lowerQuery = query.lowercase()
    return actions.filter { action ->
      action.title.lowercase().contains(lowerQuery) ||
        action.subtitle?.lowercase()?.contains(lowerQuery) == true ||
        action.category.name.lowercase().contains(lowerQuery)
    }
  }

  private fun formatTimestamp(timestamp: kotlinx.datetime.Instant): String {
    val now = kotlinx.datetime.Clock.System.now()
    val duration = now - timestamp
    return when {
      duration.inWholeMinutes < 1 -> "Just now"
      duration.inWholeMinutes < MINUTES_PER_HOUR -> "${duration.inWholeMinutes}m ago"
      duration.inWholeHours < HOURS_PER_DAY -> "${duration.inWholeHours}h ago"
      duration.inWholeDays < DAYS_PER_WEEK -> "${duration.inWholeDays}d ago"
      else -> "Over a week ago"
    }
  }
}

/** Extension to convert RecentActivityItem to route string. */
private fun RecentActivityItem.toRoute(): String = "${modeId.toRoute()}/$id"
