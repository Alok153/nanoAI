package com.vjaykrsna.nanoai.feature.audio.presentation

import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.common.error.toErrorEnvelope
import com.vjaykrsna.nanoai.shared.state.NanoAIViewEvent
import com.vjaykrsna.nanoai.shared.state.NanoAIViewState
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHost
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel for audio/voice calling interface.
 *
 * Manages audio session state, waveform data, mute/speaker controls, and call status.
 */
@HiltViewModel
class AudioViewModel
@Inject
constructor(@MainImmediateDispatcher mainDispatcher: CoroutineDispatcher) :
  ViewModelStateHost<AudioUiState, AudioUiEvent>(
    initialState = AudioUiState(),
    dispatcher = mainDispatcher,
  ) {

  private companion object {
    private const val WAVEFORM_UPDATE_DELAY_MS = 100L
    private const val SIMULATED_WAVEFORM_SIZE = 50
    private const val SESSION_INTERRUPTED_ERROR = "Audio session interrupted"
  }

  fun startAudioSession() {
    viewModelScope.launch(dispatcher) {
      if (state.value.sessionState == AudioSessionState.ACTIVE) return@launch

      updateState { copy(sessionState = AudioSessionState.ACTIVE, errorMessage = null) }

      // Simulated waveform updates - actual implementation requires AudioFeatureRepository
      // integration with real audio capture via AudioSessionCoordinator
      try {
        while (state.value.sessionState == AudioSessionState.ACTIVE) {
          val waveform = generateSimulatedWaveform()
          updateState { copy(waveformData = waveform) }
          delay(WAVEFORM_UPDATE_DELAY_MS)
        }
      } catch (throwable: Throwable) {
        val envelope = throwable.toErrorEnvelope(SESSION_INTERRUPTED_ERROR)
        publishError(AudioError.SessionError(envelope.userMessage), envelope)
      }
    }
  }

  fun toggleMute() {
    updateState { copy(isMuted = !isMuted) }
  }

  fun toggleSpeaker() {
    updateState { copy(isSpeakerOn = !isSpeakerOn) }
  }

  fun endSession() {
    updateState { copy(sessionState = AudioSessionState.ENDED, waveformData = emptyList()) }
    viewModelScope.launch(dispatcher) { emitEvent(AudioUiEvent.SessionEnded) }
  }

  private fun generateSimulatedWaveform(): List<Float> {
    return List(SIMULATED_WAVEFORM_SIZE) { kotlin.random.Random.nextFloat() }
  }

  fun clearError() {
    updateState { copy(errorMessage = null) }
  }

  private suspend fun publishError(error: AudioError, envelope: NanoAIErrorEnvelope) {
    updateState {
      copy(
        sessionState = AudioSessionState.ENDED,
        waveformData = emptyList(),
        errorMessage = envelope.userMessage,
      )
    }
    emitEvent(AudioUiEvent.ErrorRaised(error, envelope))
  }
}

/** UI state for audio/voice screen. */
data class AudioUiState(
  val sessionState: AudioSessionState = AudioSessionState.IDLE,
  val isMuted: Boolean = false,
  val isSpeakerOn: Boolean = false,
  val waveformData: List<Float> = emptyList(),
  val errorMessage: String? = null,
) : NanoAIViewState

/** Audio session state. */
enum class AudioSessionState {
  IDLE,
  ACTIVE,
  ENDED,
}

/** Error states for audio session. */
sealed interface AudioError {
  val message: String

  data class SessionError(override val message: String) : AudioError

  data class PermissionError(override val message: String) : AudioError
}

sealed interface AudioUiEvent : NanoAIViewEvent {
  data class ErrorRaised(val error: AudioError, val envelope: NanoAIErrorEnvelope) : AudioUiEvent

  data object SessionEnded : AudioUiEvent
}
