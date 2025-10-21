package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.uiux.domain.ConnectivityOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.JobOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.NavigationOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.QueueJobUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.SettingsOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.UndoActionUseCase
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.state.JobStatus
import com.vjaykrsna.nanoai.feature.uiux.state.JobType
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.every
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

private fun jobLabel(job: ProgressJob): String =
  when (job.type) {
    JobType.IMAGE_GENERATION -> "Image generation"
    JobType.AUDIO_RECORDING -> "Audio recording"
    JobType.MODEL_DOWNLOAD -> "Model download"
    JobType.TEXT_GENERATION -> "Text generation"
    JobType.TRANSLATION -> "Translation"
    JobType.OTHER -> "Background task"
  }

@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModelJobManagementTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun queueGeneration_offline_jobQueuedWithPendingUndo() =
    runTest(dispatcher) {
      val repository = FakeShellStateRepository(initialConnectivity = ConnectivityStatus.OFFLINE)
      val actionProvider = createFakeCommandPaletteActionProvider()
      val progressCoordinator = createFakeProgressCenterCoordinator()
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)
      val connectivityOperationsUseCase = mockk<ConnectivityOperationsUseCase>(relaxed = true)
      val queueJobUseCase = mockk<QueueJobUseCase>(relaxed = true)
      val jobOperationsUseCase = mockk<JobOperationsUseCase>(relaxed = true)
      val undoActionUseCase = mockk<UndoActionUseCase>(relaxed = true)
      val settingsOperationsUseCase = mockk<SettingsOperationsUseCase>(relaxed = true)

      // Set up queue job use case to actually call repository
      every { queueJobUseCase.execute(any()) } answers
        {
          val job = firstArg<ProgressJob>()
          runBlocking {
            repository.queueJob(job)
            val message =
              when {
                job.status == JobStatus.FAILED && job.canRetry -> "${jobLabel(job)} retry scheduled"
                repository.layoutSnapshot.connectivity != ConnectivityStatus.ONLINE ->
                  "${jobLabel(job)} queued for reconnect"
                job.status == JobStatus.PENDING -> "${jobLabel(job)} queued"
                else -> "${jobLabel(job)} updated"
              }
            repository.recordUndoPayload(
              com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload(
                actionId = "queue-${job.jobId}",
                metadata = mapOf("message" to message, "jobId" to job.jobId.toString()),
              )
            )
          }
        }

      val viewModel =
        ShellViewModel(
          repository,
          actionProvider,
          progressCoordinator,
          navigationOperationsUseCase,
          connectivityOperationsUseCase,
          queueJobUseCase,
          jobOperationsUseCase,
          undoActionUseCase,
          settingsOperationsUseCase,
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

      viewModel.queueGeneration(job)
      advanceUntilIdle()

      val layout = repository.layoutSnapshot
      assertThat(layout.progressJobs.map { it.jobId }).contains(jobId)
      val undoPayload = layout.pendingUndoAction
      assertThat(undoPayload).isNotNull()
      val message = undoPayload?.metadata?.get("message") as? String
      assertThat(message).isEqualTo("Image generation queued for reconnect")
      assertThat(repository.queuedJobs.map { it.jobId }).contains(jobId)
    }

  @Test
  fun queueGeneration_retryableFailure_setsRetryMessage() =
    runTest(dispatcher) {
      val repository = FakeShellStateRepository()
      val actionProvider = createFakeCommandPaletteActionProvider()
      val progressCoordinator = createFakeProgressCenterCoordinator()
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)
      val connectivityOperationsUseCase = mockk<ConnectivityOperationsUseCase>(relaxed = true)
      val queueJobUseCase = mockk<QueueJobUseCase>(relaxed = true)
      val jobOperationsUseCase = mockk<JobOperationsUseCase>(relaxed = true)
      val undoActionUseCase = mockk<UndoActionUseCase>(relaxed = true)
      val settingsOperationsUseCase = mockk<SettingsOperationsUseCase>(relaxed = true)

      // Set up queue job use case to actually call repository
      every { queueJobUseCase.execute(any()) } answers
        {
          val job = firstArg<ProgressJob>()
          runBlocking {
            repository.queueJob(job)
            val message =
              when {
                job.status == JobStatus.FAILED && job.canRetry -> "${jobLabel(job)} retry scheduled"
                repository.layoutSnapshot.connectivity != ConnectivityStatus.ONLINE ->
                  "${jobLabel(job)} queued for reconnect"
                job.status == JobStatus.PENDING -> "${jobLabel(job)} queued"
                else -> "${jobLabel(job)} updated"
              }
            repository.recordUndoPayload(
              com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload(
                actionId = "queue-${job.jobId}",
                metadata = mapOf("message" to message, "jobId" to job.jobId.toString()),
              )
            )
          }
        }

      val viewModel =
        ShellViewModel(
          repository,
          actionProvider,
          progressCoordinator,
          navigationOperationsUseCase,
          connectivityOperationsUseCase,
          queueJobUseCase,
          jobOperationsUseCase,
          undoActionUseCase,
          settingsOperationsUseCase,
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

      viewModel.queueGeneration(job)
      advanceUntilIdle()

      val layout = repository.layoutSnapshot
      val message = layout.pendingUndoAction?.metadata?.get("message") as? String
      assertThat(message).isEqualTo("Model download retry scheduled")
      assertThat(layout.progressJobs.map { it.jobId }).contains(jobId)
    }

  @Test
  fun undoAction_clearsPendingJobAndUndoPayload() =
    runTest(dispatcher) {
      val repository = FakeShellStateRepository(initialConnectivity = ConnectivityStatus.OFFLINE)
      val actionProvider = createFakeCommandPaletteActionProvider()
      val progressCoordinator = createFakeProgressCenterCoordinator()
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)
      val connectivityOperationsUseCase = mockk<ConnectivityOperationsUseCase>(relaxed = true)
      val queueJobUseCase = mockk<QueueJobUseCase>()
      every { queueJobUseCase.execute(any()) } answers
        {
          val job = firstArg<ProgressJob>()
          runBlocking { repository.queueJob(job) }
        }
      val jobOperationsUseCase = mockk<JobOperationsUseCase>(relaxed = true)
      val undoActionUseCase = mockk<UndoActionUseCase>()
      every { undoActionUseCase.execute(any()) } answers
        {
          val payload = firstArg<com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload>()
          if (payload.actionId.startsWith("queue-")) {
            val jobIdString = payload.actionId.removePrefix("queue-")
            val jobId = UUID.fromString(jobIdString)
            runBlocking { repository.completeJob(jobId) }
          }
        }
      val settingsOperationsUseCase = mockk<SettingsOperationsUseCase>(relaxed = true)
      val viewModel =
        ShellViewModel(
          repository,
          actionProvider,
          progressCoordinator,
          navigationOperationsUseCase,
          connectivityOperationsUseCase,
          queueJobUseCase,
          jobOperationsUseCase,
          undoActionUseCase,
          settingsOperationsUseCase,
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

      viewModel.queueGeneration(job)
      advanceUntilIdle()

      val queuedLayout = repository.layoutSnapshot
      val payload = requireNotNull(queuedLayout.pendingUndoAction)

      viewModel.undoAction(payload)
      advanceUntilIdle()

      val clearedLayout = repository.layoutSnapshot
      assertThat(clearedLayout.pendingUndoAction).isNull()
      assertThat(clearedLayout.progressJobs).isEmpty()
      assertThat(repository.completedJobs).contains(jobId)
    }

  @Test
  fun completeJob_removesJobAndClearsUndo() =
    runTest(dispatcher) {
      val jobId = UUID.randomUUID()
      val repository =
        FakeShellStateRepository(
          initialJobs =
            listOf(
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
        )
      val actionProvider = createFakeCommandPaletteActionProvider()
      val progressCoordinator = createFakeProgressCenterCoordinator()
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)
      val connectivityOperationsUseCase = mockk<ConnectivityOperationsUseCase>(relaxed = true)
      val queueJobUseCase = mockk<QueueJobUseCase>(relaxed = true)
      val jobOperationsUseCase = mockk<JobOperationsUseCase>(relaxed = true)
      val undoActionUseCase = mockk<UndoActionUseCase>(relaxed = true)
      val settingsOperationsUseCase = mockk<SettingsOperationsUseCase>(relaxed = true)

      // Set up job operations use case to actually call repository
      every { jobOperationsUseCase.completeJob(any()) } answers
        {
          runBlocking { repository.completeJob(firstArg()) }
        }

      val viewModel =
        ShellViewModel(
          repository,
          actionProvider,
          progressCoordinator,
          navigationOperationsUseCase,
          connectivityOperationsUseCase,
          queueJobUseCase,
          jobOperationsUseCase,
          undoActionUseCase,
          settingsOperationsUseCase,
          dispatcher,
        )

      viewModel.completeJob(jobId)
      advanceUntilIdle()

      val uiState =
        viewModel.uiState.first { state ->
          repository.completedJobs.contains(jobId) &&
            state.layout.progressJobs.none { it.jobId == jobId }
        }
      assertThat(uiState.layout.progressJobs).isEmpty()
      assertThat(repository.completedJobs).contains(jobId)
    }
}
