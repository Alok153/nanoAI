package com.vjaykrsna.nanoai.shared.testing

import kotlin.test.Test
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class FlowTestExtTest {
  @Test
  fun `testFlow extension collects emissions`() = runTest {
    flowOf(1, 2, 3).testFlow {
      awaitItem()
      awaitItem()
      awaitItem()
      awaitComplete()
    }
  }

  @Test
  fun `TestScope testFlow drains shared flows and cancels`() = runTest {
    val shared = MutableSharedFlow<Int>(replay = 3)

    val emitter = launch {
      shared.emit(42)
      shared.emit(43)
      shared.emit(44)
    }

    testFlow(shared) {
      awaitItem()
      awaitItem()
      awaitItem()
      cancelAndIgnoreRemainingEvents()
    }

    emitter.join()
  }
}
