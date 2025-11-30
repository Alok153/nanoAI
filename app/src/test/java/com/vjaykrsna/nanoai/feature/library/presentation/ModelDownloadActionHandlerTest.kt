package com.vjaykrsna.nanoai.feature.library.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.library.DownloadModelUseCase
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModelDownloadActionHandlerTest {

  private lateinit var downloadModelUseCase: DownloadModelUseCase
  private lateinit var downloadCoordinator: DownloadUiCoordinator

  @BeforeEach
  fun setUp() {
    downloadModelUseCase = mockk(relaxed = true)
    downloadCoordinator = mockk(relaxed = true)
    every { downloadModelUseCase.getDownloadProgress(any()) } returns MutableStateFlow(0f)
  }

  @Test
  fun downloadModelDelegatesToUseCaseAndDoesNotEmitError() = runTest {
    val errors = mutableListOf<LibraryError>()
    val handler = createHandler(errors)
    val taskId = UUID.randomUUID()
    coEvery { downloadModelUseCase.downloadModel("model-123") } returns NanoAIResult.success(taskId)

    handler.downloadModel("model-123")
    advanceUntilIdle()

    coVerify { downloadModelUseCase.downloadModel("model-123") }
    assertThat(errors).isEmpty()
  }

  @Test
  fun downloadModelEmitsErrorWhenUseCaseFails() = runTest {
    val errors = mutableListOf<LibraryError>()
    val handler = createHandler(errors)
    coEvery { downloadModelUseCase.downloadModel("model-404") } returns
      NanoAIResult.recoverable(message = "offline")

    handler.downloadModel("model-404")
    advanceUntilIdle()

    assertThat(errors).hasSize(1)
    val error = errors.single() as LibraryError.DownloadFailed
    assertThat(error.modelId).isEqualTo("model-404")
    assertThat(error.message).contains("offline")
  }

  @Test
  fun pauseDownloadDelegatesToUseCase() = runTest {
    val handler = createHandler()
    val taskId = UUID.randomUUID()

    handler.pauseDownload(taskId)
    advanceUntilIdle()

    coVerify { downloadModelUseCase.pauseDownload(taskId) }
  }

  @Test
  fun resumeDownloadDelegatesToUseCase() = runTest {
    val handler = createHandler()
    val taskId = UUID.randomUUID()

    handler.resumeDownload(taskId)
    advanceUntilIdle()

    coVerify { downloadModelUseCase.resumeDownload(taskId) }
  }

  @Test
  fun cancelDownloadDelegatesToUseCase() = runTest {
    val handler = createHandler()
    val taskId = UUID.randomUUID()

    handler.cancelDownload(taskId)
    advanceUntilIdle()

    coVerify { downloadModelUseCase.cancelDownload(taskId) }
  }

  @Test
  fun retryDownloadDelegatesToUseCase() = runTest {
    val handler = createHandler()
    val taskId = UUID.randomUUID()

    handler.retryDownload(taskId)
    advanceUntilIdle()

    coVerify { downloadModelUseCase.retryFailedDownload(taskId) }
  }

  @Test
  fun deleteModelDelegatesToDownloadCoordinator() = runTest {
    val handler = createHandler()

    handler.deleteModel("model-555")

    verify { downloadCoordinator.deleteModel("model-555") }
  }

  @Test
  fun observeDownloadProgressReflectsUseCaseFlow() = runTest {
    val taskId = UUID.randomUUID()
    val progress = MutableStateFlow(0.25f)
    every { downloadModelUseCase.getDownloadProgress(taskId) } returns progress
    val handler = createHandler()

    val stateFlow = handler.observeDownloadProgress(taskId)
    assertThat(stateFlow.value).isEqualTo(0f)

    progress.value = 0.6f
    advanceUntilIdle()

    assertThat(stateFlow.value).isEqualTo(0.6f)
    coroutineContext.cancelChildren()
  }

  private fun TestScope.createHandler(errors: MutableList<LibraryError> = mutableListOf()) =
    ModelDownloadActionHandler(
      downloadModelUseCase = downloadModelUseCase,
      downloadCoordinator = downloadCoordinator,
      dispatcher = StandardTestDispatcher(testScheduler),
      scope = this,
      emitError = { error -> errors.add(error) },
    )
}
