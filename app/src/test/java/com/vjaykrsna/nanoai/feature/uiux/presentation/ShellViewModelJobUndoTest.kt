package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.google.common.truth.Truth.assertThat
import io.mockk.*
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
      val actionProvider = createFakeCommandPaletteActionProvider()
      val progressCoordinator = createFakeProgressCenterCoordinator()
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)
      val connectivityOperationsUseCase = mockk<ConnectivityOperationsUseCase>(relaxed = true)
      val queueJobUseCase = mockk<QueueJobUseCase>()
      coEvery { queueJobUseCase.execute(any()) } coAnswers
        {
          val job = firstArg<ProgressJob>()
          fakeRepos.progressRepository.queueJob(job)
          val message =
            when {
              job.status == JobStatus.FAILED && job.canRetry -> "${jobLabel(job)} retry scheduled"
              fakeRepos.connectivityRepository.connectivityBannerState.first().status !=
                ConnectivityStatus.ONLINE -> "${jobLabel(job)} queued for reconnect"
              job.status == JobStatus.PENDING -> "${jobLabel(job)} queued"
              else -> "${jobLabel(job)} updated"
            }
          fakeRepos.navigationRepository.recordUndoPayload(
            com.vjaykrsna.nanoai.feature.uiux.presentation.UndoPayload(
              actionId = "queue-${job.jobId}",
              metadata = mapOf("message" to message, "jobId" to job.jobId.toString()),
            )
          )
        }
      val jobOperationsUseCase = mockk<JobOperationsUseCase>(relaxed = true)
      val undoActionUseCase = mockk<UndoActionUseCase>()
      coEvery { undoActionUseCase.execute(any()) } coAnswers
        {
          val payload = firstArg<com.vjaykrsna.nanoai.feature.uiux.presentation.UndoPayload>()
          if (payload.actionId.startsWith("queue-")) {
            val jobIdString = payload.metadata["jobId"] as String
            val jobId = UUID.fromString(jobIdString)
            fakeRepos.progressRepository.completeJob(jobId)
          }
          fakeRepos.navigationRepository.recordUndoPayload(null)
        }
      val settingsOperationsUseCase = mockk<SettingsOperationsUseCase>(relaxed = true)

      // Mock sub-ViewModels
      val navigationViewModel = mockk<NavigationViewModel>(relaxed = true)
      val connectivityViewModel = mockk<ConnectivityViewModel>(relaxed = true)
      val progressViewModel = mockk<ProgressViewModel>(relaxed = true)
      val themeViewModel = mockk<ThemeViewModel>(relaxed = true)

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

      val progressJobs = fakeRepos.progressRepository.progressJobs.first()
      val payload =
        requireNotNull(
          (fakeRepos.navigationRepository as FakeNavigationRepository).undoPayloadFlow.value
        )

      viewModel.onEvent(ShellUiEvent.Undo(payload))
      advanceUntilIdle()

      val clearedProgressJobs = fakeRepos.progressRepository.progressJobs.first()
      val clearedUndoPayload =
        (fakeRepos.navigationRepository as FakeNavigationRepository).undoPayloadFlow.value
      assertThat(clearedUndoPayload).isNull()
      assertThat(clearedProgressJobs).isEmpty()
    }
}
