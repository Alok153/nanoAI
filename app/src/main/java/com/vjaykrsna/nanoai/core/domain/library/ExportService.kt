package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile

/**
 * Abstraction responsible for aggregating data and producing export bundles for backups and
 * migrations.
 */
interface ExportService {
  /** Collect persona profiles to include in export bundle. */
  suspend fun gatherPersonas(): List<PersonaProfile>

  /** Collect configured cloud API providers to include in export bundle. */
  suspend fun gatherAPIProviderConfigs(): List<APIProviderConfig>

  /** Optionally collect chat history for exports when requested. */
  suspend fun gatherChatHistory(): List<ChatThread>

  /**
   * Create an export bundle on disk and return the destination path.
   *
   * @param personas Persona profiles to serialise into bundle metadata.
   * @param apiProviders Cloud provider configurations to store alongside personas.
   * @param destinationPath Target file path for generated archive.
   * @param chatHistory Optional ChatThread metadata to include when requested by user.
   */
  suspend fun createExportBundle(
    personas: List<PersonaProfile>,
    apiProviders: List<APIProviderConfig>,
    destinationPath: String,
    chatHistory: List<ChatThread> = emptyList(),
  ): String

  /**
   * Notify user-space that an export bundle is not encrypted and should be stored safely. Default
   * no-op for implementations that surface warning via other channels.
   */
  suspend fun notifyUnencryptedExport(destinationPath: String) {}
}
