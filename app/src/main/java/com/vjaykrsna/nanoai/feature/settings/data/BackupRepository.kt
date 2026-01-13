package com.vjaykrsna.nanoai.feature.settings.data

import android.util.Log
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.NanoAIResult.Companion.recoverable
import com.vjaykrsna.nanoai.core.common.NanoAIResult.Companion.success
import com.vjaykrsna.nanoai.core.domain.library.ExportBackupUseCase
import com.vjaykrsna.nanoai.core.domain.settings.BackupLocation
import com.vjaykrsna.nanoai.core.domain.settings.ImportService
import com.vjaykrsna.nanoai.core.domain.settings.ImportSummary
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

  suspend fun validateBackup(sourcePath: String): NanoAIResult<ImportSummary>
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
  ): NanoAIResult<String> {
    Log.i(TAG, "Starting export backup to $destinationPath (includeChatHistory=$includeChatHistory)")
    return exportBackupUseCase.invoke(destinationPath, includeChatHistory).also { result ->
      when (result) {
        is NanoAIResult.Success -> {
          Log.w(
            TAG,
            "ENCRYPTION_WARNING: Export completed to ${result.value}. " +
              "Backup is NOT encrypted - user should store securely and delete when no longer needed."
          )
        }
        else -> Log.e(TAG, "Export backup failed: $result")
      }
    }
  }

  override suspend fun importBackup(sourcePath: String): NanoAIResult<Unit> {
    Log.i(TAG, "Starting import backup from $sourcePath")
    return mapImportResult(importService.importBackup(BackupLocation(sourcePath))) { result ->
      Log.i(
        TAG,
        "Import completed: ${result.value.personasImported + result.value.personasUpdated} personas, " +
          "${result.value.providersImported + result.value.providersUpdated} providers"
      )
      success(Unit)
    }
  }

  override suspend fun validateBackup(sourcePath: String): NanoAIResult<ImportSummary> {
    Log.i(TAG, "Validating backup from $sourcePath")
    return mapImportResult(importService.validateBackup(BackupLocation(sourcePath))) { result ->
      Log.i(
        TAG,
        "Validation completed: ${result.value.personasImported + result.value.personasUpdated} personas, " +
          "${result.value.providersImported + result.value.providersUpdated} providers would be restored"
      )
      success(result.value)
    }
  }

  private inline fun <T, R> mapImportResult(
    result: NanoAIResult<T>,
    onSuccess: (NanoAIResult.Success<T>) -> NanoAIResult<R>,
  ): NanoAIResult<R> =
    when (result) {
      is NanoAIResult.Success<T> -> onSuccess(result)
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

  private companion object {
    private const val TAG = "BackupDataSource"
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

  override suspend fun validateBackup(sourcePath: String): NanoAIResult<ImportSummary> =
    dataSource.validateBackup(sourcePath)
}
