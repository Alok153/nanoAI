package com.vjaykrsna.nanoai.core.telemetry

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PerformanceMonitorTest {

  private val telemetryReporter: TelemetryReporter = mockk(relaxed = true)
  private lateinit var monitor: PerformanceMonitor

  @BeforeEach
  fun setUp() {
    monitor = PerformanceMonitor(telemetryReporter)
  }

  @Test
  fun `reportSessionMetrics emits telemetry and escalates frame drops`() {
    val metricsField = monitor.javaClass.getDeclaredField("_performanceMetrics")
    metricsField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val metricsState = metricsField.get(monitor) as MutableStateFlow<PerformanceMetrics>
    metricsState.value =
      PerformanceMetrics(
        totalFrames = 100,
        jankyFrames = 20,
        totalFrameTimeNs = 5_000_000_000,
        sessionStartTime = 0L,
        lastFrameTime = 50_000_000_000,
      )

    val metadataPayloads = mutableListOf<Map<String, String>>()
    every { telemetryReporter.trackInteraction("performance.session.report", any()) } answers
      {
        metadataPayloads += arg<Map<String, String>>(1)
      }

    val reportedErrors = mutableListOf<NanoAIResult<*>>()
    every { telemetryReporter.report("PerformanceMonitor", any()) } answers
      {
        reportedErrors += arg<NanoAIResult<*>>(1)
      }

    monitor.reportSessionMetrics()

    verify(exactly = 1) { telemetryReporter.trackInteraction("performance.session.report", any()) }
    verify(exactly = 1) { telemetryReporter.report("PerformanceMonitor", any()) }

    val metadata = metadataPayloads.single()
    assertThat(metadata).isNotEmpty()
    val error = reportedErrors.single()
    assertThat(error).isInstanceOf(NanoAIResult.RecoverableError::class.java)
    val recoverable = error as NanoAIResult.RecoverableError
    assertThat(recoverable.message).contains("High frame drop ratio")
    assertThat(recoverable.context["threshold"]).isEqualTo("5.0%")
  }
}
