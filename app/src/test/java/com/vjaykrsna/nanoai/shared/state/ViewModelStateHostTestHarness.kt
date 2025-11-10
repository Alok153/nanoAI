package com.vjaykrsna.nanoai.shared.state

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

/** Test helper that wires Turbine collectors to a [ViewModelStateHost]. */
class ViewModelStateHostTestHarness<S : NanoAIViewState, E : NanoAIViewEvent>
constructor(
  private val viewModel: ViewModelStateHost<S, E>,
  private val defaultTimeout: Duration = 5.seconds,
) {

  /** Returns the current state snapshot synchronously. */
  val currentState: S
    get() = viewModel.state.value

  /** Collects state emissions with Turbine. */
  suspend fun testStates(
    timeout: Duration = defaultTimeout,
    block: suspend TurbineTestContext<S>.() -> Unit,
  ) {
    viewModel.state.test(timeout = timeout) {
      try {
        block()
      } finally {
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  /** Collects event emissions with Turbine. */
  suspend fun testEvents(
    timeout: Duration = defaultTimeout,
    block: suspend TurbineTestContext<E>.() -> Unit,
  ) {
    viewModel.events.test(timeout = timeout) {
      try {
        block()
      } finally {
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  /** Awaits the next state that satisfies [predicate], failing after [timeout]. */
  suspend fun awaitState(predicate: (S) -> Boolean, timeout: Duration = defaultTimeout): S =
    withTimeout(timeout) { viewModel.state.first { predicate(it) } }

  /** Awaits the next event that satisfies [predicate], failing after [timeout]. */
  suspend fun awaitEvent(predicate: (E) -> Boolean, timeout: Duration = defaultTimeout): E =
    withTimeout(timeout) { viewModel.events.first { predicate(it) } }
}
