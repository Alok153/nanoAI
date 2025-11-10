package com.vjaykrsna.nanoai.shared.state

import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

private data class TestState(val counter: Int = 0, val message: String? = null) : NanoAIViewState

private sealed interface TestEvent : NanoAIViewEvent {
  data class Snackbar(val message: String) : TestEvent
}

private class TestViewModel(testDispatcher: CoroutineDispatcher) :
  ViewModelStateHost<TestState, TestEvent>(initialState = TestState(), testDispatcher) {

  fun increment() {
    updateState { copy(counter = counter + 1) }
  }

  fun setMessage(message: String) {
    updateState { copy(message = message) }
  }

  fun fireEvent(message: String) {
    viewModelScope.launch(dispatcher) { emitEvent(TestEvent.Snackbar(message)) }
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelStateHostTest {

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension()

  @Test
  fun `initial state is exposed`() =
    runTest(mainDispatcherExtension.dispatcher) {
      val viewModel = TestViewModel(mainDispatcherExtension.dispatcher)
      val harness = ViewModelStateHostTestHarness(viewModel)

      assertThat(harness.currentState.counter).isEqualTo(0)

      harness.testStates { assertThat(awaitItem().counter).isEqualTo(0) }

      viewModel.viewModelScope.cancel()
    }

  @Test
  fun `updateState emits new snapshot`() =
    runTest(mainDispatcherExtension.dispatcher) {
      val viewModel = TestViewModel(mainDispatcherExtension.dispatcher)
      val harness = ViewModelStateHostTestHarness(viewModel)

      viewModel.increment()
      advanceUntilIdle()

      val snapshot = harness.awaitState(predicate = { it.counter == 1 })
      assertThat(snapshot.counter).isEqualTo(1)

      viewModel.viewModelScope.cancel()
    }

  @Test
  fun `emitEvent surfaces through channel`() =
    runTest(mainDispatcherExtension.dispatcher) {
      val viewModel = TestViewModel(mainDispatcherExtension.dispatcher)
      val harness = ViewModelStateHostTestHarness(viewModel)

      harness.testEvents {
        viewModel.fireEvent("hello")
        val event = awaitItem()
        assertThat(event).isInstanceOf(TestEvent.Snackbar::class.java)
        assertThat((event as TestEvent.Snackbar).message).isEqualTo("hello")
      }

      viewModel.viewModelScope.cancel()
    }
}
