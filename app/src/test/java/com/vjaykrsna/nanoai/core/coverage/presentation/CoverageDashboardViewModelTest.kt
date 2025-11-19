package com.vjaykrsna.nanoai.core.coverage.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.coverage.domain.usecase.GetCoverageReportUseCase
import com.vjaykrsna.nanoai.core.coverage.ui.CoverageDashboardUiState
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHostTestHarness
import com.vjaykrsna.nanoai.testing.FakeCoverageDashboardRepository
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class CoverageDashboardViewModelTest {

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension()

  private lateinit var repository: FakeCoverageDashboardRepository
  private lateinit var useCase: GetCoverageReportUseCase
  private lateinit var viewModel: CoverageDashboardViewModel
  private lateinit var harness:
    ViewModelStateHostTestHarness<CoverageDashboardUiState, CoverageDashboardUiEvent>

  @BeforeEach
  fun setup() {
    repository = FakeCoverageDashboardRepository()
    useCase = GetCoverageReportUseCase(repository)
    viewModel = CoverageDashboardViewModel(useCase, mainDispatcherExtension.dispatcher)
    harness = ViewModelStateHostTestHarness(viewModel)
  }

  @Test
  fun `initial refresh populates coverage ui state`() = runTest {
    advanceUntilIdle()

    val state = harness.awaitState(predicate = { !it.isRefreshing })
    assertThat(state.buildId)
      .isEqualTo(repository.snapshotResult.let { (it as NanoAIResult.Success).value.buildId })
    assertThat(state.layers).isNotEmpty()
  }

  @Test
  fun `refresh failure emits envelope and mirrors message`() = runTest {
    repository.failWith(
      NanoAIResult.recoverable(message = "HTTP 503", context = mapOf("source" to "test"))
    )

    harness.testEvents {
      viewModel.refresh()
      val event = awaitItem() as CoverageDashboardUiEvent.ErrorRaised
      assertThat(event.envelope.userMessage).contains("Unable to refresh coverage dashboard")
      assertThat(event.envelope.context["source"]).isEqualTo("test")
    }

    assertThat(harness.currentState.lastErrorMessage)
      .contains("Unable to refresh coverage dashboard")
    assertThat(harness.currentState.errorBanner).isNotNull()
  }
}
