package com.vjaykrsna.nanoai.core.telemetry

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandAction
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandCategory
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandDestination
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandInvocationSource
import com.vjaykrsna.nanoai.core.domain.model.uiux.DrawerSide
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.PaletteDismissReason
import com.vjaykrsna.nanoai.core.domain.model.uiux.PaletteSource
import com.vjaykrsna.nanoai.core.domain.model.uiux.ProgressJob
import com.vjaykrsna.nanoai.core.domain.model.uiux.RightPanel
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ShellTelemetryTest {

  private val telemetryReporter: TelemetryReporter = mockk(relaxed = true)
  private lateinit var shellTelemetry: ShellTelemetry

  @BeforeEach
  fun setUp() {
    shellTelemetry = ShellTelemetry(telemetryReporter)
  }

  @Test
  fun `trackCommandPaletteOpened emits source metadata`() {
    shellTelemetry.trackCommandPaletteOpened(PaletteSource.TOP_APP_BAR, ModeId.CHAT)

    val metadataSlot = slot<Map<String, String>>()
    verify {
      telemetryReporter.trackInteraction("shell.command_palette.open", capture(metadataSlot))
    }
    val metadata = metadataSlot.captured
    assertThat(metadata["source"]).isEqualTo("top_app_bar")
    assertThat(metadata["mode"]).isEqualTo("chat")
  }

  @Test
  fun `trackCommandInvocation captures navigation destination`() {
    val action =
      CommandAction(
        id = "open-settings",
        title = "Open Settings",
        category = CommandCategory.SETTINGS,
        destination = CommandDestination.Navigate("settings"),
      )

    shellTelemetry.trackCommandInvocation(action, CommandInvocationSource.PALETTE, ModeId.HOME)

    val metadataSlot = slot<Map<String, String>>()
    verify {
      telemetryReporter.trackInteraction("shell.command_palette.command", capture(metadataSlot))
    }
    val metadata = metadataSlot.captured
    assertThat(metadata["action_id"]).isEqualTo("open-settings")
    assertThat(metadata["destination_type"]).isEqualTo("navigate")
    assertThat(metadata["destination_route"]).isEqualTo("settings")
    assertThat(metadata["mode"]).isEqualTo("home")
  }

  @Test
  fun `trackDrawerToggle encodes drawer state`() {
    shellTelemetry.trackDrawerToggle(
      DrawerSide.RIGHT,
      isOpen = true,
      panel = RightPanel.MODEL_SELECTOR,
      activeMode = ModeId.LIBRARY,
    )

    val metadataSlot = slot<Map<String, String>>()
    verify { telemetryReporter.trackInteraction("shell.drawer.toggle", capture(metadataSlot)) }
    val metadata = metadataSlot.captured
    assertThat(metadata["side"]).isEqualTo("right")
    assertThat(metadata["open"]).isEqualTo("true")
    assertThat(metadata["panel"]).isEqualTo("model_selector")
  }

  @Test
  fun `trackProgressJobQueued forwards retry metadata`() {
    val job =
      ProgressJob(
        jobId = UUID.randomUUID(),
        type = JobType.MODEL_DOWNLOAD,
        status = JobStatus.PENDING,
        progress = 0f,
        canRetry = true,
      )

    shellTelemetry.trackProgressJobQueued(job, offline = true, activeMode = ModeId.HOME)

    val metadataSlot = slot<Map<String, String>>()
    verify {
      telemetryReporter.trackInteraction("shell.progress.job_queued", capture(metadataSlot))
    }
    val metadata = metadataSlot.captured
    assertThat(metadata["job_type"]).isEqualTo("model_download")
    assertThat(metadata["job_status"]).isEqualTo("pending")
    assertThat(metadata["can_retry"]).isEqualTo("true")
    assertThat(metadata["offline"]).isEqualTo("true")
  }

  @Test
  fun `trackCommandPaletteDismissed normalizes metadata`() {
    shellTelemetry.trackCommandPaletteDismissed(PaletteDismissReason.BACK_PRESSED, ModeId.HISTORY)

    val metadataSlot = slot<Map<String, String>>()
    verify {
      telemetryReporter.trackInteraction("shell.command_palette.dismiss", capture(metadataSlot))
    }
    val metadata = metadataSlot.captured
    assertThat(metadata["reason"]).isEqualTo("back_pressed")
    assertThat(metadata["mode"]).isEqualTo("history")
  }
}
