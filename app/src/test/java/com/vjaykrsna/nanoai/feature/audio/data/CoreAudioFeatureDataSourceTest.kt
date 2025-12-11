package com.vjaykrsna.nanoai.feature.audio.data

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.audio.AudioRepository
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import com.vjaykrsna.nanoai.core.model.NanoAISuccess
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoreAudioFeatureDataSourceTest {
  private val fakeRepository = FakeAudioRepository()
  private val dataSource = CoreAudioFeatureDataSource(fakeRepository)

  @Test
  fun startSession_delegatesToRepository() = runTest {
    val result = dataSource.startSession()

    assertThat(fakeRepository.startCalls).isEqualTo(1)
    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
  }

  @Test
  fun endSession_delegatesToRepository() = runTest {
    val result = dataSource.endSession()

    assertThat(fakeRepository.stopCalls).isEqualTo(1)
    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
  }

  @Test
  fun observeWaveform_streamsFromRepository() = runTest {
    val expected = listOf(0.1f, 0.2f)
    fakeRepository.waveform.value = expected

    val observed = dataSource.observeWaveform().first()

    assertThat(observed).isEqualTo(expected)
  }

  private class FakeAudioRepository : AudioRepository {
    val waveform = MutableStateFlow<List<Float>>(emptyList())
    var startCalls = 0
    var stopCalls = 0
    var toggleMuteCalls = 0
    var toggleSpeakerCalls = 0
    var permissionChecks = 0

    override suspend fun startRecording(): NanoAIResult<Unit> {
      startCalls += 1
      return NanoAIResult.success(Unit)
    }

    override suspend fun stopRecording(): NanoAIResult<Unit> {
      stopCalls += 1
      return NanoAIResult.success(Unit)
    }

    override suspend fun toggleMute(): NanoAIResult<Boolean> {
      toggleMuteCalls += 1
      return NanoAIResult.success(true)
    }

    override suspend fun toggleSpeaker(): NanoAIResult<Boolean> {
      toggleSpeakerCalls += 1
      return NanoAIResult.success(false)
    }

    override fun getWaveformData() = waveform

    override suspend fun checkMicrophonePermission(): NanoAIResult<Boolean> {
      permissionChecks += 1
      return NanoAIResult.success(true)
    }
  }
}
