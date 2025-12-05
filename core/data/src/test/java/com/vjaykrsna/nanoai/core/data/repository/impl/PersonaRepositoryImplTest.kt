package com.vjaykrsna.nanoai.core.data.repository.impl

import android.os.Build
import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.NanoAIDatabase
import com.vjaykrsna.nanoai.core.data.db.daos.PersonaProfileDao
import com.vjaykrsna.nanoai.core.domain.repository.PersonaRepository
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Tests for [PersonaRepositoryImpl].
 *
 * Validates persona CRUD operations with Flow collection using an in-memory database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
@OptIn(ExperimentalCoroutinesApi::class)
class PersonaRepositoryImplTest {

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension()

  private lateinit var database: NanoAIDatabase
  private lateinit var personaProfileDao: PersonaProfileDao
  private lateinit var repository: PersonaRepository

  @Before
  fun setup() {
    database =
      Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), NanoAIDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    personaProfileDao = database.personaProfileDao()
    repository = PersonaRepositoryImpl(personaProfileDao, mainDispatcherExtension.dispatcher)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `createPersona persists persona to database`() = runTest {
    val persona = DomainTestBuilders.buildPersona(name = "Test Persona")

    repository.createPersona(persona)

    val retrieved = repository.getPersona(persona.personaId)
    assertThat(retrieved).isNotNull()
    assertThat(retrieved?.personaId).isEqualTo(persona.personaId)
    assertThat(retrieved?.name).isEqualTo("Test Persona")
  }

  @Test
  fun `getAllPersonas returns all personas`() = runTest {
    val persona1 = DomainTestBuilders.buildPersona(name = "Persona 1")
    val persona2 = DomainTestBuilders.buildPersona(name = "Persona 2")

    repository.createPersona(persona1)
    repository.createPersona(persona2)

    val all = repository.getAllPersonas()
    assertThat(all).hasSize(2)
    assertThat(all.map { it.name }).containsExactly("Persona 1", "Persona 2")
  }

  @Test
  fun `getPersona retrieves specific persona by ID`() = runTest {
    val persona = DomainTestBuilders.buildPersona(name = "Specific Persona")
    repository.createPersona(persona)

    val retrieved = repository.getPersona(persona.personaId)
    assertThat(retrieved?.name).isEqualTo("Specific Persona")
  }

  @Test
  fun `getPersona returns null for non-existent ID`() = runTest {
    val nonExistentId = UUID.randomUUID()

    val retrieved = repository.getPersona(nonExistentId)
    assertThat(retrieved).isNull()
  }

  @Test
  fun `updatePersona modifies existing persona`() = runTest {
    val persona = DomainTestBuilders.buildPersona(name = "Original Name")
    repository.createPersona(persona)

    val updated = persona.copy(name = "Updated Name")
    repository.updatePersona(updated)

    val retrieved = repository.getPersona(persona.personaId)
    assertThat(retrieved?.name).isEqualTo("Updated Name")
  }

  @Test
  fun `deletePersona removes persona from database`() = runTest {
    val persona = DomainTestBuilders.buildPersona()
    repository.createPersona(persona)

    repository.deletePersona(persona.personaId)

    val retrieved = repository.getPersona(persona.personaId)
    assertThat(retrieved).isNull()
  }

  @Test
  fun `getPersonaById emits Flow updates`() = runTest {
    val persona = DomainTestBuilders.buildPersona(name = "Flow Persona")
    repository.createPersona(persona)

    val flow = repository.getPersonaById(persona.personaId)
    val initial = flow.first()
    assertThat(initial?.name).isEqualTo("Flow Persona")

    // Update persona
    repository.updatePersona(persona.copy(name = "Updated Flow Persona"))

    val updated = flow.first()
    assertThat(updated?.name).isEqualTo("Updated Flow Persona")
  }

  @Test
  fun `observeAllPersonas emits updates reactively`() = runTest {
    val flow = repository.observeAllPersonas()

    // Initially empty
    assertThat(flow.first()).isEmpty()

    // Add persona
    val persona = DomainTestBuilders.buildPersona()
    repository.createPersona(persona)

    // Flow should emit new list
    assertThat(flow.first()).hasSize(1)
  }

  @Test
  fun `getDefaultPersona returns first persona`() = runTest {
    val now = Clock.System.now()
    val persona1 = DomainTestBuilders.buildPersona(name = "First").copy(createdAt = now)
    val persona2 =
      DomainTestBuilders.buildPersona(name = "Second")
        .copy(createdAt = now.plus(kotlin.time.Duration.parse("1s")))

    repository.createPersona(persona1)
    repository.createPersona(persona2)

    val default = repository.getDefaultPersona()
    assertThat(default?.name).isEqualTo("First")
  }

  @Test
  fun `getDefaultPersona returns null when no personas exist`() = runTest {
    val default = repository.getDefaultPersona()
    assertThat(default).isNull()
  }

  @Test
  fun `createPersona stores all persona attributes`() = runTest {
    val persona =
      DomainTestBuilders.buildPersona(
        name = "Complete Persona",
        description = "Test description",
        systemPrompt = "You are a test assistant",
        temperature = 0.7f,
        topP = 0.9f,
        defaultModelPreference = "test-model-id",
      )

    repository.createPersona(persona)

    val retrieved = repository.getPersona(persona.personaId)
    assertThat(retrieved).isNotNull()
    assertThat(retrieved?.name).isEqualTo("Complete Persona")
    assertThat(retrieved?.description).isEqualTo("Test description")
    assertThat(retrieved?.systemPrompt).isEqualTo("You are a test assistant")
    assertThat(retrieved?.temperature).isEqualTo(0.7f)
    assertThat(retrieved?.topP).isEqualTo(0.9f)
    assertThat(retrieved?.defaultModelPreference).isEqualTo("test-model-id")
  }

  @Test
  fun `updatePersona preserves persona ID`() = runTest {
    val persona = DomainTestBuilders.buildPersona(name = "Original")
    repository.createPersona(persona)

    val updated = persona.copy(name = "Updated")
    repository.updatePersona(updated)

    val retrieved = repository.getPersona(persona.personaId)
    assertThat(retrieved?.personaId).isEqualTo(persona.personaId)
  }

  @Test
  fun `updatePersona allows changing temperature and topP`() = runTest {
    val persona = DomainTestBuilders.buildPersona(temperature = 1.0f, topP = 1.0f)
    repository.createPersona(persona)

    val updated = persona.copy(temperature = 0.5f, topP = 0.8f)
    repository.updatePersona(updated)

    val retrieved = repository.getPersona(persona.personaId)
    assertThat(retrieved?.temperature).isEqualTo(0.5f)
    assertThat(retrieved?.topP).isEqualTo(0.8f)
  }

  @Test
  fun `createPersona with null model preference works`() = runTest {
    val persona = DomainTestBuilders.buildPersona(defaultModelPreference = null)

    repository.createPersona(persona)

    val retrieved = repository.getPersona(persona.personaId)
    assertThat(retrieved?.defaultModelPreference).isNull()
  }

  @Test
  fun `multiple personas can be created and retrieved`() = runTest {
    val personas = (1..5).map { index -> DomainTestBuilders.buildPersona(name = "Persona $index") }

    personas.forEach { repository.createPersona(it) }

    val all = repository.getAllPersonas()
    assertThat(all).hasSize(5)
  }

  @Test
  fun `deletePersona does not affect other personas`() = runTest {
    val persona1 = DomainTestBuilders.buildPersona(name = "Keep Me")
    val persona2 = DomainTestBuilders.buildPersona(name = "Delete Me")

    repository.createPersona(persona1)
    repository.createPersona(persona2)

    repository.deletePersona(persona2.personaId)

    val remaining = repository.getAllPersonas()
    assertThat(remaining).hasSize(1)
    assertThat(remaining.first().name).isEqualTo("Keep Me")
  }

  @Test
  fun `deletePersona on missing id is a no-op`() = runTest {
    val persona = DomainTestBuilders.buildPersona(name = "Existing")
    repository.createPersona(persona)

    repository.deletePersona(UUID.randomUUID())

    val all = repository.getAllPersonas()
    assertThat(all).hasSize(1)
  }

  @Test
  fun `getPersonaById returns null for non-existent ID`() = runTest {
    val nonExistentId = UUID.randomUUID()

    val flow = repository.getPersonaById(nonExistentId)
    val result = flow.first()
    assertThat(result).isNull()
  }

  @Test
  fun `observeAllPersonas reflects deletions`() = runTest {
    val persona = DomainTestBuilders.buildPersona()
    repository.createPersona(persona)

    val flow = repository.observeAllPersonas()
    assertThat(flow.first()).hasSize(1)

    repository.deletePersona(persona.personaId)
    assertThat(flow.first()).isEmpty()
  }

  @Test
  fun `createPersona with reserved voice and image style fields`() = runTest {
    val persona =
      DomainTestBuilders.buildPersona(
        defaultVoice = "voice-id-future",
        defaultImageStyle = "realistic",
      )

    repository.createPersona(persona)

    val retrieved = repository.getPersona(persona.personaId)
    assertThat(retrieved?.defaultVoice).isEqualTo("voice-id-future")
    assertThat(retrieved?.defaultImageStyle).isEqualTo("realistic")
  }

  @Test
  fun `updatePersona can change system prompt`() = runTest {
    val persona = DomainTestBuilders.buildPersona(systemPrompt = "Original prompt")
    repository.createPersona(persona)

    val updated = persona.copy(systemPrompt = "New improved prompt")
    repository.updatePersona(updated)

    val retrieved = repository.getPersona(persona.personaId)
    assertThat(retrieved?.systemPrompt).isEqualTo("New improved prompt")
  }

  @Test
  fun `getAllPersonas returns empty list when no personas exist`() = runTest {
    val all = repository.getAllPersonas()
    assertThat(all).isEmpty()
  }

  @Test
  fun `repository should implement PersonaRepository interface`() {
    // Verify that the repository implements the correct interface
    assertThat(repository).isInstanceOf(PersonaRepository::class.java)
  }

  @Test
  fun `repository should be properly constructed`() {
    // Verify that the repository can be constructed with dependencies
    assertThat(repository).isNotNull()
  }
}
