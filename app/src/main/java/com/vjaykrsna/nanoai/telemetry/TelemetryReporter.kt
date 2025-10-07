package com.vjaykrsna.nanoai.telemetry

import android.util.Log
import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreferenceStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Centralizes telemetry publishing for success and error results flowing through the app. Emits
 * structured events for observability while mirroring them to Logcat for debugging.
 */
@Singleton
class TelemetryReporter
@Inject
constructor(
  private val privacyPreferenceStore: PrivacyPreferenceStore,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
  @Volatile private var telemetryEnabled: Boolean = false

  init {
    scope.launch {
      privacyPreferenceStore.privacyPreference.collectLatest { preference ->
        telemetryEnabled = preference.telemetryOptIn
      }
    }
  }

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
        if (emit(payload)) {
          Log.i(TAG, payload.toLogMessage())
        }
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
        if (emit(payload)) {
          Log.w(TAG, payload.toLogMessage())
        }
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
        val emitted = emit(payload)
        if (emitted) {
          Log.e(TAG, payload.toLogMessage(), result.cause)
        }
      }
    }
  }

  fun trackInteraction(event: String, metadata: Map<String, String> = emptyMap()) {
    val payload = TelemetryEvent.Interaction(name = event, metadata = metadata)
    if (emit(payload)) {
      Log.i(TAG, payload.toLogMessage())
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

    data class Interaction(
      val name: String,
      val metadata: Map<String, String>,
    ) : TelemetryEvent()
  }

  private fun TelemetryEvent.toLogMessage(): String =
    when (this) {
      is TelemetryEvent.Success -> buildSuccessMessage()
      is TelemetryEvent.Recoverable -> buildRecoverableMessage()
      is TelemetryEvent.Fatal -> buildFatalMessage()
      is TelemetryEvent.Interaction -> buildInteractionMessage()
    }

  private fun TelemetryEvent.Success.buildSuccessMessage(): String = buildString {
    append(source)
    append(" success")
    if (metadata.isNotEmpty()) {
      append(" metadata=")
      append(metadata)
    }
  }

  private fun TelemetryEvent.Recoverable.buildRecoverableMessage(): String = buildString {
    append(source)
    append(" recoverable error: ")
    append(message)
    appendRetryAfter(retryAfterSeconds)
    appendTelemetryId(telemetryId)
    appendContext(context)
  }

  private fun TelemetryEvent.Fatal.buildFatalMessage(): String = buildString {
    append(source)
    append(" fatal error: ")
    append(message)
    appendTelemetryId(telemetryId)
    supportContact?.let {
      append(" supportContact=")
      append(it)
    }
    appendContext(context)
  }

  private fun TelemetryEvent.Interaction.buildInteractionMessage(): String = buildString {
    append(name)
    append(" interaction")
    appendMetadata(metadata)
  }

  private fun StringBuilder.appendRetryAfter(retryAfterSeconds: Long?) {
    retryAfterSeconds?.let {
      append(" retryAfter=")
      append(it)
      append("s")
    }
  }

  private fun StringBuilder.appendTelemetryId(telemetryId: String?) {
    telemetryId?.let {
      append(" telemetryId=")
      append(it)
    }
  }

  private fun StringBuilder.appendContext(context: Map<String, String>) {
    if (context.isNotEmpty()) {
      append(" context=")
      append(context)
    }
  }

  private fun StringBuilder.appendMetadata(metadata: Map<String, String>) {
    if (metadata.isNotEmpty()) {
      append(" metadata=")
      append(metadata)
    }
  }

  private fun emit(event: TelemetryEvent): Boolean {
    if (!telemetryEnabled) {
      return false
    }
    val emitted = _events.tryEmit(event)
    if (!emitted) {
      Log.w(TAG, "Dropped telemetry event: $event")
    }
    return emitted
  }

  companion object {
    private const val TAG = "TelemetryReporter"
  }
}
