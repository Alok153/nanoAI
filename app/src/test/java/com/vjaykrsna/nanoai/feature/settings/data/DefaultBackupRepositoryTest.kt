package com.vjaykrsna.nanoai.feature.settings.data

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import com.vjaykrsna.nanoai.core.model.NanoAISuccess
import com.vjaykrsna.nanoai.feature.settings.domain.BackupUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultBackupRepositoryTest {
  private val fakeDataSource = FakeBackupDataSource()
  private val repository = DefaultBackupRepository(fakeDataSource)
  private val useCase = BackupUseCase(repository)

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
  }
}

private class FakeBackupDataSource : BackupDataSource {
  val exportedPaths = mutableListOf<String>()
  val exportWithChats = mutableListOf<Boolean>()
  val importedPaths = mutableListOf<String>()
  val validatedPaths = mutableListOf<String>()

  override suspend fun exportBackup(
    destinationPath: String,
    includeChatHistory: Boolean,
  ): NanoAIResult<String> {
    exportedPaths += destinationPath
    exportWithChats += includeChatHistory
    return NanoAIResult.success(destinationPath)
  }

  override suspend fun importBackup(sourcePath: String): NanoAIResult<Unit> {
    importedPaths += sourcePath
    return NanoAIResult.success(Unit)
  }

  override suspend fun validateBackup(sourcePath: String): NanoAIResult<Boolean> {
    validatedPaths += sourcePath
    return NanoAIResult.success(true)
  }
}
