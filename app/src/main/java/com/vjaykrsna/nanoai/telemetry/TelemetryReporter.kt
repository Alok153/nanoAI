package com.vjaykrsna.nanoai.telemetry

import android.util.Log
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Centralizes telemetry publishing for success and error results flowing through the app. Emits
 * structured events for observability while mirroring them to Logcat for debugging.
 */
@Singleton
class TelemetryReporter @Inject constructor() {

  private val _events = MutableSharedFlow<TelemetryEvent>(replay = 0, extraBufferCapacity = 16)
  val events: SharedFlow<TelemetryEvent> = _events.asSharedFlow()

  fun report(
    source: String,
    result: NanoAIResult<*>,
    extraContext: Map<String, String> = emptyMap(),
  ) {
    when (result) {
      is NanoAIResult.Success -> {
        val payload =
          TelemetryEvent.Success(source = source, metadata = result.metadata + extraContext)
        emit(payload)
        Log.i(TAG, payload.toLogMessage())
      }
      is NanoAIResult.RecoverableError -> {
        val payload =
          TelemetryEvent.Recoverable(
            source = source,
            message = result.message,
            retryAfterSeconds = result.retryAfterSeconds,
            telemetryId = result.telemetryId,
            context = result.context + extraContext,
          )
        emit(payload)
        Log.w(TAG, payload.toLogMessage())
      }
      is NanoAIResult.FatalError -> {
        val payload =
          TelemetryEvent.Fatal(
            source = source,
            message = result.message,
            telemetryId = result.telemetryId,
            supportContact = result.supportContact,
            context = extraContext,
          )
        emit(payload)
        Log.e(TAG, payload.toLogMessage(), result.cause)
      }
    }
  }

  private fun emit(event: TelemetryEvent) {
    if (!_events.tryEmit(event)) {
      Log.w(TAG, "Dropped telemetry event: $event")
    }
  }

  sealed class TelemetryEvent {
    data class Success(
      val source: String,
      val metadata: Map<String, String>,
    ) : TelemetryEvent()

    data class Recoverable(
      val source: String,
      val message: String,
      val retryAfterSeconds: Long?,
      val telemetryId: String?,
      val context: Map<String, String>,
    ) : TelemetryEvent()

    data class Fatal(
      val source: String,
      val message: String,
      val telemetryId: String?,
      val supportContact: String?,
      val context: Map<String, String>,
    ) : TelemetryEvent()
  }

  private fun TelemetryEvent.toLogMessage(): String =
    when (this) {
      is TelemetryEvent.Success ->
        buildString {
          append(source)
          append(" success")
          if (metadata.isNotEmpty()) {
            append(" metadata=")
            append(metadata)
          }
        }
      is TelemetryEvent.Recoverable ->
        buildString {
          append(source)
          append(" recoverable error: ")
          append(message)
          retryAfterSeconds?.let {
            append(" retryAfter=")
            append(it)
            append("s")
          }
          telemetryId?.let {
            append(" telemetryId=")
            append(it)
          }
          if (context.isNotEmpty()) {
            append(" context=")
            append(context)
          }
        }
      is TelemetryEvent.Fatal ->
        buildString {
          append(source)
          append(" fatal error: ")
          append(message)
          telemetryId?.let {
            append(" telemetryId=")
            append(it)
          }
          supportContact?.let {
            append(" supportContact=")
            append(it)
          }
          if (context.isNotEmpty()) {
            append(" context=")
            append(context)
          }
        }
    }

  companion object {
    private const val TAG = "TelemetryReporter"
  }
}
