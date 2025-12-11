package com.vjaykrsna.nanoai.feature.audio.data

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import com.vjaykrsna.nanoai.core.model.NanoAISuccess
import com.vjaykrsna.nanoai.feature.audio.domain.AudioSessionCoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAudioFeatureRepositoryTest {
  private val fakeDataSource = FakeAudioFeatureDataSource()
  private val repository = DefaultAudioFeatureRepository(fakeDataSource)
  private val coordinator = AudioSessionCoordinator(repository)

  @Test
  fun startSession_delegatesToDataSource() = runTest {
    val result = repository.startSession()

    assertThat(fakeDataSource.startCalls).isEqualTo(1)
    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
  }

  @Test
  fun observeWaveform_emitsFromDataSource() = runTest {
    val expected = listOf(0.1f, 0.2f)
    fakeDataSource.waveformFlow.value = expected

    val observed = repository.observeWaveform().first()

    assertThat(observed).isEqualTo(expected)
  }

  @Test
  fun coordinator_routesCallsThroughRepository() = runTest {
    val result = coordinator.endSession()

    assertThat(fakeDataSource.endCalls).isEqualTo(1)
    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
  }
}

private class FakeAudioFeatureDataSource : AudioFeatureDataSource {
  val waveformFlow = MutableStateFlow<List<Float>>(emptyList())
  var startCalls = 0
  var endCalls = 0

  override suspend fun startSession(): NanoAIResult<Unit> {
    startCalls += 1
    return NanoAIResult.success(Unit)
  }

  override suspend fun endSession(): NanoAIResult<Unit> {
    endCalls += 1
    return NanoAIResult.success(Unit)
  }

  override suspend fun toggleMute(): NanoAIResult<Boolean> = NanoAIResult.success(true)

  override suspend fun toggleSpeaker(): NanoAIResult<Boolean> = NanoAIResult.success(false)

  override fun observeWaveform() = waveformFlow

  override suspend fun checkMicrophonePermission(): NanoAIResult<Boolean> =
    NanoAIResult.success(true)
}
