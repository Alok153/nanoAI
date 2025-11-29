package com.vjaykrsna.nanoai.core.data.audio

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class AudioRepositoryImplTest {

  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  private val context: Context = mockk(relaxed = true)
  private val repository = AudioRepositoryImpl(context)

  @Test
  fun getWaveformData_emitsData_onlyWhenRecording() =
    runTest(dispatcher) {
      val emissions = mutableListOf<List<Float>>()
      val job = launch { repository.getWaveformData().collect { emissions.add(it) } }

      // Initial state: not recording. Should emit empty list.
      advanceTimeBy(1)
      assertThat(emissions).isNotEmpty()
      assertThat(emissions.last()).isEmpty()

      // Start recording
      repository.startRecording()
      advanceTimeBy(200) // Wait for emissions

      // Should have new non-empty data
      assertThat(emissions.size).isGreaterThan(1)
      assertThat(emissions.last()).isNotEmpty()
      val countAfterStart = emissions.size

      // Stop recording
      repository.stopRecording()
      advanceTimeBy(200)

      // Should stop emitting. The flow might emit one last empty list when switching back to false.
      // We check that it doesn't keep emitting new data indefinitely.
      // The count should stabilize.
      val countAfterStop = emissions.size
      assertThat(countAfterStop)
        .isAtMost(countAfterStart + 2) // Allow for 1-2 extra emissions during switch

      // Verify the last emission is empty (since we switched to false)
      assertThat(emissions.last()).isEmpty()

      job.cancel()
    }

  @Test
  fun toggleMute_togglesState() =
    runTest(dispatcher) {
      // Initial state should be false (unmuted)
      // First toggle -> true
      val result1 = repository.toggleMute()
      assertThat(result1).isInstanceOf(NanoAIResult.Success::class.java)
      assertThat((result1 as NanoAIResult.Success).value).isTrue()

      // Second toggle -> false
      val result2 = repository.toggleMute()
      assertThat(result2).isInstanceOf(NanoAIResult.Success::class.java)
      assertThat((result2 as NanoAIResult.Success).value).isFalse()
    }

  @Test
  fun toggleSpeaker_togglesState() =
    runTest(dispatcher) {
      // Initial state should be false (speaker off)
      // First toggle -> true
      val result1 = repository.toggleSpeaker()
      assertThat(result1).isInstanceOf(NanoAIResult.Success::class.java)
      assertThat((result1 as NanoAIResult.Success).value).isTrue()

      // Second toggle -> false
      val result2 = repository.toggleSpeaker()
      assertThat(result2).isInstanceOf(NanoAIResult.Success::class.java)
      assertThat((result2 as NanoAIResult.Success).value).isFalse()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherExtension(
  val dispatcher: kotlinx.coroutines.test.TestDispatcher = StandardTestDispatcher()
) :
  org.junit.jupiter.api.extension.BeforeEachCallback,
  org.junit.jupiter.api.extension.AfterEachCallback {

  override fun beforeEach(context: org.junit.jupiter.api.extension.ExtensionContext) {
    kotlinx.coroutines.Dispatchers.setMain(dispatcher)
  }

  override fun afterEach(context: org.junit.jupiter.api.extension.ExtensionContext) {
    kotlinx.coroutines.Dispatchers.resetMain()
  }
}
