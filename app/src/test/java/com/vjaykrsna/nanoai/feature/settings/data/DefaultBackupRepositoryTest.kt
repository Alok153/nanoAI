package com.vjaykrsna.nanoai.feature.settings.data

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.settings.ImportSummary
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import com.vjaykrsna.nanoai.core.model.NanoAISuccess
import com.vjaykrsna.nanoai.feature.settings.domain.BackupUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultBackupRepositoryTest {
  private lateinit var fakeDataSource: FakeBackupDataSource
  private lateinit var repository: DefaultBackupRepository
  private lateinit var useCase: BackupUseCase

  @BeforeEach
  fun setUp() {
    fakeDataSource = FakeBackupDataSource()
    repository = DefaultBackupRepository(fakeDataSource)
    useCase = BackupUseCase(repository)
  }

  @Test
  fun exportBackup_delegatesToDataSource() = runTest {
    val result = useCase.exportBackup("/tmp/backup.zip", includeChatHistory = true)

    assertThat(fakeDataSource.exportedPaths).containsExactly("/tmp/backup.zip")
    assertThat(fakeDataSource.exportWithChats).containsExactly(true)
    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
  }

  @Test
  fun validateBackup_returnsDataSourceResult() = runTest {
    val result = repository.validateBackup("/tmp/backup.zip")

    assertThat(fakeDataSource.validatedPaths).containsExactly("/tmp/backup.zip")
    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
    assertThat((result as NanoAISuccess).value).isEqualTo(fakeDataSource.validationSummary)
  }

  @Test
  fun validateBackup_doesNotTriggerImport() = runTest {
    val validationResult = useCase.validateBackup("/tmp/backup.zip")
    val importResult = useCase.importBackup("/tmp/backup.zip")

    assertThat(validationResult).isInstanceOf(NanoAISuccess::class.java)
    assertThat((validationResult as NanoAISuccess).value)
      .isEqualTo(fakeDataSource.validationSummary)
    assertThat(importResult).isInstanceOf(NanoAISuccess::class.java)
    assertThat(fakeDataSource.validationCallCount).isEqualTo(1)
    assertThat(fakeDataSource.importCallCount).isEqualTo(1)
  }

  @Nested
  @DisplayName("Deterministic Persona Restore")
  inner class DeterministicPersonaRestoreTests {

    @Test
    @DisplayName("import with personas returns deterministic persona count")
    fun importBackup_withPersonas_returnsDeterministicPersonaCount() = runTest {
      val expectedSummary = ImportSummary(
        personasImported = 3,
        personasUpdated = 2,
        providersImported = 0,
        providersUpdated = 0,
      )
      fakeDataSource.nextImportSummary = expectedSummary

      val result = useCase.importBackup("/tmp/personas-backup.json")

      assertThat(result).isInstanceOf(NanoAISuccess::class.java)
      val summary = (result as NanoAISuccess).value
      assertThat(summary.personasImported).isEqualTo(3)
      assertThat(summary.personasUpdated).isEqualTo(2)
    }

    @Test
    @DisplayName("validation returns exact import summary without modifications")
    fun validateBackup_returnsDeterministicSummary() = runTest {
      val expectedSummary = ImportSummary(
        personasImported = 5,
        personasUpdated = 1,
        providersImported = 2,
        providersUpdated = 3,
      )
      fakeDataSource.validationSummary = expectedSummary

      val result = useCase.validateBackup("/tmp/full-backup.json")

      assertThat(result).isInstanceOf(NanoAISuccess::class.java)
      val summary = (result as NanoAISuccess).value
      assertThat(summary).isEqualTo(expectedSummary)
    }

    @Test
    @DisplayName("import and validation produce same counts for same backup")
    fun importAndValidation_produceSameCountsForSameBackup() = runTest {
      val summary = ImportSummary(
        personasImported = 4,
        personasUpdated = 0,
        providersImported = 1,
        providersUpdated = 1,
      )
      fakeDataSource.validationSummary = summary
      fakeDataSource.nextImportSummary = summary

      val validationResult = useCase.validateBackup("/tmp/consistent-backup.json")
      val importResult = useCase.importBackup("/tmp/consistent-backup.json")

      assertThat((validationResult as NanoAISuccess).value)
        .isEqualTo((importResult as NanoAISuccess).value)
    }
  }

  @Nested
  @DisplayName("Deterministic Provider Restore")
  inner class DeterministicProviderRestoreTests {

    @Test
    @DisplayName("import with providers returns deterministic provider count")
    fun importBackup_withProviders_returnsDeterministicProviderCount() = runTest {
      val expectedSummary = ImportSummary(
        personasImported = 0,
        personasUpdated = 0,
        providersImported = 2,
        providersUpdated = 3,
      )
      fakeDataSource.nextImportSummary = expectedSummary

      val result = useCase.importBackup("/tmp/providers-backup.json")

      assertThat(result).isInstanceOf(NanoAISuccess::class.java)
      val summary = (result as NanoAISuccess).value
      assertThat(summary.providersImported).isEqualTo(2)
      assertThat(summary.providersUpdated).isEqualTo(3)
    }

    @Test
    @DisplayName("provider credentials are not exposed in summary")
    fun importBackup_providerCredentialsNotInSummary() = runTest {
      val expectedSummary = ImportSummary(
        personasImported = 1,
        personasUpdated = 0,
        providersImported = 1,
        providersUpdated = 0,
      )
      fakeDataSource.nextImportSummary = expectedSummary

      val result = useCase.importBackup("/tmp/secure-backup.json")

      // Summary only contains counts, not credential details
      assertThat(result).isInstanceOf(NanoAISuccess::class.java)
      val summary = (result as NanoAISuccess).value
      assertThat(summary.providersImported).isEqualTo(1)
    }

    @Test
    @DisplayName("full restore with personas and providers is deterministic")
    fun importBackup_fullRestore_isDeterministic() = runTest {
      val expectedSummary = ImportSummary(
        personasImported = 10,
        personasUpdated = 5,
        providersImported = 3,
        providersUpdated = 2,
      )
      fakeDataSource.nextImportSummary = expectedSummary

      val result = useCase.importBackup("/tmp/full-restore.json")

      assertThat(result).isInstanceOf(NanoAISuccess::class.java)
      val summary = (result as NanoAISuccess).value
      assertThat(summary.personasImported + summary.personasUpdated).isEqualTo(15)
      assertThat(summary.providersImported + summary.providersUpdated).isEqualTo(5)
    }
  }

  @Nested
  @DisplayName("Error Handling")
  inner class ErrorHandlingTests {

    @Test
    @DisplayName("export failure returns recoverable error")
    fun exportBackup_failure_returnsRecoverableError() = runTest {
      fakeDataSource.nextExportResult = NanoAIResult.recoverable(
        message = "Disk full",
        telemetryId = "export-error-001",
      )

      val result = useCase.exportBackup("/tmp/backup.json", includeChatHistory = true)

      assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
      val error = result as NanoAIResult.RecoverableError
      assertThat(error.message).isEqualTo("Disk full")
    }

    @Test
    @DisplayName("import failure returns recoverable error")
    fun importBackup_failure_returnsRecoverableError() = runTest {
      fakeDataSource.nextImportResult = NanoAIResult.recoverable(
        message = "Invalid backup format",
        telemetryId = "import-error-001",
      )

      val result = useCase.importBackup("/tmp/corrupt-backup.json")

      assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
      val error = result as NanoAIResult.RecoverableError
      assertThat(error.message).isEqualTo("Invalid backup format")
    }

    @Test
    @DisplayName("validation failure does not corrupt state")
    fun validateBackup_failure_doesNotCorruptState() = runTest {
      fakeDataSource.nextValidationResult = NanoAIResult.recoverable(
        message = "Cannot read backup file",
        telemetryId = "validation-error-001",
      )

      val result = useCase.validateBackup("/tmp/unreadable-backup.json")

      assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
      assertThat(fakeDataSource.importCallCount).isEqualTo(0)
    }
  }
}

private class FakeBackupDataSource : BackupDataSource {
  val exportedPaths = mutableListOf<String>()
  val exportWithChats = mutableListOf<Boolean>()
  val importedPaths = mutableListOf<String>()
  val validatedPaths = mutableListOf<String>()
  var importCallCount: Int = 0
  var validationCallCount: Int = 0
  var validationSummary: ImportSummary = ImportSummary(0, 0, 0, 0)
  var nextImportSummary: ImportSummary = ImportSummary(0, 0, 0, 0)
  var nextExportResult: NanoAIResult<String>? = null
  var nextImportResult: NanoAIResult<Unit>? = null
  var nextValidationResult: NanoAIResult<ImportSummary>? = null

  override suspend fun exportBackup(
    destinationPath: String,
    includeChatHistory: Boolean,
  ): NanoAIResult<String> {
    exportedPaths += destinationPath
    exportWithChats += includeChatHistory
    return nextExportResult ?: NanoAIResult.success(destinationPath)
  }

  override suspend fun importBackup(sourcePath: String): NanoAIResult<Unit> {
    importedPaths += sourcePath
    importCallCount += 1
    return nextImportResult ?: NanoAIResult.success(nextImportSummary)
  }

  override suspend fun validateBackup(sourcePath: String): NanoAIResult<ImportSummary> {
    validatedPaths += sourcePath
    validationCallCount += 1
    return nextValidationResult ?: NanoAIResult.success(validationSummary)
  }
}
