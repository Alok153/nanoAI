package com.vjaykrsna.nanoai.core.domain.audio

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Implementation of audio session management use case. */
class AudioSessionUseCaseImpl
@Inject
constructor(
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val audioRepository: AudioRepository,
) : AudioSessionUseCase {

  override suspend fun startSession(): NanoAIResult<Unit> =
    withContext(ioDispatcher) { audioRepository.startRecording() }

  override suspend fun endSession(): NanoAIResult<Unit> =
    withContext(ioDispatcher) { audioRepository.stopRecording() }

  override suspend fun toggleMute(): NanoAIResult<Boolean> =
    withContext(ioDispatcher) { audioRepository.toggleMute() }

  override suspend fun toggleSpeaker(): NanoAIResult<Boolean> =
    withContext(ioDispatcher) { audioRepository.toggleSpeaker() }

  override fun getWaveformData(): Flow<List<Float>> {
    return audioRepository.getWaveformData()
  }

  override suspend fun checkMicrophonePermission(): NanoAIResult<Boolean> =
    withContext(ioDispatcher) { audioRepository.checkMicrophonePermission() }
}
