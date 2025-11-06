package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModelJobQueueTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun queueGeneration_offline_setsReconnectMessage() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
      runBlocking {
        fakeRepos.connectivityRepository.updateConnectivity(ConnectivityStatus.OFFLINE)
      }
      val navigationOperationsUseCase =
        NavigationOperationsUseCase(fakeRepos.navigationRepository, dispatcher)
      val progressViewModel = createProgressViewModel(fakeRepos, dispatcher)

      // Mock sub-ViewModels unrelated to progress operations
      val navigationViewModel = mockk<NavigationViewModel>(relaxed = true)
      val connectivityViewModel = mockk<ConnectivityViewModel>(relaxed = true)
      val themeViewModel = mockk<ThemeViewModel>(relaxed = true)

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
          eta = Duration.ofSeconds(90),
          canRetry = true,
          queuedAt = Instant.parse("2025-10-06T00:00:00Z"),
        )

      viewModel.onEvent(ShellUiEvent.QueueJob(job))
      advanceUntilIdle()
      val progressJobs = fakeRepos.progressRepository.progressJobs.first { it.isNotEmpty() }
      assertThat(progressJobs.map { it.jobId }).contains(jobId)
      val undoPayload =
        requireNotNull(fakeRepos.navigationRepository.undoPayload.first { it != null })
      val message = undoPayload.metadata["message"] as? String
      assertThat(message).isEqualTo("Image generation queued for reconnect")
    }

  @Test
  fun queueGeneration_retryableFailure_setsRetryMessage() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
      val navigationOperationsUseCase =
        NavigationOperationsUseCase(fakeRepos.navigationRepository, dispatcher)
      val progressViewModel = createProgressViewModel(fakeRepos, dispatcher)

      // Mock sub-ViewModels unrelated to progress operations
      val navigationViewModel = mockk<NavigationViewModel>(relaxed = true)
      val connectivityViewModel = mockk<ConnectivityViewModel>(relaxed = true)
      val themeViewModel = mockk<ThemeViewModel>(relaxed = true)

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
          type = JobType.MODEL_DOWNLOAD,
          status = JobStatus.FAILED,
          progress = 0f,
          eta = Duration.ofSeconds(60),
          canRetry = true,
          queuedAt = Instant.parse("2025-10-06T01:00:00Z"),
        )

      viewModel.onEvent(ShellUiEvent.QueueJob(job))
      advanceUntilIdle()
      val progressJobs = fakeRepos.progressRepository.progressJobs.first { it.isNotEmpty() }
      val undoPayload =
        requireNotNull(fakeRepos.navigationRepository.undoPayload.first { it != null })
      val message = undoPayload.metadata["message"] as? String
      assertThat(message).isEqualTo("Model download retry scheduled")
      assertThat(progressJobs.map { it.jobId }).contains(jobId)
    }
}
