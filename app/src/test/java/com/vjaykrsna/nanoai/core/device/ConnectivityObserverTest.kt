package com.vjaykrsna.nanoai.core.device

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityBannerState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.repository.ConnectivityRepository
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityObserverTest {
  private val dispatcher = StandardTestDispatcher()
  private val repository = FakeConnectivityRepository(dispatcher)
  private val observer = SharedFlowConnectivityObserver(repository, dispatcher)

  @Test
  fun emitsStatusAndUpdatesRepository() =
    runTest(dispatcher) {
      observer.status.test {
        assertThat(awaitItem()).isEqualTo(ConnectivityStatus.ONLINE)

        observer.onStatusChanged(ConnectivityStatus.OFFLINE)

        assertThat(awaitItem()).isEqualTo(ConnectivityStatus.OFFLINE)
        assertThat(repository.updateCalls.get()).isEqualTo(1)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun deduplicatesIdenticalStatus() =
    runTest(dispatcher) {
      observer.status.test {
        awaitItem() // initial ONLINE

        observer.onStatusChanged(ConnectivityStatus.OFFLINE)
        assertThat(awaitItem()).isEqualTo(ConnectivityStatus.OFFLINE)

        observer.onStatusChanged(ConnectivityStatus.OFFLINE)
        expectNoEvents()
        assertThat(repository.updateCalls.get()).isEqualTo(1)
        cancelAndIgnoreRemainingEvents()
      }
    }

  private class FakeConnectivityRepository(
    override val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
  ) : ConnectivityRepository {
    val updateCalls = AtomicInteger(0)
    private val bannerState =
      MutableStateFlow(ConnectivityBannerState(status = ConnectivityStatus.ONLINE))

    override val connectivityBannerState: Flow<ConnectivityBannerState> = bannerState

    override suspend fun updateConnectivity(status: ConnectivityStatus) {
      updateCalls.incrementAndGet()
      bannerState.value = bannerState.value.copy(status = status)
    }
  }
}
