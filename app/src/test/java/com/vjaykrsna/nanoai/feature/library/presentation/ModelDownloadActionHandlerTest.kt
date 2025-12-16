package com.vjaykrsna.nanoai.feature.library.presentation

import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModelDownloadActionHandlerTest {

  private lateinit var downloadCoordinator: DownloadUiCoordinator

  @BeforeEach
  fun setUp() {
    downloadCoordinator = mockk(relaxed = true)
  }

  @Test
  fun downloadModelDelegatesToCoordinator() = runTest {
    val handler = createHandler()

    handler.downloadModel("model-123")
    advanceUntilIdle()

    coVerify { downloadCoordinator.downloadModel("model-123") }
  }

  @Test
  fun pauseDownloadDelegatesToCoordinator() = runTest {
    val handler = createHandler()
    val taskId = UUID.randomUUID()

    handler.pauseDownload(taskId)
    advanceUntilIdle()

    coVerify { downloadCoordinator.pauseDownload(taskId) }
  }

  @Test
  fun resumeDownloadDelegatesToCoordinator() = runTest {
    val handler = createHandler()
    val taskId = UUID.randomUUID()

    handler.resumeDownload(taskId)
    advanceUntilIdle()

    coVerify { downloadCoordinator.resumeDownload(taskId) }
  }

  @Test
  fun cancelDownloadDelegatesToCoordinator() = runTest {
    val handler = createHandler()
    val taskId = UUID.randomUUID()

    handler.cancelDownload(taskId)
    advanceUntilIdle()

    coVerify { downloadCoordinator.cancelDownload(taskId) }
  }

  @Test
  fun retryDownloadDelegatesToCoordinator() = runTest {
    val handler = createHandler()
    val taskId = UUID.randomUUID()

    handler.retryDownload(taskId)
    advanceUntilIdle()

    coVerify { downloadCoordinator.retryDownload(taskId) }
  }

  @Test
  fun deleteModelDelegatesToDownloadCoordinator() = runTest {
    val handler = createHandler()

    handler.deleteModel("model-555")

    verify { downloadCoordinator.deleteModel("model-555") }
  }

  @Test
  fun observeDownloadProgressReflectsCoordinatorFlow() = runTest {
    val taskId = UUID.randomUUID()
    val progress = MutableStateFlow(0.25f)
    every { downloadCoordinator.observeDownloadProgress(taskId) } returns progress
    val handler = createHandler()

    val stateFlow = handler.observeDownloadProgress(taskId)
    assertThat(stateFlow.value).isEqualTo(0.25f)

    progress.value = 0.6f
    advanceUntilIdle()

    assertThat(stateFlow.value).isEqualTo(0.6f)
  }

  private fun TestScope.createHandler() =
    ModelDownloadActionHandler(
      downloadCoordinator = downloadCoordinator,
      dispatcher = StandardTestDispatcher(testScheduler),
      scope = this,
    )
}
