package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ProgressJob
import com.vjaykrsna.nanoai.core.domain.uiux.NavigationOperationsUseCase
import com.vjaykrsna.nanoai.shared.ui.shell.ShellUiEvent
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.mockk
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModelJobCompletionTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun completeJob_removesJobAndClearsUndo() =
    runTest(dispatcher) {
      val jobId = UUID.randomUUID()
      val fakeRepos = createFakeRepositories()
      fakeRepos.progressRepository.queueJob(
        ProgressJob(
          jobId = jobId,
          type = JobType.MODEL_DOWNLOAD,
          status = JobStatus.RUNNING,
          progress = 0.5f,
          eta = Duration.ofSeconds(30),
          canRetry = false,
          queuedAt = Instant.parse("2025-10-06T00:00:00Z"),
        )
      )

      val navigationOperationsUseCase =
        NavigationOperationsUseCase(fakeRepos.navigationRepository, dispatcher)
      val progressViewModel = createProgressViewModel(fakeRepos, dispatcher)

      val navigationViewModel = mockk<NavigationViewModel>(relaxed = true)
      val connectivityViewModel = createConnectivityViewModel(fakeRepos, dispatcher)
      val themeViewModel = createThemeViewModel(fakeRepos, dispatcher)

      val viewModel =
        ShellViewModel(
          navigationOperationsUseCase,
          navigationViewModel,
          connectivityViewModel,
          progressViewModel,
          themeViewModel,
          dispatcher,
        )

      viewModel.onEvent(ShellUiEvent.CompleteJob(jobId))
      advanceUntilIdle()

      val progressJobs = fakeRepos.progressRepository.progressJobs.first { jobs -> jobs.isEmpty() }
      assertThat(progressJobs).isEmpty()
    }
}
