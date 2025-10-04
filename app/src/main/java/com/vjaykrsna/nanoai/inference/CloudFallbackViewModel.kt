package com.vjaykrsna.nanoai.inference

import androidx.lifecycle.ViewModel
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Coordinates fallback behaviour when the on-device runtime declines a request and we escalate to
 * cloud inference. The ViewModel exposes telemetry metadata so the UI can guide the user through
 * retry flows or support escalation paths.
 */
@HiltViewModel
class CloudFallbackViewModel @Inject constructor() : ViewModel() {

  data class State(
    val error: String? = null,
    val telemetryId: String? = null,
    val retryInSeconds: Long? = null,
    val supportContact: String? = null,
  )

  private val _state = MutableStateFlow(State())
  val state: StateFlow<State> = _state.asStateFlow()

  /** Consume the result of an inference attempt and update the UI with retry or escalation data. */
  fun onInferenceResult(result: NanoAIResult<*>) {
    when (result) {
      is NanoAIResult.Success -> _state.value = State()
      is NanoAIResult.RecoverableError ->
        _state.update {
          it.copy(
            error = result.message,
            telemetryId = result.telemetryId,
            retryInSeconds = result.retryAfterSeconds,
            supportContact = null,
          )
        }
      is NanoAIResult.FatalError ->
        _state.update {
          it.copy(
            error = result.message,
            telemetryId = result.telemetryId,
            retryInSeconds = null,
            supportContact = result.supportContact,
          )
        }
    }
  }

  /** Clears any surfaced error state once user has acknowledged guidance. */
  fun clearError() {
    _state.value = State()
  }
}
