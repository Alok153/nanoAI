package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.testing.assertIsSuccess
import com.vjaykrsna.nanoai.testing.assertRecoverableError
import com.vjaykrsna.nanoai.testing.assertRecoverableErrorWithMessage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ManageModelUseCaseTest {

  private lateinit var repository: ModelCatalogRepository
  private lateinit var useCase: ManageModelUseCase

  @Before
  fun setUp() {
    repository = mockk(relaxed = true)
    useCase = ManageModelUseCase(repository)
  }

  @Test
  fun `deleteModel returns recoverable when model is active`() = runTest {
    val modelId = "model-id"
    coEvery { repository.isModelActiveInSession(modelId) } returns true

    val result = useCase.deleteModel(modelId)

    result.assertRecoverableErrorWithMessage("Model $modelId is active in a conversation")
    coVerify(exactly = 0) { repository.deleteModelFiles(modelId) }
    coVerify(exactly = 0) { repository.updateInstallState(any(), any()) }
    coVerify(exactly = 0) { repository.updateDownloadTaskId(any(), any()) }
  }

  @Test
  fun `deleteModel returns success when repository succeeds`() = runTest {
    val modelId = "model-id"
    coEvery { repository.isModelActiveInSession(modelId) } returns false
    coEvery { repository.deleteModelFiles(modelId) } just runs
    coEvery { repository.updateInstallState(modelId, InstallState.NOT_INSTALLED) } just runs
    coEvery { repository.updateDownloadTaskId(modelId, null) } just runs

    val result = useCase.deleteModel(modelId)

    result.assertIsSuccess()
    coVerify(exactly = 1) { repository.deleteModelFiles(modelId) }
    coVerify(exactly = 1) { repository.updateInstallState(modelId, InstallState.NOT_INSTALLED) }
    coVerify(exactly = 1) { repository.updateDownloadTaskId(modelId, null) }
  }

  @Test
  fun `deleteModel returns recoverable when repository throws illegal state`() = runTest {
    val modelId = "model-id"
    coEvery { repository.isModelActiveInSession(modelId) } returns false
    coEvery { repository.deleteModelFiles(modelId) } throws IllegalStateException("database down")

    val result = useCase.deleteModel(modelId)

    result.assertRecoverableError()
  }

  @Test
  fun `deleteModel rethrows cancellation exception`() = runTest {
    val modelId = "model-id"
    coEvery { repository.isModelActiveInSession(modelId) } throws CancellationException("cancel")

    assertFailsWith<CancellationException> { useCase.deleteModel(modelId) }
  }
}
