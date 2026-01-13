package com.vjaykrsna.nanoai.core.data.settings.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.common.error.toErrorEnvelope
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreferenceStore
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.domain.model.ProviderCredentialMutation
import com.vjaykrsna.nanoai.core.domain.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.domain.repository.PersonaRepository
import com.vjaykrsna.nanoai.core.domain.settings.BackupLocation
import com.vjaykrsna.nanoai.core.domain.settings.ImportService
import com.vjaykrsna.nanoai.core.domain.settings.ImportSummary
import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.core.model.ProviderStatus
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
  @OneShot("Import backup bundle from local storage")
  override suspend fun importBackup(location: BackupLocation): NanoAIResult<ImportSummary> =
    runCatching {
        val bundle = readBundle(location)
        val plan = planImport(bundle)
        applyPlan(plan)
      }
      .fold(
        onSuccess = { summary -> NanoAIResult.success(summary) },
        onFailure = { throwable ->
          Log.e(TAG, "Failed to import backup", throwable)
          NanoAIResult.recoverable(
            message = throwable.toErrorEnvelope(IMPORT_FAILURE_MESSAGE).userMessage,
            cause = throwable,
            context = mapOf("descriptor" to location.value),
          )
        },
      )

  @OneShot("Validate backup bundle without applying changes")
  override suspend fun validateBackup(location: BackupLocation): NanoAIResult<ImportSummary> =
    runCatching {
        val bundle = readBundle(location)
        val plan = planImport(bundle)
        plan.toSummary()
      }
      .fold(
        onSuccess = { summary -> NanoAIResult.success(summary) },
        onFailure = { throwable ->
          Log.e(TAG, "Failed to validate backup", throwable)
          NanoAIResult.recoverable(
            message = throwable.toErrorEnvelope(IMPORT_FAILURE_MESSAGE).userMessage,
            cause = throwable,
            context = mapOf("descriptor" to location.value),
          )
        },
      )

  private suspend fun readBundle(location: BackupLocation): BackupBundleDto =
    withContext(Dispatchers.IO) {
      val uri = location.toUri()
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

  private suspend fun applyPlan(plan: ImportPlan): ImportSummary {
    plan.personaImports.forEach { action ->
      when (action.action) {
        PersonaImportAction.IMPORTED -> personaRepository.createPersona(action.persona)
        PersonaImportAction.UPDATED -> personaRepository.updatePersona(action.persona)
      }
    }

    plan.providerImports.forEach { action ->
      when (action.action) {
        ProviderImportAction.IMPORTED ->
          apiProviderConfigRepository.addProvider(action.config, action.credentialMutation)
        ProviderImportAction.UPDATED ->
          apiProviderConfigRepository.updateProvider(action.config, action.credentialMutation)
      }
    }

    applySettings(plan.settings)

    return plan.toSummary()
  }

  private suspend fun planImport(bundle: BackupBundleDto): ImportPlan {
    val now = Clock.System.now()
    val personaPlans = planPersonas(bundle.personas, now)
    val providerPlans = planProviders(bundle.apiProviders)
    return ImportPlan(personaPlans, providerPlans, bundle.settings)
  }

  private suspend fun planPersonas(
    personas: List<BackupPersonaDto>,
    defaultTimestamp: Instant,
  ): List<PersonaImport> {
    return personas.map { dto ->
      val personaId = dto.resolvePersonaId()
      val existing = personaRepository.getPersona(personaId)
      val persona = dto.toPersonaProfile(personaId, existing, defaultTimestamp)
      val action =
        if (existing == null) PersonaImportAction.IMPORTED else PersonaImportAction.UPDATED
      PersonaImport(persona = persona, action = action)
    }
  }

  private suspend fun planProviders(providers: List<BackupApiProviderDto>): List<ProviderImport> {
    return providers.map { dto ->
      val providerId = dto.id.ifBlank { UUID.randomUUID().toString() }
      val existing = apiProviderConfigRepository.getProvider(providerId)
      val baseConfig =
        existing?.copy(
          providerName = dto.name,
          baseUrl = dto.baseUrl,
          apiType = dto.apiType?.let(::parseApiType) ?: existing.apiType,
          isEnabled = dto.enabled ?: existing.isEnabled,
        ) ?: buildNewProvider(providerId, dto)

      val credentialMutation =
        dto.apiKey?.let { ProviderCredentialMutation.Replace(it) }
          ?: ProviderCredentialMutation.None

      val action =
        if (existing == null) ProviderImportAction.IMPORTED else ProviderImportAction.UPDATED
      ProviderImport(config = baseConfig, credentialMutation = credentialMutation, action = action)
    }
  }

  private fun BackupPersonaDto.toPersonaProfile(
    personaId: UUID,
    existing: PersonaProfile?,
    defaultTimestamp: Instant,
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

  private enum class ProviderImportAction {
    IMPORTED,
    UPDATED,
  }

  private data class PersonaImport(val persona: PersonaProfile, val action: PersonaImportAction)

  private data class ProviderImport(
    val config: APIProviderConfig,
    val credentialMutation: ProviderCredentialMutation,
    val action: ProviderImportAction,
  )

  private data class ImportPlan(
    val personaImports: List<PersonaImport>,
    val providerImports: List<ProviderImport>,
    val settings: BackupSettingsDto?,
  )

  private fun ImportPlan.toSummary(): ImportSummary {
    val personasImported = personaImports.count { it.action == PersonaImportAction.IMPORTED }
    val providersImported = providerImports.count { it.action == ProviderImportAction.IMPORTED }
    return ImportSummary(
      personasImported = personasImported,
      personasUpdated = personaImports.size - personasImported,
      providersImported = providersImported,
      providersUpdated = providerImports.size - providersImported,
    )
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

  private fun BackupLocation.toUri(): Uri = Uri.parse(value)

  private sealed class ImportException(message: String, cause: Throwable? = null) :
    IOException(message, cause) {
    class FileNotFound(message: String) : ImportException(message)

    class InvalidFormat(message: String, cause: Throwable? = null) :
      ImportException(message, cause)
  }

  companion object {
    private const val TAG = "ImportService"
    private const val DEFAULT_TEMPERATURE = 1.0f
    private const val DEFAULT_TOP_P = 1.0f
    private const val ZIP_SIGNATURE_LENGTH = 2
    private const val ZIP_MAGIC_FIRST = 'P'.code
    private const val ZIP_MAGIC_SECOND = 'K'.code
    private const val IMPORT_FAILURE_MESSAGE = "Failed to import backup"
  }
}
