package com.vjaykrsna.nanoai.shared.testing

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

private val DEFAULT_TIMEOUT: Duration = 5.seconds

/**
 * Launches [Flow.test] within a [TestScope], ensuring the turbine completes and the virtual clock
 * advances until idle before returning. Useful for verifying emissions in structured concurrency
 * tests that combine multiple async sources.
 */
suspend fun <T> TestScope.testFlow(
  flow: Flow<T>,
  timeout: Duration = DEFAULT_TIMEOUT,
  assertions: suspend TurbineTestContext<T>.() -> Unit,
) {
  val job = launch { flow.test(timeout) { assertions() } }
  advanceUntilIdle()
  job.join()
}

/**
 * Convenience wrapper for calling [Flow.test] with a shared timeout when a [TestScope] is already
 * in scope (e.g. inside `runTest`).
 */
suspend fun <T> Flow<T>.testFlow(
  timeout: Duration = DEFAULT_TIMEOUT,
  assertions: suspend TurbineTestContext<T>.() -> Unit,
) {
  test(timeout) { assertions() }
}
