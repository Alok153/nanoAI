package com.vjaykrsna.nanoai.feature.settings.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreferenceStore
import com.vjaykrsna.nanoai.core.data.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.data.repository.PersonaRepository
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.core.model.ProviderStatus
import com.vjaykrsna.nanoai.feature.settings.domain.ImportService
import com.vjaykrsna.nanoai.feature.settings.domain.ImportSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Singleton
class ImportServiceImpl
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val json: Json,
  private val personaRepository: PersonaRepository,
  private val apiProviderConfigRepository: ApiProviderConfigRepository,
  private val privacyPreferenceStore: PrivacyPreferenceStore,
) : ImportService {
  override suspend fun importBackup(uri: Uri): Result<ImportSummary> =
    runCatching {
        val bundle = readBundle(uri)
        applyBundle(bundle)
      }
      .onFailure { error -> Log.e(TAG, "Failed to import backup", error) }

  private suspend fun readBundle(uri: Uri): BackupBundleDto =
    withContext(Dispatchers.IO) {
      val resolver = context.contentResolver
      val rawBytes =
        resolver.openInputStream(uri)?.use { input -> input.readBytes() }
          ?: throw ImportException.FileNotFound("Unable to open backup file: $uri")

      val payload = decodePayload(rawBytes)
      try {
        json.decodeFromString(BackupBundleDto.serializer(), payload)
      } catch (error: SerializationException) {
        throw ImportException.InvalidFormat("Invalid backup JSON", error)
      }
    }

  private suspend fun applyBundle(bundle: BackupBundleDto): ImportSummary {
    val personaResult = importPersonas(bundle.personas)
    val providerResult = importProviders(bundle.apiProviders)
    applySettings(bundle.settings)
    return ImportSummary(
      personasImported = personaResult.first,
      personasUpdated = personaResult.second,
      providersImported = providerResult.first,
      providersUpdated = providerResult.second,
    )
  }

  private suspend fun importPersonas(personas: List<BackupPersonaDto>): Pair<Int, Int> {
    var imported = 0
    var updated = 0
    val now = Clock.System.now()
    for (dto in personas) {
      when (processPersona(dto, now)) {
        PersonaImportAction.IMPORTED -> imported += 1
        PersonaImportAction.UPDATED -> updated += 1
      }
    }
    return imported to updated
  }

  private suspend fun processPersona(
    dto: BackupPersonaDto,
    defaultTimestamp: Instant
  ): PersonaImportAction {
    val personaId = dto.resolvePersonaId()
    val existing = personaRepository.getPersona(personaId)
    val persona = dto.toPersonaProfile(personaId, existing, defaultTimestamp)
    return if (existing == null) {
      personaRepository.createPersona(persona)
      PersonaImportAction.IMPORTED
    } else {
      personaRepository.updatePersona(persona)
      PersonaImportAction.UPDATED
    }
  }

  private fun BackupPersonaDto.toPersonaProfile(
    personaId: UUID,
    existing: PersonaProfile?,
    defaultTimestamp: Instant
  ): PersonaProfile {
    val persistedCreatedAt = resolveCreatedAt(existing, defaultTimestamp)
    val persistedUpdatedAt = resolveUpdatedAt(defaultTimestamp)
    return PersonaProfile(
      personaId = personaId,
      name = name,
      description = description.orEmpty(),
      systemPrompt = systemPrompt,
      defaultModelPreference = resolveModelPreference(existing),
      temperature = resolveTemperature(existing),
      topP = resolveTopP(existing),
      defaultVoice = resolveDefaultVoice(existing),
      defaultImageStyle = resolveDefaultImageStyle(existing),
      createdAt = persistedCreatedAt,
      updatedAt = persistedUpdatedAt,
    )
  }

  private fun BackupPersonaDto.resolveCreatedAt(
    existing: PersonaProfile?,
    fallback: Instant,
  ): Instant = existing?.createdAt ?: createdAt?.let(::parseInstant) ?: fallback

  private fun BackupPersonaDto.resolveUpdatedAt(fallback: Instant): Instant =
    updatedAt?.let(::parseInstant) ?: fallback

  private fun BackupPersonaDto.resolveModelPreference(existing: PersonaProfile?): String? =
    defaultModelPreference ?: existing?.defaultModelPreference

  private fun BackupPersonaDto.resolveTemperature(existing: PersonaProfile?): Float =
    temperature ?: existing?.temperature ?: DEFAULT_TEMPERATURE

  private fun BackupPersonaDto.resolveTopP(existing: PersonaProfile?): Float =
    topP ?: existing?.topP ?: DEFAULT_TOP_P

  private fun BackupPersonaDto.resolveDefaultVoice(existing: PersonaProfile?): String? =
    defaultVoice ?: existing?.defaultVoice

  private fun BackupPersonaDto.resolveDefaultImageStyle(existing: PersonaProfile?): String? =
    defaultImageStyle ?: existing?.defaultImageStyle

  private fun BackupPersonaDto.resolvePersonaId(): UUID {
    val parsed = id?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    return parsed ?: UUID.randomUUID()
  }

  private enum class PersonaImportAction {
    IMPORTED,
    UPDATED,
  }

  private suspend fun importProviders(providers: List<BackupApiProviderDto>): Pair<Int, Int> {
    var imported = 0
    var updated = 0
    for (dto in providers) {
      val providerId = dto.id.ifBlank { UUID.randomUUID().toString() }
      val existing = apiProviderConfigRepository.getProvider(providerId)
      val baseConfig =
        existing?.copy(
          providerName = dto.name,
          baseUrl = dto.baseUrl,
          apiKey = dto.apiKey ?: existing.apiKey,
          apiType = dto.apiType?.let(::parseApiType) ?: existing.apiType,
          isEnabled = dto.enabled ?: existing.isEnabled,
        ) ?: buildNewProvider(providerId, dto)

      if (existing == null) {
        apiProviderConfigRepository.addProvider(baseConfig)
        imported += 1
      } else {
        apiProviderConfigRepository.updateProvider(baseConfig)
        updated += 1
      }
    }
    return imported to updated
  }

  private suspend fun applySettings(settings: BackupSettingsDto?) {
    settings ?: return
    settings.telemetryOptIn?.let { privacyPreferenceStore.setTelemetryOptIn(it) }
    settings.exportWarningsDismissed?.let { privacyPreferenceStore.setExportWarningsDismissed(it) }
  }

  private fun buildNewProvider(providerId: String, dto: BackupApiProviderDto): APIProviderConfig =
    APIProviderConfig(
      providerId = providerId,
      providerName = dto.name,
      baseUrl = dto.baseUrl,
      apiKey = dto.apiKey.orEmpty(),
      apiType = dto.apiType?.let(::parseApiType) ?: APIType.OPENAI_COMPATIBLE,
      isEnabled = dto.enabled ?: true,
      quotaResetAt = null,
      lastStatus = ProviderStatus.UNKNOWN,
    )

  private fun parseInstant(value: String): Instant =
    runCatching { Instant.parse(value) }.getOrElse { Clock.System.now() }

  private fun parseApiType(value: String): APIType =
    runCatching { APIType.valueOf(value.uppercase()) }.getOrDefault(APIType.OPENAI_COMPATIBLE)

  private fun decodePayload(bytes: ByteArray): String {
    if (bytes.hasZipSignature()) {
      return decodeZipPayload(bytes)
    }
    val trimmed = bytes.decodeToString().trim()
    return when {
      trimmed.startsWith("{") -> trimmed
      else -> decodeBase64(trimmed)
    }
  }

  private fun decodeZipPayload(bytes: ByteArray): String {
    ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
      generateSequence { zip.nextEntry }
        .firstOrNull { it.name.endsWith(".json", ignoreCase = true) }
        ?.let {
          val output = ByteArrayOutputStream()
          val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
          var read: Int
          while (zip.read(buffer).also { read = it } != -1) {
            output.write(buffer, 0, read)
          }
          return String(output.toByteArray(), Charsets.UTF_8)
        }
    }
    throw ImportException.InvalidFormat("ZIP archive does not contain a JSON file")
  }

  @OptIn(ExperimentalEncodingApi::class)
  private fun decodeBase64(encoded: String): String =
    try {
      val decoded = Base64.decode(encoded)
      decoded.decodeToString()
    } catch (error: IllegalArgumentException) {
      throw ImportException.InvalidFormat("Backup payload is neither JSON nor Base64", error)
    }

  private fun ByteArray.hasZipSignature(): Boolean {
    if (size < ZIP_SIGNATURE_LENGTH) {
      return false
    }
    return this[0].toInt() == ZIP_MAGIC_FIRST && this[1].toInt() == ZIP_MAGIC_SECOND
  }

  private sealed class ImportException(
    message: String,
    cause: Throwable? = null,
  ) : IOException(message, cause) {
    class FileNotFound(
      message: String,
    ) : ImportException(message)

    class InvalidFormat(
      message: String,
      cause: Throwable? = null,
    ) : ImportException(message, cause)
  }

  companion object {
    private const val TAG = "ImportService"
    private const val DEFAULT_TEMPERATURE = 1.0f
    private const val DEFAULT_TOP_P = 1.0f
    private const val ZIP_SIGNATURE_LENGTH = 2
    private const val ZIP_MAGIC_FIRST = 'P'.code
    private const val ZIP_MAGIC_SECOND = 'K'.code
  }
}
