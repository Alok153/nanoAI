package com.vjaykrsna.nanoai.core.domain.audio

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.common.annotations.ReactiveStream
import kotlinx.coroutines.flow.Flow

/** Repository interface for audio operations. */
interface AudioRepository {

  /** Starts audio recording. */
  @OneShot("Start audio recording session") suspend fun startRecording(): NanoAIResult<Unit>

  /** Stops audio recording. */
  @OneShot("Stop audio recording session") suspend fun stopRecording(): NanoAIResult<Unit>

  /** Toggles microphone mute. */
  @OneShot("Toggle microphone mute state") suspend fun toggleMute(): NanoAIResult<Boolean>

  /** Toggles speaker output. */
  @OneShot("Toggle speaker output") suspend fun toggleSpeaker(): NanoAIResult<Boolean>

  /** Gets real-time waveform data. */
  @ReactiveStream("Real-time waveform samples") fun getWaveformData(): Flow<List<Float>>

  /** Checks microphone permission status. */
  @OneShot("Check microphone permission state")
  suspend fun checkMicrophonePermission(): NanoAIResult<Boolean>
}
