package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.testing.assertFatalError
import com.vjaykrsna.nanoai.testing.assertRecoverableError
import com.vjaykrsna.nanoai.testing.assertSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExportBackupUseCaseTest {
  private lateinit var useCase: ExportBackupUseCase
  private lateinit var exportService: ExportService

  private val destinationPath = "/path/to/backup"
  private val bundlePath = "/path/to/bundle.zip"

  @BeforeEach
  fun setup() {
    exportService = mockk(relaxed = true)

    useCase = ExportBackupUseCase(exportService)
  }

  @Test
  fun `invoke exports without chat history successfully`() = runTest {
    coEvery { exportService.createExportBundle(any(), any(), any(), any()) } returns bundlePath

    val result = useCase.invoke(destinationPath, includeChatHistory = false)

    val returnedPath = result.assertSuccess()
    assert(returnedPath == bundlePath)
    coVerify { exportService.gatherPersonas() }
    coVerify { exportService.gatherAPIProviderConfigs() }
    coVerify(exactly = 0) { exportService.gatherChatHistory() }
    coVerify { exportService.createExportBundle(any(), any(), destinationPath, emptyList()) }
    coVerify { exportService.notifyUnencryptedExport(bundlePath) }
  }

  @Test
  fun `invoke exports with chat history successfully`() = runTest {
    coEvery { exportService.createExportBundle(any(), any(), any(), any()) } returns bundlePath

    val result = useCase.invoke(destinationPath, includeChatHistory = true)

    result.assertSuccess()
    coVerify { exportService.gatherChatHistory() }
  }

  @Test
  fun `invoke returns recoverable error when export fails with io exception`() = runTest {
    val exception = IOException("Export failed")
    coEvery { exportService.gatherPersonas() } throws exception

    val result = useCase.invoke(destinationPath, includeChatHistory = false)

    result.assertRecoverableError()
  }

  @Test
  fun `invoke returns fatal error when security exception occurs`() = runTest {
    val exception = SecurityException("Keystore locked")
    coEvery { exportService.gatherPersonas() } throws exception

    val result = useCase.invoke(destinationPath, includeChatHistory = true)

    result.assertFatalError()
  }
}
