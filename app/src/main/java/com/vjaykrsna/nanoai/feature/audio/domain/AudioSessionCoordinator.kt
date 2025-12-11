package com.vjaykrsna.nanoai.feature.audio.domain

import com.vjaykrsna.nanoai.core.model.NanoAIResult
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Feature-level audio contract exposing session controls to the UI layer. */
interface AudioFeatureRepository {
  suspend fun startSession(): NanoAIResult<Unit>

  suspend fun endSession(): NanoAIResult<Unit>

  suspend fun toggleMute(): NanoAIResult<Boolean>

  suspend fun toggleSpeaker(): NanoAIResult<Boolean>

  fun observeWaveform(): Flow<List<Float>>

  suspend fun checkMicrophonePermission(): NanoAIResult<Boolean>
}

/** Thin use case wrapper to preserve ViewModel → UseCase → Repository layering. */
class AudioSessionCoordinator @Inject constructor(private val repository: AudioFeatureRepository) {

  suspend fun startSession(): NanoAIResult<Unit> = repository.startSession()

  suspend fun endSession(): NanoAIResult<Unit> = repository.endSession()

  suspend fun toggleMute(): NanoAIResult<Boolean> = repository.toggleMute()

  suspend fun toggleSpeaker(): NanoAIResult<Boolean> = repository.toggleSpeaker()

  fun observeWaveform(): Flow<List<Float>> = repository.observeWaveform()

  suspend fun checkMicrophonePermission(): NanoAIResult<Boolean> =
    repository.checkMicrophonePermission()
}
