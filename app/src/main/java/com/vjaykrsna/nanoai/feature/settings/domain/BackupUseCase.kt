package com.vjaykrsna.nanoai.feature.settings.domain

import com.vjaykrsna.nanoai.core.domain.settings.ImportSummary
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import javax.inject.Inject

/** Repository contract for backup and restore flows in Settings. */
interface BackupRepository {
  suspend fun exportBackup(
    destinationPath: String,
    includeChatHistory: Boolean,
  ): NanoAIResult<String>

  suspend fun importBackup(sourcePath: String): NanoAIResult<ImportSummary>

  suspend fun validateBackup(sourcePath: String): NanoAIResult<ImportSummary>
}

/** Use case wrapper that keeps Settings ViewModels decoupled from data sources. */
class BackupUseCase @Inject constructor(private val repository: BackupRepository) {

  suspend fun exportBackup(
    destinationPath: String,
    includeChatHistory: Boolean,
  ): NanoAIResult<String> = repository.exportBackup(destinationPath, includeChatHistory)

  suspend fun importBackup(sourcePath: String): NanoAIResult<ImportSummary> =
    repository.importBackup(sourcePath)

  suspend fun validateBackup(sourcePath: String): NanoAIResult<ImportSummary> =
    repository.validateBackup(sourcePath)
}
