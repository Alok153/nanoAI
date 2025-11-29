package com.vjaykrsna.nanoai.core.domain.library

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import com.vjaykrsna.nanoai.testing.assertIsSuccess
import com.vjaykrsna.nanoai.testing.assertRecoverableError
import com.vjaykrsna.nanoai.testing.assertSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class ModelDownloadsAndExportUseCaseExportTest : ModelDownloadsAndExportUseCaseTestBase() {

  @Test
  fun `exportBackup generates bundle`() = runTest {
    val exportPath = "/tmp/nanoai-backup.zip"
    val personas = listOf(DomainTestBuilders.buildPersona())
    val providers = listOf(DomainTestBuilders.buildProviderConfig())

    coEvery { exportService.gatherPersonas() } returns personas
    coEvery { exportService.gatherAPIProviderConfigs() } returns providers
    coEvery {
      exportService.createExportBundle(personas, providers, exportPath, emptyList())
    } returns exportPath

    val result = useCase.exportBackup(exportPath)

    val exportedPath = result.assertSuccess()
    assertThat(exportedPath).isEqualTo(exportPath)
    coVerify { exportService.createExportBundle(personas, providers, exportPath, emptyList()) }
    coVerify { exportService.notifyUnencryptedExport(exportPath) }
  }

  @Test
  fun `exportBackup skips chat history by default`() = runTest {
    val exportPath = "/tmp/backup.zip"

    useCase.exportBackup(exportPath)

    coVerify(exactly = 0) { exportService.gatherChatHistory() }
  }

  @Test
  fun `exportBackup includes chat history on request`() = runTest {
    val exportPath = "/tmp/backup-with-history.zip"
    val chats = listOf(DomainTestBuilders.buildChatThread())

    coEvery { exportService.gatherChatHistory() } returns chats
    coEvery { exportService.gatherPersonas() } returns emptyList()
    coEvery { exportService.gatherAPIProviderConfigs() } returns emptyList()
    coEvery {
      exportService.createExportBundle(emptyList(), emptyList(), exportPath, chats)
    } returns exportPath

    val result = useCase.exportBackup(exportPath, includeChatHistory = true)

    result.assertIsSuccess()
    coVerify { exportService.gatherChatHistory() }
    coVerify { exportService.createExportBundle(emptyList(), emptyList(), exportPath, chats) }
  }

  @Test
  fun `exportBackup returns recoverable when export fails`() = runTest {
    val exportPath = "/tmp/fail.zip"
    coEvery { exportService.gatherPersonas() } returns emptyList()
    coEvery { exportService.gatherAPIProviderConfigs() } returns emptyList()
    coEvery {
      exportService.createExportBundle(emptyList(), emptyList(), exportPath, emptyList())
    } throws IOException("disk full")

    val result = useCase.exportBackup(exportPath)

    result.assertRecoverableError()
  }
}
