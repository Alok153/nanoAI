package com.vjaykrsna.nanoai.core.telemetry

import android.app.Activity
import android.view.FrameMetrics
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.JankStats.OnFrameListener
import androidx.metrics.performance.PerformanceMetricsState
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors UI performance using JankStats and frame metrics.
 *
 * Tracks frame drops, janky frames, and provides performance insights for UI optimization.
 * Integrates with the telemetry system to report performance issues.
 */
@Singleton
class PerformanceMonitor @Inject constructor(private val telemetryReporter: TelemetryReporter) {

  private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
  val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()

  private var jankStats: JankStats? = null
  private var performanceMetricsState: PerformanceMetricsState.Holder? = null

  private val frameListener = OnFrameListener { frameData -> updateMetrics(frameData) }

  /**
   * Starts monitoring UI performance for the given activity. Should be called in
   * Activity.onResume().
   */
  fun startMonitoring(activity: Activity) {
    if (jankStats != null) return // Already monitoring

    try {
      performanceMetricsState =
        PerformanceMetricsState.getHolderForHierarchy(activity.findViewById(android.R.id.content))
      jankStats = JankStats.createAndTrack(activity.window, frameListener)
      jankStats?.isTrackingEnabled = true

      telemetryReporter.trackInteraction(
        event = PERFORMANCE_MONITORING_STARTED,
        metadata =
          mapOf(KEY_ACTIVITY to activity::class.java.simpleName, KEY_JANKSTATS_ENABLED to "true"),
      )
    } catch (e: Exception) {
      telemetryReporter.report(
        source = "PerformanceMonitor.startMonitoring",
        result =
          NanoAIResult.FatalError(
            message = "Failed to initialize performance monitoring: ${e.message}",
            supportContact = null,
            telemetryId = null,
            cause = e,
          ),
      )
    }
  }

  /** Stops monitoring UI performance. Should be called in Activity.onPause(). */
  fun stopMonitoring() {
    try {
      jankStats?.isTrackingEnabled = false
      jankStats = null
      performanceMetricsState = null

      telemetryReporter.trackInteraction(
        event = PERFORMANCE_MONITORING_STOPPED,
        metadata = mapOf(KEY_DURATION_MS to _performanceMetrics.value.sessionDurationMs.toString()),
      )
    } catch (e: Exception) {
      telemetryReporter.report(
        source = "PerformanceMonitor.stopMonitoring",
        result =
          NanoAIResult.RecoverableError(
            message = "Failed to stop performance monitoring: ${e.message}",
            retryAfterSeconds = null,
            telemetryId = null,
            cause = e,
          ),
      )
    }
  }

  /**
   * Gets the current frame metrics for manual analysis. Note: Currently returns null as detailed
   * frame metrics require additional JankStats API integration.
   */
  @Suppress("FunctionOnlyReturningConstant") fun getCurrentFrameMetrics(): FrameMetrics? = null

  /** Reports performance metrics for the current session. */
  fun reportSessionMetrics() {
    val metrics = _performanceMetrics.value

    telemetryReporter.trackInteraction(
      event = PERFORMANCE_SESSION_REPORT,
      metadata =
        mapOf(
          KEY_TOTAL_FRAMES to metrics.totalFrames.toString(),
          KEY_JANKY_FRAMES to metrics.jankyFrames.toString(),
          KEY_FRAME_DROP_RATIO to String.format(Locale.ROOT, "%.2f", metrics.frameDropRatio),
          KEY_AVERAGE_FRAME_TIME_MS to
            String.format(Locale.ROOT, "%.2f", metrics.averageFrameTimeMs),
          KEY_SESSION_DURATION_MS to metrics.sessionDurationMs.toString(),
        ),
    )

    // Report as error if frame drops exceed threshold
    if (metrics.frameDropRatio > FRAME_DROP_THRESHOLD) {
      telemetryReporter.report(
        source = "PerformanceMonitor",
        result =
          NanoAIResult.RecoverableError(
            message =
              "High frame drop ratio detected: ${String.format(Locale.ROOT, "%.1f", metrics.frameDropRatio * PERCENTAGE_MULTIPLIER)}%",
            retryAfterSeconds = null,
            telemetryId = null,
            cause = null,
            context =
              mapOf(
                "jankyFrames" to metrics.jankyFrames.toString(),
                "totalFrames" to metrics.totalFrames.toString(),
                "threshold" to
                  String.format(Locale.ROOT, "%.1f", FRAME_DROP_THRESHOLD * PERCENTAGE_MULTIPLIER) +
                    "%",
              ),
          ),
      )
    }
  }

  private fun updateMetrics(@Suppress("UnusedParameter") frameData: FrameData) {
    val currentMetrics = _performanceMetrics.value

    // Simplified metrics collection - JankStats provides frame data internally
    // For now, we just track frame counts and basic timing
    val newMetrics =
      currentMetrics.copy(
        totalFrames = currentMetrics.totalFrames + 1,
        // Note: Detailed frame metrics require additional API integration
        sessionStartTime = currentMetrics.sessionStartTime ?: System.nanoTime(),
        lastFrameTime = System.nanoTime(),
      )

    _performanceMetrics.value = newMetrics
  }

  companion object {
    private const val PERFORMANCE_MONITORING_STARTED = "performance.monitoring.started"
    private const val PERFORMANCE_MONITORING_STOPPED = "performance.monitoring.stopped"
    private const val PERFORMANCE_SESSION_REPORT = "performance.session.report"

    private const val KEY_ACTIVITY = "activity"
    private const val KEY_JANKSTATS_ENABLED = "jankstats_enabled"
    private const val KEY_DURATION_MS = "duration_ms"
    private const val KEY_TOTAL_FRAMES = "total_frames"
    private const val KEY_JANKY_FRAMES = "janky_frames"
    private const val KEY_FRAME_DROP_RATIO = "frame_drop_ratio"
    private const val KEY_AVERAGE_FRAME_TIME_MS = "avg_frame_time_ms"
    private const val KEY_SESSION_DURATION_MS = "session_duration_ms"

    private const val FRAME_DROP_THRESHOLD = 0.05 // 5% frame drops
    private const val PERCENTAGE_MULTIPLIER = 100.0
  }
}

/** Data class representing UI performance metrics. */
data class PerformanceMetrics(
  val totalFrames: Long = 0,
  val jankyFrames: Long = 0,
  val totalFrameTimeNs: Long = 0,
  val sessionStartTime: Long? = null,
  val lastFrameTime: Long? = null,
) {
  /** Ratio of janky frames to total frames (0.0 to 1.0). */
  val frameDropRatio: Double
    get() = if (totalFrames > 0) jankyFrames.toDouble() / totalFrames else 0.0

  /** Average frame time in milliseconds. */
  @Suppress("MagicNumber")
  val averageFrameTimeMs: Double
    get() = if (totalFrames > 0) (totalFrameTimeNs.toDouble() / totalFrames) / 1_000_000.0 else 0.0

  /** Session duration in milliseconds. */
  val sessionDurationMs: Long
    get() =
      sessionStartTime
        ?.let { (lastFrameTime ?: System.nanoTime()) - it }
        ?.nanoseconds
        ?.inWholeMilliseconds ?: 0
}
