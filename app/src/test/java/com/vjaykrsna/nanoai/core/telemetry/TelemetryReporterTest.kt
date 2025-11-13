package com.vjaykrsna.nanoai.core.telemetry

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreferenceStore
import com.vjaykrsna.nanoai.core.domain.settings.model.PrivacyPreference
import com.vjaykrsna.nanoai.core.domain.settings.model.RetentionPolicy
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TelemetryReporterTest {

  @Test
  fun `emits success events when telemetry enabled`() = runTest {
    val preferenceFlow = MutableStateFlow(privacyOptIn(false))
    val store = mockk<PrivacyPreferenceStore>()
    every { store.privacyPreference } returns preferenceFlow
    val dispatcher = StandardTestDispatcher(testScheduler)

    val reporter = TelemetryReporter(store, dispatcher)

    preferenceFlow.value = privacyOptIn(true)
    testScheduler.runCurrent()

    reporter.events.test {
      reporter.report("unitTest", NanoAIResult.success(Unit))
      val event = awaitItem() as TelemetryReporter.TelemetryEvent.Success
      assertThat(event.source).isEqualTo("unitTest")
      assertThat(event.metadata).isEmpty()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `drops events when telemetry disabled`() = runTest {
    val preferenceFlow = MutableStateFlow(privacyOptIn(false))
    val store = mockk<PrivacyPreferenceStore>()
    every { store.privacyPreference } returns preferenceFlow
    val dispatcher = StandardTestDispatcher(testScheduler)

    val reporter = TelemetryReporter(store, dispatcher)

    reporter.events.test {
      reporter.trackInteraction(event = "disabled_event")
      expectNoEvents()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `emits fatal events with merged context`() = runTest {
    val preferenceFlow = MutableStateFlow(privacyOptIn(true))
    val store = mockk<PrivacyPreferenceStore>()
    every { store.privacyPreference } returns preferenceFlow
    val dispatcher = StandardTestDispatcher(testScheduler)

    val reporter = TelemetryReporter(store, dispatcher)
    testScheduler.runCurrent()

    reporter.events.test {
      val error =
        NanoAIResult.FatalError(
          message = "crash",
          supportContact = "support@nanoai.test",
          telemetryId = "fatal-123",
          context = mapOf("attempt" to "1"),
        )
      reporter.report("TelemetryReporterTest", error, extraContext = mapOf("foo" to "bar"))

      val event = awaitItem() as TelemetryReporter.TelemetryEvent.Fatal
      assertThat(event.message).isEqualTo("crash")
      assertThat(event.telemetryId).isEqualTo("fatal-123")
      assertThat(event.context).containsExactlyEntriesIn(mapOf("attempt" to "1", "foo" to "bar"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun privacyOptIn(optIn: Boolean): PrivacyPreference =
    PrivacyPreference(
      exportWarningsDismissed = false,
      telemetryOptIn = optIn,
      consentAcknowledgedAt = null,
      disclaimerShownCount = 0,
      retentionPolicy = RetentionPolicy.INDEFINITE,
    )
}
