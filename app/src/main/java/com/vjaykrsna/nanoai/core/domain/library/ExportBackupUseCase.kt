package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import javax.inject.Inject
import javax.inject.Singleton

/** Use case for exporting application data as backup bundles. */
@Singleton
class ExportBackupUseCase @Inject constructor(private val exportService: ExportService) {
  /** Export personas, provider configs, and optional chat history as bundle. */
  suspend fun invoke(destinationPath: String, includeChatHistory: Boolean): NanoAIResult<String> {
    return try {
      val personas = exportService.gatherPersonas()
      val providers = exportService.gatherAPIProviderConfigs()
      val chatHistory = if (includeChatHistory) exportService.gatherChatHistory() else emptyList()

      val bundlePath =
        exportService.createExportBundle(personas, providers, destinationPath, chatHistory)
      exportService.notifyUnencryptedExport(bundlePath)
      NanoAIResult.success(bundlePath)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to export backup to $destinationPath",
        cause = e,
        context =
          mapOf(
            "destinationPath" to destinationPath,
            "includeChatHistory" to includeChatHistory.toString(),
          ),
      )
    }
  }
}
