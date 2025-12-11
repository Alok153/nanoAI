package com.vjaykrsna.nanoai.feature.settings.data

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.NanoAIResult.Companion.recoverable
import com.vjaykrsna.nanoai.core.common.NanoAIResult.Companion.success
import com.vjaykrsna.nanoai.core.domain.library.ExportBackupUseCase
import com.vjaykrsna.nanoai.core.domain.settings.BackupLocation
import com.vjaykrsna.nanoai.core.domain.settings.ImportService
import com.vjaykrsna.nanoai.feature.settings.domain.BackupRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Data source contract for exporting and importing user backups. */
interface BackupDataSource {
  suspend fun exportBackup(
    destinationPath: String,
    includeChatHistory: Boolean,
  ): NanoAIResult<String>

  suspend fun importBackup(sourcePath: String): NanoAIResult<Unit>

  suspend fun validateBackup(sourcePath: String): NanoAIResult<Boolean>
}

/** Data source backed by core import/export services. */
@Singleton
class CoreBackupDataSource
@Inject
constructor(
  private val exportBackupUseCase: ExportBackupUseCase,
  private val importService: ImportService,
) : BackupDataSource {

  override suspend fun exportBackup(
    destinationPath: String,
    includeChatHistory: Boolean,
  ): NanoAIResult<String> = exportBackupUseCase.invoke(destinationPath, includeChatHistory)

  override suspend fun importBackup(sourcePath: String): NanoAIResult<Unit> =
    mapImportResult(importService.importBackup(BackupLocation(sourcePath))) { success(Unit) }

  override suspend fun validateBackup(sourcePath: String): NanoAIResult<Boolean> =
    mapImportResult(importService.importBackup(BackupLocation(sourcePath))) { success(true) }

  private inline fun <T> mapImportResult(
    result: NanoAIResult<*>,
    onSuccess: () -> NanoAIResult<T>,
  ): NanoAIResult<T> =
    when (result) {
      is NanoAIResult.Success<*> -> onSuccess()
      is NanoAIResult.RecoverableError ->
        recoverable(
          message = result.message,
          retryAfterSeconds = result.retryAfterSeconds,
          telemetryId = result.telemetryId,
          cause = result.cause,
          context = result.context,
        )
      is NanoAIResult.FatalError -> result
    }
}

/** Repository implementation mediating between the domain and data layers. */
@Singleton
class DefaultBackupRepository @Inject constructor(private val dataSource: BackupDataSource) :
  BackupRepository {

  override suspend fun exportBackup(
    destinationPath: String,
    includeChatHistory: Boolean,
  ): NanoAIResult<String> = dataSource.exportBackup(destinationPath, includeChatHistory)

  override suspend fun importBackup(sourcePath: String): NanoAIResult<Unit> =
    dataSource.importBackup(sourcePath)

  override suspend fun validateBackup(sourcePath: String): NanoAIResult<Boolean> =
    dataSource.validateBackup(sourcePath)
}
