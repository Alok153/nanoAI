package com.vjaykrsna.nanoai.feature.library.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.library.DownloadModelUseCase
import com.vjaykrsna.nanoai.core.domain.library.ManageModelUseCase
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
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
import kotlinx.datetime.Clock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadUiCoordinatorTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var downloadModelUseCase: DownloadModelUseCase
  private lateinit var manageModelUseCase: ManageModelUseCase
  private lateinit var coordinator: DownloadUiCoordinator

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    downloadModelUseCase = mockk(relaxed = true)
    manageModelUseCase = mockk(relaxed = true)
    every { downloadModelUseCase.getDownloadProgress(any()) } returns MutableStateFlow(0f)
    every { downloadModelUseCase.observeDownloadTasks() } returns flowOf(emptyList())
    coEvery { downloadModelUseCase.observeDownloadTask(any()) } returns flowOf(null)

    coordinator = DownloadUiCoordinator(downloadModelUseCase, manageModelUseCase)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `downloadModel calls use case with model id`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadModelUseCase.downloadModel("model-123") } returns NanoAIResult.success(taskId)

    coordinator.downloadModel("model-123")
    advanceUntilIdle()

    coVerify { downloadModelUseCase.downloadModel("model-123") }
  }

  @Test
  fun `downloadModel emits error on failure`() = runTest {
    coEvery { downloadModelUseCase.downloadModel("model-123") } returns
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

    coVerify { downloadModelUseCase.pauseDownload(taskId) }
  }

  @Test
  fun `pauseDownload emits error on failure`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadModelUseCase.pauseDownload(taskId) } throws RuntimeException("Pause failed")

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

    coVerify { downloadModelUseCase.resumeDownload(taskId) }
  }

  @Test
  fun `resumeDownload emits error on failure`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadModelUseCase.resumeDownload(taskId) } throws RuntimeException("Resume failed")

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

    coVerify { downloadModelUseCase.cancelDownload(taskId) }
  }

  @Test
  fun `cancelDownload emits error on failure`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadModelUseCase.cancelDownload(taskId) } throws RuntimeException("Cancel failed")

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

    coVerify { downloadModelUseCase.retryFailedDownload(taskId) }
  }

  @Test
  fun `retryDownload emits error on failure`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadModelUseCase.retryFailedDownload(taskId) } throws
      RuntimeException("Retry failed")

    coordinator.retryDownload(taskId)

    coordinator.errorEvents.test {
      advanceUntilIdle()
      val error = awaitItem()
      assertThat(error).isInstanceOf(LibraryError.RetryFailed::class.java)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `deleteModel calls manage model use case`() = runTest {
    coEvery { manageModelUseCase.deleteModel("model-123") } returns NanoAIResult.success(Unit)

    coordinator.deleteModel("model-123")
    advanceUntilIdle()

    coVerify { manageModelUseCase.deleteModel("model-123") }
  }

  @Test
  fun `deleteModel emits error on failure`() = runTest {
    coEvery { manageModelUseCase.deleteModel("model-123") } returns
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
    every { downloadModelUseCase.getDownloadProgress(taskId) } returns progressFlow

    val stateFlow = coordinator.observeDownloadProgress(taskId)
    advanceUntilIdle()

    assertThat(stateFlow.value).isEqualTo(0.5f)
  }

  @Test
  fun `observeDownloadTasks returns state flow from use case`() = runTest {
    val taskId = UUID.randomUUID()
    val now = Clock.System.now()
    val task =
      DownloadTask(
        taskId = taskId,
        modelId = "model-123",
        status = DownloadStatus.DOWNLOADING,
        progress = 0.5f,
        errorMessage = null,
        startedAt = now,
      )
    every { downloadModelUseCase.observeDownloadTasks() } returns flowOf(listOf(task))

    val stateFlow = coordinator.observeDownloadTasks()
    advanceUntilIdle()

    assertThat(stateFlow.value).hasSize(1)
    assertThat(stateFlow.value.first().taskId).isEqualTo(taskId)
  }
}
