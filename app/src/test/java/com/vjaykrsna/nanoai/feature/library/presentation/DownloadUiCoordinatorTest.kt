package com.vjaykrsna.nanoai.feature.library.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import com.vjaykrsna.nanoai.feature.library.domain.CancelModelDownloadUseCase
import com.vjaykrsna.nanoai.feature.library.domain.DeleteModelUseCase
import com.vjaykrsna.nanoai.feature.library.domain.ObserveDownloadProgressUseCase
import com.vjaykrsna.nanoai.feature.library.domain.ObserveDownloadTaskUseCase
import com.vjaykrsna.nanoai.feature.library.domain.ObserveDownloadTasksUseCase
import com.vjaykrsna.nanoai.feature.library.domain.PauseModelDownloadUseCase
import com.vjaykrsna.nanoai.feature.library.domain.QueueModelDownloadUseCase
import com.vjaykrsna.nanoai.feature.library.domain.ResumeModelDownloadUseCase
import com.vjaykrsna.nanoai.feature.library.domain.RetryModelDownloadUseCase
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadUiCoordinatorTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var queueModelDownloadUseCase: QueueModelDownloadUseCase
  private lateinit var pauseModelDownloadUseCase: PauseModelDownloadUseCase
  private lateinit var resumeModelDownloadUseCase: ResumeModelDownloadUseCase
  private lateinit var cancelModelDownloadUseCase: CancelModelDownloadUseCase
  private lateinit var retryModelDownloadUseCase: RetryModelDownloadUseCase
  private lateinit var deleteModelUseCase: DeleteModelUseCase
  private lateinit var observeDownloadTaskUseCase: ObserveDownloadTaskUseCase
  private lateinit var observeDownloadTasksUseCase: ObserveDownloadTasksUseCase
  private lateinit var observeDownloadProgressUseCase: ObserveDownloadProgressUseCase
  private lateinit var coordinator: DownloadUiCoordinator

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    queueModelDownloadUseCase = mockk(relaxed = true)
    pauseModelDownloadUseCase = mockk(relaxed = true)
    resumeModelDownloadUseCase = mockk(relaxed = true)
    cancelModelDownloadUseCase = mockk(relaxed = true)
    retryModelDownloadUseCase = mockk(relaxed = true)
    deleteModelUseCase = mockk(relaxed = true)
    observeDownloadTaskUseCase = mockk(relaxed = true)
    observeDownloadTasksUseCase = mockk(relaxed = true)
    observeDownloadProgressUseCase = mockk(relaxed = true)

    every { observeDownloadProgressUseCase.invoke(any()) } returns MutableStateFlow(0f)
    every { observeDownloadTasksUseCase.invoke() } returns flowOf(emptyList())
    coEvery { observeDownloadTaskUseCase.invoke(any()) } returns flowOf(null)

    coordinator =
      DownloadUiCoordinator(
        queueModelDownloadUseCase,
        pauseModelDownloadUseCase,
        resumeModelDownloadUseCase,
        cancelModelDownloadUseCase,
        retryModelDownloadUseCase,
        deleteModelUseCase,
        observeDownloadTaskUseCase,
        observeDownloadTasksUseCase,
        observeDownloadProgressUseCase,
      )
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `downloadModel calls use case with model id`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { queueModelDownloadUseCase("model-123") } returns NanoAIResult.success(taskId)

    coordinator.downloadModel("model-123")
    advanceUntilIdle()

    coVerify { queueModelDownloadUseCase("model-123") }
  }

  @Test
  fun `downloadModel emits error on failure`() = runTest {
    coEvery { queueModelDownloadUseCase("model-123") } returns
      NanoAIResult.recoverable(message = "Download failed")

    coordinator.downloadModel("model-123")

    coordinator.errorEvents.test {
      advanceUntilIdle()
      val error = awaitItem()
      assertThat(error).isInstanceOf(LibraryError.DownloadFailed::class.java)
      val downloadError = error as LibraryError.DownloadFailed
      assertThat(downloadError.modelId).isEqualTo("model-123")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `pauseDownload delegates to use case`() = runTest {
    val taskId = UUID.randomUUID()

    coordinator.pauseDownload(taskId)
    advanceUntilIdle()

    coVerify { pauseModelDownloadUseCase(taskId) }
  }

  @Test
  fun `pauseDownload emits error on failure`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { pauseModelDownloadUseCase(taskId) } returns
      NanoAIResult.recoverable(message = "Pause failed")

    coordinator.pauseDownload(taskId)

    coordinator.errorEvents.test {
      advanceUntilIdle()
      val error = awaitItem()
      assertThat(error).isInstanceOf(LibraryError.PauseFailed::class.java)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `resumeDownload delegates to use case`() = runTest {
    val taskId = UUID.randomUUID()

    coordinator.resumeDownload(taskId)
    advanceUntilIdle()

    coVerify { resumeModelDownloadUseCase(taskId) }
  }

  @Test
  fun `resumeDownload emits error on failure`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { resumeModelDownloadUseCase(taskId) } returns
      NanoAIResult.recoverable(message = "Resume failed")

    coordinator.resumeDownload(taskId)

    coordinator.errorEvents.test {
      advanceUntilIdle()
      val error = awaitItem()
      assertThat(error).isInstanceOf(LibraryError.ResumeFailed::class.java)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `cancelDownload delegates to use case`() = runTest {
    val taskId = UUID.randomUUID()

    coordinator.cancelDownload(taskId)
    advanceUntilIdle()

    coVerify { cancelModelDownloadUseCase(taskId) }
  }

  @Test
  fun `cancelDownload emits error on failure`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { cancelModelDownloadUseCase(taskId) } returns
      NanoAIResult.recoverable(message = "Cancel failed")

    coordinator.cancelDownload(taskId)

    coordinator.errorEvents.test {
      advanceUntilIdle()
      val error = awaitItem()
      assertThat(error).isInstanceOf(LibraryError.CancelFailed::class.java)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `retryDownload delegates to use case`() = runTest {
    val taskId = UUID.randomUUID()

    coordinator.retryDownload(taskId)
    advanceUntilIdle()

    coVerify { retryModelDownloadUseCase(taskId) }
  }

  @Test
  fun `retryDownload emits error on failure`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { retryModelDownloadUseCase(taskId) } returns
      NanoAIResult.recoverable(message = "Retry failed")

    coordinator.retryDownload(taskId)

    coordinator.errorEvents.test {
      advanceUntilIdle()
      val error = awaitItem()
      assertThat(error).isInstanceOf(LibraryError.RetryFailed::class.java)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `deleteModel calls delete use case`() = runTest {
    coEvery { deleteModelUseCase("model-123") } returns NanoAIResult.success(Unit)

    coordinator.deleteModel("model-123")
    advanceUntilIdle()

    coVerify { deleteModelUseCase("model-123") }
  }

  @Test
  fun `deleteModel emits error on failure`() = runTest {
    coEvery { deleteModelUseCase("model-123") } returns
      NanoAIResult.recoverable(message = "Delete failed")

    coordinator.deleteModel("model-123")

    coordinator.errorEvents.test {
      advanceUntilIdle()
      val error = awaitItem()
      assertThat(error).isInstanceOf(LibraryError.DeleteFailed::class.java)
      val deleteError = error as LibraryError.DeleteFailed
      assertThat(deleteError.modelId).isEqualTo("model-123")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `observeDownloadProgress returns state flow from use case`() = runTest {
    val taskId = UUID.randomUUID()
    val progressFlow = MutableStateFlow(0.5f)
    every { observeDownloadProgressUseCase.invoke(taskId) } returns progressFlow

    val stateFlow = coordinator.observeDownloadProgress(taskId)
    advanceUntilIdle()

    assertThat(stateFlow.value).isEqualTo(0.5f)
  }

  @Test
  fun `observeDownloadTasks returns state flow from use case`() = runTest {
    val taskId = UUID.randomUUID()
    val task =
      DownloadTask(
        taskId = taskId,
        modelId = "model-123",
        status = DownloadStatus.DOWNLOADING,
        progress = 0.5f,
        errorMessage = null,
        startedAt = null,
        finishedAt = null,
        bytesDownloaded = 0L,
        totalBytes = 100L,
      )
    every { observeDownloadTasksUseCase.invoke() } returns flowOf(listOf(task))

    val stateFlow = coordinator.observeDownloadTasks()
    advanceUntilIdle()

    assertThat(stateFlow.value).hasSize(1)
    assertThat(stateFlow.value.first().taskId).isEqualTo(taskId)
  }
}
