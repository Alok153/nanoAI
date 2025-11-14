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
class ShellViewModelJobUndoTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun undoAction_clearsPendingJobAndUndoPayload() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
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

      val jobId = UUID.randomUUID()
      val job =
        ProgressJob(
          jobId = jobId,
          type = JobType.IMAGE_GENERATION,
          status = JobStatus.PENDING,
          progress = 0f,
          eta = Duration.ofSeconds(120),
          canRetry = true,
          queuedAt = Instant.parse("2025-10-06T02:00:00Z"),
        )

      viewModel.onEvent(ShellUiEvent.QueueJob(job))
      advanceUntilIdle()

      val progressJobs = fakeRepos.progressRepository.progressJobs.first { it.isNotEmpty() }
      val payload = requireNotNull(fakeRepos.navigationRepository.undoPayload.first { it != null })

      viewModel.onEvent(ShellUiEvent.Undo(payload))
      advanceUntilIdle()

      val clearedProgressJobs = fakeRepos.progressRepository.progressJobs.first { it.isEmpty() }
      val clearedUndoPayload = fakeRepos.navigationRepository.undoPayload.first { it == null }
      assertThat(clearedProgressJobs).isEmpty()
      assertThat(clearedUndoPayload).isNull()
    }
}
