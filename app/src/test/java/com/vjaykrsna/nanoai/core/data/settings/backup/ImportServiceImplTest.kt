package com.vjaykrsna.nanoai.core.data.settings.backup

import android.net.Uri
import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreferenceStore
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.domain.model.ProviderCredentialMutation
import com.vjaykrsna.nanoai.core.domain.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.domain.repository.PersonaRepository
import com.vjaykrsna.nanoai.core.domain.settings.ImportService
import com.vjaykrsna.nanoai.core.model.APIType
import java.io.ByteArrayInputStream
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class ImportServiceImplTest {
  private lateinit var context: android.content.Context
  private lateinit var preferenceStore: PrivacyPreferenceStore
  private lateinit var personaRepository: FakePersonaRepository
  private lateinit var providerRepository: FakeApiProviderRepository
  private lateinit var importService: ImportService
  private lateinit var shadowContentResolver: ShadowContentResolver

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    preferenceStore = PrivacyPreferenceStore(context)
    runBlocking { preferenceStore.reset() }
    personaRepository = FakePersonaRepository()
    providerRepository = FakeApiProviderRepository()
    val importJson = Json {
      ignoreUnknownKeys = true
      encodeDefaults = true
      explicitNulls = false
    }

    importService =
      ImportServiceImpl(
        context = context,
        json = importJson,
        personaRepository = personaRepository,
        apiProviderConfigRepository = providerRepository,
        privacyPreferenceStore = preferenceStore,
      )
    shadowContentResolver = shadowOf(context.contentResolver)
  }

  @Test
  fun `importBackup applies personas providers and settings`() = runTest {
    val payload =
      """
                {
                  "personas": [
                    {
                      "personaId": "${KNOWN_PERSONA_ID}",
                      "name": "Helpful",
                      "description": "Helpful assistant",
                      "systemPrompt": "Be helpful",
                      "defaultModelPreference": "gpt-4",
                      "temperature": 0.7,
                      "topP": 0.9,
                      "createdAt": "2024-01-01T00:00:00Z",
                      "updatedAt": "2024-01-02T00:00:00Z"
                    }
                  ],
                  "apiProviders": [
                    {
                      "providerId": "openai",
                      "name": "OpenAI",
                      "endpoint": "https://api.openai.com/v1",
                      "apiKey": "sk-123",
                      "apiType": "OPENAI_COMPATIBLE",
                      "enabled": true
                    }
                  ],
                  "settings": {
                    "telemetryOptIn": true,
                    "exportWarningsDismissed": true
                  }
                }
            """
        .trimIndent()

    val uri = Uri.parse("content://nanoai/backup.json")
    shadowContentResolver.registerInputStream(uri, ByteArrayInputStream(payload.toByteArray()))

    val summary = importService.importBackup(uri).getOrThrow()
    advanceUntilIdle()

    assertThat(summary.personasImported).isEqualTo(1)
    assertThat(summary.personasUpdated).isEqualTo(0)
    assertThat(summary.providersImported).isEqualTo(1)
    assertThat(summary.providersUpdated).isEqualTo(0)

    val personas = personaRepository.getAllPersonas()
    assertThat(personas).hasSize(1)
    val persona = personas.first()
    assertThat(persona.personaId.toString()).isEqualTo(KNOWN_PERSONA_ID)
    assertThat(persona.defaultModelPreference).isEqualTo("gpt-4")
    assertThat(persona.temperature).isWithin(0.0001f).of(0.7f)
    assertThat(persona.topP).isWithin(0.0001f).of(0.9f)

    val providers = providerRepository.getAllProviders()
    assertThat(providers).hasSize(1)
    val provider = providers.first()
    assertThat(provider.hasCredential).isTrue()
    assertThat(provider.baseUrl).isEqualTo("https://api.openai.com/v1")
    assertThat(provider.apiType).isEqualTo(APIType.OPENAI_COMPATIBLE)

    val preferences = preferenceStore.privacyPreference.first()
    assertThat(preferences.telemetryOptIn).isTrue()
    assertThat(preferences.exportWarningsDismissed).isTrue()
  }

  @Test
  fun `importBackup returns failure for invalid payload`() = runTest {
    val uri = Uri.parse("content://nanoai/invalid.json")
    shadowContentResolver.registerInputStream(uri, ByteArrayInputStream("not-json".toByteArray()))

    val result = importService.importBackup(uri)

    assertThat(result.isFailure).isTrue()
    assertThat(personaRepository.getAllPersonas()).isEmpty()
    assertThat(providerRepository.getAllProviders()).isEmpty()
  }

  private class FakePersonaRepository : PersonaRepository {
    private val personas = LinkedHashMap<UUID, PersonaProfile>()
    private val personasFlow = MutableStateFlow<List<PersonaProfile>>(emptyList())

    override suspend fun getAllPersonas(): List<PersonaProfile> = personas.values.toList()

    override suspend fun getPersona(personaId: UUID): PersonaProfile? = personas[personaId]

    override suspend fun getPersonaById(personaId: UUID): Flow<PersonaProfile?> =
      personasFlow.map { list -> list.firstOrNull { it.personaId == personaId } }

    override suspend fun createPersona(persona: PersonaProfile) {
      personas[persona.personaId] = persona
      personasFlow.update { personas.values.toList() }
    }

    override suspend fun updatePersona(persona: PersonaProfile) {
      personas[persona.personaId] = persona
      personasFlow.update { personas.values.toList() }
    }

    override suspend fun deletePersona(personaId: UUID) {
      personas.remove(personaId)
      personasFlow.update { personas.values.toList() }
    }

    override suspend fun getDefaultPersona(): PersonaProfile? = personas.values.firstOrNull()

    override fun observeAllPersonas(): Flow<List<PersonaProfile>> = personasFlow
  }

  private class FakeApiProviderRepository : ApiProviderConfigRepository {
    private val providers = LinkedHashMap<String, APIProviderConfig>()
    private val providersFlow = MutableStateFlow<List<APIProviderConfig>>(emptyList())
    private var aliasCounter = 0

    override suspend fun getAllProviders(): List<APIProviderConfig> = providers.values.toList()

    override suspend fun getProvider(providerId: String): APIProviderConfig? = providers[providerId]

    override suspend fun addProvider(
      config: APIProviderConfig,
      credentialMutation: ProviderCredentialMutation,
    ) {
      val updated = applyCredentialMutation(config, credentialMutation)
      providers[updated.providerId] = updated
      providersFlow.update { providers.values.toList() }
    }

    override suspend fun updateProvider(
      config: APIProviderConfig,
      credentialMutation: ProviderCredentialMutation,
    ) {
      val updated = applyCredentialMutation(config, credentialMutation)
      providers[updated.providerId] = updated
      providersFlow.update { providers.values.toList() }
    }

    override suspend fun deleteProvider(providerId: String) {
      providers.remove(providerId)
      providersFlow.update { providers.values.toList() }
    }

    override suspend fun getEnabledProviders(): List<APIProviderConfig> =
      providers.values.filter { it.isEnabled }

    override fun observeAllProviders(): Flow<List<APIProviderConfig>> = providersFlow

    private fun applyCredentialMutation(
      config: APIProviderConfig,
      mutation: ProviderCredentialMutation,
    ): APIProviderConfig =
      when (mutation) {
        ProviderCredentialMutation.None -> config
        ProviderCredentialMutation.Remove -> config.copy(credentialId = null)
        is ProviderCredentialMutation.Replace ->
          config.copy(credentialId = config.credentialId ?: newAlias(config.providerId))
      }

    private fun newAlias(providerId: String): String {
      aliasCounter += 1
      return "$providerId-alias-$aliasCounter"
    }
  }

  companion object {
    private const val KNOWN_PERSONA_ID = "b6b94d0b-7f3f-4e69-9f44-519c0d53a9f3"
  }
}
