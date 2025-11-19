package com.vjaykrsna.nanoai.core.domain.audio

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.common.annotations.ReactiveStream
import kotlinx.coroutines.flow.Flow

/** Use case for managing audio sessions and processing. */
interface AudioSessionUseCase {

  /** Starts an audio recording session. */
  @OneShot("Start audio session") suspend fun startSession(): NanoAIResult<Unit>

  /** Ends the current audio session. */
  @OneShot("End audio session") suspend fun endSession(): NanoAIResult<Unit>

  /** Toggles microphone mute state. */
  @OneShot("Toggle session mute state") suspend fun toggleMute(): NanoAIResult<Boolean>

  /** Toggles speaker output. */
  @OneShot("Toggle session speaker output") suspend fun toggleSpeaker(): NanoAIResult<Boolean>

  /** Gets real-time waveform data flow. */
  @ReactiveStream("Observe session waveform") fun getWaveformData(): Flow<List<Float>>

  /** Checks if microphone permission is granted. */
  @OneShot("Check microphone permission")
  suspend fun checkMicrophonePermission(): NanoAIResult<Boolean>
}
