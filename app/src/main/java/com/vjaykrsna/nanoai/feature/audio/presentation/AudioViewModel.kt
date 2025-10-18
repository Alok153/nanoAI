package com.vjaykrsna.nanoai.feature.audio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for audio/voice calling interface.
 *
 * Manages audio session state, waveform data, mute/speaker controls, and call status.
 */
@HiltViewModel
class AudioViewModel
@Inject
constructor(
  @MainImmediateDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

  private companion object {
    private const val WAVEFORM_UPDATE_DELAY_MS = 100L
    private const val SIMULATED_WAVEFORM_SIZE = 50
  }

  private val _uiState = MutableStateFlow(AudioUiState())
  val uiState: StateFlow<AudioUiState> = _uiState.asStateFlow()

  private val _errorEvents = MutableSharedFlow<AudioError>()
  val errorEvents = _errorEvents.asSharedFlow()

  fun startAudioSession() {
    viewModelScope.launch(dispatcher) {
      _uiState.value =
        _uiState.value.copy(
          sessionState = AudioSessionState.ACTIVE,
          errorMessage = null,
        )

      // TODO: Implement actual audio session logic
      // Simulate waveform updates
      while (_uiState.value.sessionState == AudioSessionState.ACTIVE) {
        val waveform = generateSimulatedWaveform()
        _uiState.value = _uiState.value.copy(waveformData = waveform)
        kotlinx.coroutines.delay(WAVEFORM_UPDATE_DELAY_MS)
      }
    }
  }

  fun toggleMute() {
    _uiState.value = _uiState.value.copy(isMuted = !_uiState.value.isMuted)
  }

  fun toggleSpeaker() {
    _uiState.value = _uiState.value.copy(isSpeakerOn = !_uiState.value.isSpeakerOn)
  }

  fun endSession() {
    _uiState.value =
      _uiState.value.copy(
        sessionState = AudioSessionState.ENDED,
        waveformData = emptyList(),
      )
  }

  private fun generateSimulatedWaveform(): List<Float> {
    return List(SIMULATED_WAVEFORM_SIZE) { kotlin.random.Random.nextFloat() }
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }
}

/** UI state for audio/voice screen. */
data class AudioUiState(
  val sessionState: AudioSessionState = AudioSessionState.IDLE,
  val isMuted: Boolean = false,
  val isSpeakerOn: Boolean = false,
  val waveformData: List<Float> = emptyList(),
  val errorMessage: String? = null,
)

/** Audio session state. */
enum class AudioSessionState {
  IDLE,
  ACTIVE,
  ENDED,
}

/** Error states for audio session. */
sealed interface AudioError {
  data class SessionError(val message: String) : AudioError

  data class PermissionError(val message: String) : AudioError
}
