package com.vjaykrsna.nanoai.feature.library.data.export

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.data.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.data.repository.PersonaRepository
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.core.model.ProviderStatus
import java.io.File
import java.util.UUID
import kotlin.io.path.createTempFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Before
import org.junit.Test

class ExportServiceImplTest {
  private val json = Json { prettyPrint = true }

  private lateinit var personaRepository: FakePersonaRepository
  private lateinit var apiProviderRepository: FakeApiProviderConfigRepository
  private lateinit var conversationRepository: FakeConversationRepository

  private lateinit var exportService: ExportServiceImpl

  @Before
  fun setup() {
    personaRepository = FakePersonaRepository()
    apiProviderRepository = FakeApiProviderConfigRepository()
    conversationRepository = FakeConversationRepository()

    exportService =
      ExportServiceImpl(
        personaRepository = personaRepository,
        apiProviderConfigRepository = apiProviderRepository,
        conversationRepository = conversationRepository,
        json = json,
      )
  }

  @Test
  fun `createExportBundle writes personas and providers keys`() = runTest {
    val now = Clock.System.now()
    val persona =
      PersonaProfile(
        personaId = UUID.randomUUID(),
        name = "Test Persona",
        description = "Offline assistant",
        systemPrompt = "You are helpful",
        defaultModelPreference = "gemini-2.0",
        temperature = 0.7f,
        topP = 0.95f,
        defaultVoice = null,
        defaultImageStyle = null,
        createdAt = now,
        updatedAt = now,
      )
    val provider =
      APIProviderConfig(
        providerId = "openai",
        providerName = "OpenAI",
        baseUrl = "https://api.openai.com",
        apiKey = "sk-test",
        apiType = APIType.OPENAI_COMPATIBLE,
        isEnabled = true,
        quotaResetAt = null,
        lastStatus = ProviderStatus.OK,
      )

    personaRepository.personas = listOf(persona)
    apiProviderRepository.providers = listOf(provider)

    val tempFile =
      createTempFile(prefix = "nanoai-export", suffix = ".json").toFile().apply { deleteOnExit() }

    val exportedPath =
      exportService.createExportBundle(
        personas = personaRepository.getAllPersonas(),
        apiProviders = apiProviderRepository.getAllProviders(),
        destinationPath = tempFile.absolutePath,
        chatHistory = emptyList(),
      )

    val exportedFile = File(exportedPath)
    val payload = json.parseToJsonElement(exportedFile.readText()).jsonObject

    assertThat(payload["personas"]).isNotNull()
    assertThat(payload["personas"]!!.jsonArray).isNotEmpty()
    assertThat(payload["apiProviders"]).isNotNull()
    assertThat(payload["apiProviders"]!!.jsonArray).isNotEmpty()
  }

  private class FakePersonaRepository : PersonaRepository {
    var personas: List<PersonaProfile> = emptyList()

    override suspend fun getAllPersonas(): List<PersonaProfile> = personas

    override suspend fun getPersona(personaId: UUID): PersonaProfile? =
      personas.firstOrNull { it.personaId == personaId }

    override suspend fun getPersonaById(personaId: UUID): Flow<PersonaProfile?> =
      flowOf(personas.firstOrNull { it.personaId == personaId })

    override suspend fun createPersona(persona: PersonaProfile) {
      throw UnsupportedOperationException("Not required for test")
    }

    override suspend fun updatePersona(persona: PersonaProfile) {
      throw UnsupportedOperationException("Not required for test")
    }

    override suspend fun deletePersona(personaId: UUID) {
      throw UnsupportedOperationException("Not required for test")
    }

    override suspend fun getDefaultPersona(): PersonaProfile? = personas.firstOrNull()

    override fun observeAllPersonas(): Flow<List<PersonaProfile>> = flowOf(personas)
  }

  private class FakeApiProviderConfigRepository : ApiProviderConfigRepository {
    var providers: List<APIProviderConfig> = emptyList()

    override suspend fun getAllProviders(): List<APIProviderConfig> = providers

    override suspend fun getProvider(providerId: String): APIProviderConfig? =
      providers.firstOrNull { it.providerId == providerId }

    override suspend fun addProvider(config: APIProviderConfig) {
      throw UnsupportedOperationException("Not required for test")
    }

    override suspend fun updateProvider(config: APIProviderConfig) {
      throw UnsupportedOperationException("Not required for test")
    }

    override suspend fun deleteProvider(providerId: String) {
      throw UnsupportedOperationException("Not required for test")
    }

    override suspend fun getEnabledProviders(): List<APIProviderConfig> =
      providers.filter { it.isEnabled }

    override fun observeAllProviders(): Flow<List<APIProviderConfig>> = flowOf(providers)
  }

  private class FakeConversationRepository : ConversationRepository {
    override suspend fun getThread(threadId: UUID): ChatThread? =
      throw UnsupportedOperationException("Not required for test")

    override suspend fun getAllThreads(): List<ChatThread> = emptyList()

    override suspend fun getArchivedThreads(): List<ChatThread> = emptyList()

    override suspend fun createThread(thread: ChatThread) {
      throw UnsupportedOperationException("Not required for test")
    }

    override suspend fun updateThread(thread: ChatThread) {
      throw UnsupportedOperationException("Not required for test")
    }

    override suspend fun archiveThread(threadId: UUID) {
      throw UnsupportedOperationException("Not required for test")
    }

    override suspend fun deleteThread(threadId: UUID) {
      throw UnsupportedOperationException("Not required for test")
    }

    override suspend fun addMessage(message: Message) {
      throw UnsupportedOperationException("Not required for test")
    }

    override suspend fun getMessages(threadId: UUID): List<Message> = emptyList()

    override fun getMessagesFlow(threadId: UUID): Flow<List<Message>> = flowOf(emptyList())

    override fun getAllThreadsFlow(): Flow<List<ChatThread>> = flowOf(emptyList())

    override suspend fun getCurrentPersonaForThread(threadId: UUID): UUID? = null

    override suspend fun createNewThread(personaId: UUID, title: String?): UUID =
      throw UnsupportedOperationException("Not required for test")

    override suspend fun updateThreadPersona(threadId: UUID, personaId: UUID?) {
      throw UnsupportedOperationException("Not required for test")
    }
  }
}
