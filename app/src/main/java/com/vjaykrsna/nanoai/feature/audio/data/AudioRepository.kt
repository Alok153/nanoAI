package com.vjaykrsna.nanoai.feature.audio.data

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import kotlinx.coroutines.flow.Flow

/** Repository interface for audio operations. */
interface AudioRepository {

  /** Starts audio recording. */
  suspend fun startRecording(): NanoAIResult<Unit>

  /** Stops audio recording. */
  suspend fun stopRecording(): NanoAIResult<Unit>

  /** Toggles microphone mute. */
  suspend fun toggleMute(): NanoAIResult<Boolean>

  /** Toggles speaker output. */
  suspend fun toggleSpeaker(): NanoAIResult<Boolean>

  /** Gets real-time waveform data. */
  fun getWaveformData(): Flow<List<Float>>

  /** Checks microphone permission status. */
  suspend fun checkMicrophonePermission(): NanoAIResult<Boolean>
}
