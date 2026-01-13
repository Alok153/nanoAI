package com.vjaykrsna.nanoai.feature.settings.data

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.library.ExportBackupUseCase
import com.vjaykrsna.nanoai.core.domain.library.ExportService
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.settings.BackupLocation
import com.vjaykrsna.nanoai.core.domain.settings.ImportService
import com.vjaykrsna.nanoai.core.domain.settings.ImportSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoreBackupDataSourceTest {
  private val fakeExportService = FakeExportService()
  private val exportUseCase = ExportBackupUseCase(fakeExportService)
  private val fakeImport = FakeImportService()
  private val dataSource = CoreBackupDataSource(exportUseCase, fakeImport)

  @Test
  fun exportBackup_delegatesToExportUseCase() = runTest {
    val result = dataSource.exportBackup("/tmp/out.zip", includeChatHistory = true)

    assertThat(fakeExportService.requestedPaths).containsExactly("/tmp/out.zip")
    assertThat(fakeExportService.requestedWithHistory).containsExactly(true)
    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
  }

  @Test
  fun importBackup_mapsSuccess() = runTest {
    val result = dataSource.importBackup("content://backup.zip")

    assertThat(fakeImport.requestedLocations)
      .containsExactly(BackupLocation("content://backup.zip"))
    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
  }

  @Test
  fun validateBackup_propagatesRecoverable() = runTest {
    fakeImport.nextValidationResult =
      NanoAIResult.recoverable(message = "bad file", telemetryId = "x")

    val result = dataSource.validateBackup("content://bad.zip")

    assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
  }

  @Test
  fun validateBackup_doesNotInvokeImportPath() = runTest {
    val result = dataSource.validateBackup("content://valid.zip")

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    assertThat(fakeImport.validateCalls).isEqualTo(1)
    assertThat(fakeImport.importCalls).isEqualTo(0)
  }

  private class FakeExportService : ExportService {
    val requestedPaths = mutableListOf<String>()
    val requestedWithHistory = mutableListOf<Boolean>()

    override suspend fun gatherPersonas() =
      emptyList<com.vjaykrsna.nanoai.core.domain.model.PersonaProfile>()

    override suspend fun gatherAPIProviderConfigs() =
      emptyList<com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig>()

    override suspend fun gatherChatHistory(): List<ChatThread> =
      listOf(
        ChatThread(
          threadId = java.util.UUID.randomUUID(),
          title = "sample",
          personaId = null,
          activeModelId = "m1",
          createdAt = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
          updatedAt = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
        )
      )

    override suspend fun createExportBundle(
      personas: List<com.vjaykrsna.nanoai.core.domain.model.PersonaProfile>,
      apiProviders: List<com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig>,
      destinationPath: String,
      chatHistory: List<ChatThread>,
    ): String {
      requestedPaths += destinationPath
      requestedWithHistory += chatHistory.isNotEmpty()
      return destinationPath
    }

    override suspend fun notifyUnencryptedExport(destinationPath: String) = Unit
  }

  private class FakeImportService : ImportService {
    val requestedLocations = mutableListOf<BackupLocation>()
    var nextResult: NanoAIResult<ImportSummary> = NanoAIResult.success(ImportSummary(0, 0, 0, 0))
    var nextValidationResult: NanoAIResult<ImportSummary> =
      NanoAIResult.success(ImportSummary(0, 0, 0, 0))
    var importCalls: Int = 0
    var validateCalls: Int = 0

    override suspend fun importBackup(location: BackupLocation): NanoAIResult<ImportSummary> {
      requestedLocations += location
      importCalls += 1
      return nextResult
    }

    override suspend fun validateBackup(location: BackupLocation): NanoAIResult<ImportSummary> {
      requestedLocations += location
      validateCalls += 1
      return nextValidationResult
    }
  }
}
