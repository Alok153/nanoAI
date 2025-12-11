package com.vjaykrsna.nanoai.feature.audio.data

import com.vjaykrsna.nanoai.core.domain.audio.AudioRepository
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import com.vjaykrsna.nanoai.feature.audio.domain.AudioFeatureRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Data source abstraction for audio hardware/runtime operations. */
interface AudioFeatureDataSource {
  suspend fun startSession(): NanoAIResult<Unit>

  suspend fun endSession(): NanoAIResult<Unit>

  suspend fun toggleMute(): NanoAIResult<Boolean>

  suspend fun toggleSpeaker(): NanoAIResult<Boolean>

  fun observeWaveform(): Flow<List<Float>>

  suspend fun checkMicrophonePermission(): NanoAIResult<Boolean>
}

/** Data source implementation that delegates to the shared audio repository. */
@Singleton
class CoreAudioFeatureDataSource @Inject constructor(private val audioRepository: AudioRepository) :
  AudioFeatureDataSource {

  override suspend fun startSession(): NanoAIResult<Unit> = audioRepository.startRecording()

  override suspend fun endSession(): NanoAIResult<Unit> = audioRepository.stopRecording()

  override suspend fun toggleMute(): NanoAIResult<Boolean> = audioRepository.toggleMute()

  override suspend fun toggleSpeaker(): NanoAIResult<Boolean> = audioRepository.toggleSpeaker()

  override fun observeWaveform(): Flow<List<Float>> = audioRepository.getWaveformData()

  override suspend fun checkMicrophonePermission(): NanoAIResult<Boolean> =
    audioRepository.checkMicrophonePermission()
}

/** Repository implementation bridging the domain contract to the data source. */
@Singleton
class DefaultAudioFeatureRepository
@Inject
constructor(private val dataSource: AudioFeatureDataSource) : AudioFeatureRepository {

  override suspend fun startSession(): NanoAIResult<Unit> = dataSource.startSession()

  override suspend fun endSession(): NanoAIResult<Unit> = dataSource.endSession()

  override suspend fun toggleMute(): NanoAIResult<Boolean> = dataSource.toggleMute()

  override suspend fun toggleSpeaker(): NanoAIResult<Boolean> = dataSource.toggleSpeaker()

  override fun observeWaveform(): Flow<List<Float>> = dataSource.observeWaveform()

  override suspend fun checkMicrophonePermission(): NanoAIResult<Boolean> =
    dataSource.checkMicrophonePermission()
}
