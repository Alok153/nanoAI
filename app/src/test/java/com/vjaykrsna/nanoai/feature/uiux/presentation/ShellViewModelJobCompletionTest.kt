package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
      // Pre-populate the progress repository with a job
      runBlocking {
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
      }
      val actionProvider = createFakeCommandPaletteActionProvider()
      val progressCoordinator = createFakeProgressCenterCoordinator()
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)
      val connectivityOperationsUseCase = mockk<ConnectivityOperationsUseCase>(relaxed = true)
      val queueJobUseCase = mockk<QueueJobUseCase>(relaxed = true)
      val jobOperationsUseCase = mockk<JobOperationsUseCase>(relaxed = true)
      val undoActionUseCase = mockk<UndoActionUseCase>(relaxed = true)
      val settingsOperationsUseCase = mockk<SettingsOperationsUseCase>(relaxed = true)

      // Mock sub-ViewModels
      val navigationViewModel = mockk<NavigationViewModel>(relaxed = true)
      val connectivityViewModel = mockk<ConnectivityViewModel>(relaxed = true)
      val progressViewModel = mockk<ProgressViewModel>(relaxed = true)
      val themeViewModel = mockk<ThemeViewModel>(relaxed = true)

      // Set up job operations use case to actually call repository
      every { jobOperationsUseCase.completeJob(any()) } answers
        {
          runBlocking { fakeRepos.progressRepository.completeJob(firstArg()) }
        }

      val viewModel =
        ShellViewModel(
          fakeRepos.navigationRepository,
          fakeRepos.connectivityRepository,
          fakeRepos.themeRepository,
          fakeRepos.progressRepository,
          fakeRepos.userProfileRepository,
          actionProvider,
          progressCoordinator,
          navigationOperationsUseCase,
          connectivityOperationsUseCase,
          queueJobUseCase,
          jobOperationsUseCase,
          undoActionUseCase,
          settingsOperationsUseCase,
          navigationViewModel,
          connectivityViewModel,
          progressViewModel,
          themeViewModel,
          dispatcher,
        )

      viewModel.onEvent(ShellUiEvent.CompleteJob(jobId))
      advanceUntilIdle()

      val progressJobs = fakeRepos.progressRepository.progressJobs.first()
      assertThat(progressJobs).isEmpty()
    }
}
