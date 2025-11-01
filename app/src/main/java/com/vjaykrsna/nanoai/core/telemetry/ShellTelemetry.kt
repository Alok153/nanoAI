package com.vjaykrsna.nanoai.core.telemetry

import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandDestination
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandInvocationSource
import com.vjaykrsna.nanoai.feature.uiux.presentation.DrawerSide
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
import com.vjaykrsna.nanoai.feature.uiux.presentation.PaletteDismissReason
import com.vjaykrsna.nanoai.feature.uiux.presentation.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.presentation.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.presentation.RightPanel
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records shell-specific analytics while respecting global telemetry consent.
 *
 * Encapsulates event naming and metadata shims so presentation logic can remain lightweight.
 */
@Singleton
class ShellTelemetry @Inject constructor(private val telemetryReporter: TelemetryReporter) {

  /** Track when the command palette is opened along with the initiating surface. */
  fun trackCommandPaletteOpened(source: PaletteSource, activeMode: ModeId) {
    telemetryReporter.trackInteraction(
      event = COMMAND_PALETTE_OPEN,
      metadata =
        mapOf(
          KEY_SOURCE to source.name.lowercase(Locale.ROOT),
          KEY_MODE to activeMode.name.lowercase(Locale.ROOT),
        ),
    )
  }

  /** Track when the command palette is dismissed by the user. */
  fun trackCommandPaletteDismissed(reason: PaletteDismissReason, activeMode: ModeId) {
    telemetryReporter.trackInteraction(
      event = COMMAND_PALETTE_DISMISS,
      metadata =
        mapOf(
          KEY_REASON to reason.name.lowercase(Locale.ROOT),
          KEY_MODE to activeMode.name.lowercase(Locale.ROOT),
        ),
    )
  }

  /** Track execution of a command action (palette, quick action, or banner CTA). */
  fun trackCommandInvocation(
    action: CommandAction,
    invocationSource: CommandInvocationSource,
    activeMode: ModeId,
  ) {
    val destinationType =
      when (action.destination) {
        is CommandDestination.Navigate -> DESTINATION_NAVIGATE
        is CommandDestination.OpenRightPanel -> DESTINATION_RIGHT_PANEL
        CommandDestination.None -> DESTINATION_NONE
      }
    val destinationRoute =
      (action.destination as? CommandDestination.Navigate)?.route ?: EMPTY_VALUE
    val panel =
      (action.destination as? CommandDestination.OpenRightPanel)
        ?.panel
        ?.name
        ?.lowercase(Locale.ROOT) ?: EMPTY_VALUE

    telemetryReporter.trackInteraction(
      event = COMMAND_PALETTE_INVOKE,
      metadata =
        mapOf(
            KEY_ACTION_ID to action.id,
            KEY_ACTION_CATEGORY to action.category.name.lowercase(Locale.ROOT),
            KEY_SOURCE to invocationSource.name.lowercase(Locale.ROOT),
            KEY_DESTINATION_TYPE to destinationType,
            KEY_DESTINATION_ROUTE to destinationRoute,
            KEY_PANEL to panel,
            KEY_MODE to activeMode.name.lowercase(Locale.ROOT),
          )
          .filterValues { it.isNotEmpty() },
    )
  }

  /** Track when the left or right drawers are toggled. */
  fun trackDrawerToggle(side: DrawerSide, isOpen: Boolean, panel: RightPanel?, activeMode: ModeId) {
    telemetryReporter.trackInteraction(
      event = DRAWER_TOGGLE,
      metadata =
        mapOf(
            KEY_SIDE to side.name.lowercase(Locale.ROOT),
            KEY_OPEN to isOpen.toString(),
            KEY_PANEL to (panel?.name?.lowercase(Locale.ROOT) ?: EMPTY_VALUE),
            KEY_MODE to activeMode.name.lowercase(Locale.ROOT),
          )
          .filterValues { it.isNotEmpty() },
    )
  }

  /** Track when a progress job is queued from the shell (typically while offline). */
  fun trackProgressJobQueued(job: ProgressJob, offline: Boolean, activeMode: ModeId) {
    telemetryReporter.trackInteraction(
      event = PROGRESS_JOB_QUEUED,
      metadata =
        mapOf(
          KEY_JOB_TYPE to job.type.name.lowercase(Locale.ROOT),
          KEY_JOB_STATUS to job.status.name.lowercase(Locale.ROOT),
          KEY_CAN_RETRY to job.canRetry.toString(),
          KEY_OFFLINE to offline.toString(),
          KEY_MODE to activeMode.name.lowercase(Locale.ROOT),
        ),
    )
  }

  private companion object {
    private const val COMMAND_PALETTE_OPEN = "shell.command_palette.open"
    private const val COMMAND_PALETTE_DISMISS = "shell.command_palette.dismiss"
    private const val COMMAND_PALETTE_INVOKE = "shell.command_palette.command"
    private const val DRAWER_TOGGLE = "shell.drawer.toggle"
    private const val PROGRESS_JOB_QUEUED = "shell.progress.job_queued"

    private const val DESTINATION_NAVIGATE = "navigate"
    private const val DESTINATION_RIGHT_PANEL = "right_panel"
    private const val DESTINATION_NONE = "none"

    private const val KEY_SOURCE = "source"
    private const val KEY_MODE = "mode"
    private const val KEY_REASON = "reason"
    private const val KEY_ACTION_ID = "action_id"
    private const val KEY_ACTION_CATEGORY = "category"
    private const val KEY_DESTINATION_TYPE = "destination_type"
    private const val KEY_DESTINATION_ROUTE = "destination_route"
    private const val KEY_PANEL = "panel"
    private const val KEY_SIDE = "side"
    private const val KEY_OPEN = "open"
    private const val KEY_JOB_TYPE = "job_type"
    private const val KEY_JOB_STATUS = "job_status"
    private const val KEY_CAN_RETRY = "can_retry"
    private const val KEY_OFFLINE = "offline"

    private const val EMPTY_VALUE = ""
  }
}
