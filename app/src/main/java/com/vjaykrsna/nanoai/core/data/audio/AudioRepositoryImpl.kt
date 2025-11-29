package com.vjaykrsna.nanoai.core.data.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.audio.AudioRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * Implementation of audio repository.
 *
 * TODO: Implement actual audio recording/playback logic
 */
class AudioRepositoryImpl @Inject constructor(private val context: Context) : AudioRepository {

  private val isRecording = MutableStateFlow(false)
  private var isMuted = false
  private var isSpeakerOn = false

  companion object {
    private const val WAVEFORM_SAMPLE_COUNT = 50
    private const val WAVEFORM_UPDATE_DELAY_MS = 100L
    private const val PHASE_INCREMENT = 0.2
    private const val NORMALIZATION_FACTOR = 1.7f
    private const val SECOND_HARMONIC_WEIGHT = 0.5
    private const val SECOND_HARMONIC_MULTIPLIER = 2.0
    private const val SECOND_HARMONIC_OFFSET = 1.0
    private const val NOISE_SCALE = 0.2f
    private const val CYCLE_MULTIPLIER = 2.0
  }

  override suspend fun startRecording(): NanoAIResult<Unit> {
    isRecording.value = true
    return NanoAIResult.Success(Unit)
  }

  override suspend fun stopRecording(): NanoAIResult<Unit> {
    isRecording.value = false
    return NanoAIResult.Success(Unit)
  }

  override suspend fun toggleMute(): NanoAIResult<Boolean> {
    isMuted = !isMuted
    return NanoAIResult.Success(isMuted)
  }

  override suspend fun toggleSpeaker(): NanoAIResult<Boolean> {
    isSpeakerOn = !isSpeakerOn
    return NanoAIResult.Success(isSpeakerOn)
  }

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  override fun getWaveformData(): Flow<List<Float>> {
    return isRecording.flatMapLatest { recording ->
      if (recording) {
        flow {
          var phase = 0.0
          while (true) {
            val waveform =
              List(WAVEFORM_SAMPLE_COUNT) { index ->
                // Generate a smooth wave: sin(t) + 0.5*sin(2t) + small noise
                val t =
                  phase + (index.toDouble() / WAVEFORM_SAMPLE_COUNT) * CYCLE_MULTIPLIER * Math.PI
                val value =
                  (Math.sin(t) +
                      SECOND_HARMONIC_WEIGHT *
                        Math.sin(SECOND_HARMONIC_MULTIPLIER * t + SECOND_HARMONIC_OFFSET) +
                      kotlin.random.Random.nextFloat() * NOISE_SCALE)
                    .toFloat()
                // Normalize roughly to -1..1 range
                value / NORMALIZATION_FACTOR
              }
            emit(waveform)
            phase += PHASE_INCREMENT // Advance phase for animation
            kotlinx.coroutines.delay(WAVEFORM_UPDATE_DELAY_MS)
          }
        }
      } else {
        flowOf(emptyList())
      }
    }
  }

  override suspend fun checkMicrophonePermission(): NanoAIResult<Boolean> {
    val granted =
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    return NanoAIResult.Success(granted)
  }
}
