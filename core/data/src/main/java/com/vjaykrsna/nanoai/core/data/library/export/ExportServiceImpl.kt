package com.vjaykrsna.nanoai.core.data.library.export

import android.util.Log
import com.vjaykrsna.nanoai.core.domain.library.ExportService
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.domain.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.domain.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.domain.repository.PersonaRepository
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/** Default implementation that aggregates local data and emits a zip bundle. */
@Singleton
class ExportServiceImpl
@Inject
constructor(
  private val personaRepository: PersonaRepository,
  private val apiProviderConfigRepository: ApiProviderConfigRepository,
  private val conversationRepository: ConversationRepository,
  private val json: Json,
) : ExportService {
  override suspend fun gatherPersonas(): List<PersonaProfile> = personaRepository.getAllPersonas()

  override suspend fun gatherAPIProviderConfigs(): List<APIProviderConfig> =
    apiProviderConfigRepository.getAllProviders()

  override suspend fun gatherChatHistory(): List<ChatThread> =
    conversationRepository.getAllThreads()

  override suspend fun createExportBundle(
    personas: List<PersonaProfile>,
    apiProviders: List<APIProviderConfig>,
    destinationPath: String,
    chatHistory: List<ChatThread>,
  ): String {
    val outputFile = File(destinationPath)
    outputFile.parentFile?.mkdirs()

    val payload = buildExportPayload(personas, apiProviders, chatHistory)
    val payloadJson = json.encodeToString(JsonObject.serializer(), payload)

    if (outputFile.extension.equals("zip", ignoreCase = true)) {
      ZipOutputStream(outputFile.outputStream().buffered()).use { zip ->
        zip.putNextEntry(ZipEntry("metadata.json"))
        zip.write(payloadJson.encodeToByteArray())
        zip.closeEntry()
      }
    } else {
      outputFile.writeText(payloadJson)
    }
    return outputFile.absolutePath
  }

  override suspend fun notifyUnencryptedExport(destinationPath: String) {
    Log.w(
      TAG,
      "Export bundle created at $destinationPath is not encrypted. Advise user to store securely.",
    )
  }

  private fun buildExportPayload(
    personas: List<PersonaProfile>,
    providers: List<APIProviderConfig>,
    chatThreads: List<ChatThread>,
  ): JsonObject = buildJsonObject {
    put("schemaVersion", JsonPrimitive(EXPORT_SCHEMA_VERSION))
    put("generatedAt", JsonPrimitive(Clock.System.now().toString()))
    put("personas", buildPersonasArray(personas))
    put("apiProviders", buildProvidersArray(providers))
    put("chatThreads", buildThreadsArray(chatThreads))
  }

  private fun buildPersonasArray(personas: List<PersonaProfile>): JsonArray = buildJsonArray {
    personas.forEach { persona ->
      add(
        buildJsonObject {
          putString("id", persona.personaId.toString())
          putString("name", persona.name)
          putString("description", persona.description)
          putString("systemPrompt", persona.systemPrompt)
          putString("defaultModel", persona.defaultModelPreference)
          putFloat("temperature", persona.temperature)
          putFloat("topP", persona.topP)
          putString("defaultVoice", persona.defaultVoice)
          putString("defaultImageStyle", persona.defaultImageStyle)
          putString("createdAt", persona.createdAt.toString())
          putString("updatedAt", persona.updatedAt.toString())
        }
      )
    }
  }

  private fun buildProvidersArray(providers: List<APIProviderConfig>): JsonArray = buildJsonArray {
    providers.forEach { provider ->
      add(
        buildJsonObject {
          putString("id", provider.providerId)
          putString("name", provider.providerName)
          putString("baseUrl", provider.baseUrl)
          putString("apiType", provider.apiType.name)
          put("enabled", JsonPrimitive(provider.isEnabled))
          putString("quotaResetAt", provider.quotaResetAt?.toString())
          putString("lastStatus", provider.lastStatus.name)
          put("hasCredential", JsonPrimitive(provider.hasCredential))
        }
      )
    }
  }

  private fun buildThreadsArray(threads: List<ChatThread>): JsonArray = buildJsonArray {
    threads.forEach { thread ->
      add(
        buildJsonObject {
          putString("threadId", thread.threadId.toString())
          putString("title", thread.title)
          putString("personaId", thread.personaId?.toString())
          putString("activeModelId", thread.activeModelId)
          putString("createdAt", thread.createdAt.toString())
          putString("updatedAt", thread.updatedAt.toString())
          put("isArchived", JsonPrimitive(thread.isArchived))
        }
      )
    }
  }

  private fun JsonObjectBuilder.putString(key: String, value: String?) {
    put(key, value?.let(::JsonPrimitive) ?: JsonNull)
  }

  private fun JsonObjectBuilder.putFloat(key: String, value: Float?) {
    put(key, value?.let { JsonPrimitive(it) } ?: JsonNull)
  }

  companion object {
    private const val TAG = "ExportService"
    private const val EXPORT_SCHEMA_VERSION = 1
  }
}
