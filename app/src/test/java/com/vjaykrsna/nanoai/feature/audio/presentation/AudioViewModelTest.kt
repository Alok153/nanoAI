package com.vjaykrsna.nanoai.feature.audio.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class AudioViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  private lateinit var viewModel: AudioViewModel

  @BeforeEach
  fun setup() {
    viewModel = AudioViewModel(dispatcher)
  }

  @Test
  fun startAudioSession_updatesStateAndWaveform() =
    runTest(dispatcher) {
      viewModel.startAudioSession()
      advanceTimeBy(500)

      val state = viewModel.state.value
      assertThat(state.sessionState).isEqualTo(AudioSessionState.ACTIVE)
      assertThat(state.waveformData).isNotEmpty()

      viewModel.endSession()
      advanceUntilIdle()
    }

  @Test
  fun endSession_emitsSessionEndedEvent() =
    runTest(dispatcher) {
      val eventDeferred = async { viewModel.events.first() }

      viewModel.endSession()
      advanceUntilIdle()

      val event = eventDeferred.await()
      assertThat(event).isEqualTo(AudioUiEvent.SessionEnded)
    }

  @Test
  fun toggleMuteAndSpeaker_updateStateFlags() {
    viewModel.toggleMute()
    viewModel.toggleSpeaker()

    val state = viewModel.state.value
    assertThat(state.isMuted).isTrue()
    assertThat(state.isSpeakerOn).isTrue()
  }
}
