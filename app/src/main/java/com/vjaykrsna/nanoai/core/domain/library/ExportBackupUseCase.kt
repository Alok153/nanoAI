package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/** Use case for exporting application data as backup bundles. */
@Singleton
class ExportBackupUseCase @Inject constructor(private val exportService: ExportService) {
  /** Export personas, provider configs, and optional chat history as bundle. */
  @OneShot("Export backup bundle")
  suspend fun invoke(destinationPath: String, includeChatHistory: Boolean): NanoAIResult<String> {
    return try {
      val personas = exportService.gatherPersonas()
      val providers = exportService.gatherAPIProviderConfigs()
      val chatHistory = if (includeChatHistory) exportService.gatherChatHistory() else emptyList()

      val bundlePath =
        exportService.createExportBundle(personas, providers, destinationPath, chatHistory)
      exportService.notifyUnencryptedExport(bundlePath)
      NanoAIResult.success(bundlePath)
    } catch (cancellationException: CancellationException) {
      throw cancellationException
    } catch (ioException: IOException) {
      NanoAIResult.recoverable(
        message = "Failed to export backup to $destinationPath",
        cause = ioException,
        context =
          mapOf(
            "destinationPath" to destinationPath,
            "includeChatHistory" to includeChatHistory.toString(),
          ),
      )
    } catch (securityException: SecurityException) {
      NanoAIResult.fatal(
        message = "Security policy prevented backup export",
        supportContact = null,
        cause = securityException,
      )
    } catch (illegalStateException: IllegalStateException) {
      NanoAIResult.recoverable(
        message = "Failed to export backup to $destinationPath",
        cause = illegalStateException,
        context =
          mapOf(
            "destinationPath" to destinationPath,
            "includeChatHistory" to includeChatHistory.toString(),
          ),
      )
    }
  }
}
