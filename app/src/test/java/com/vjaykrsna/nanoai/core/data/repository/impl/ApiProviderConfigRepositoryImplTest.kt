package com.vjaykrsna.nanoai.core.data.repository.impl

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.daos.ApiProviderConfigDao
import com.vjaykrsna.nanoai.core.data.db.entities.ApiProviderConfigEntity
import com.vjaykrsna.nanoai.core.data.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.domain.model.ApiProviderConfig
import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MainDispatcherExtension::class)
class ApiProviderConfigRepositoryImplTest {

  private lateinit var apiProviderConfigDao: ApiProviderConfigDao
  private lateinit var repository: ApiProviderConfigRepositoryImpl
  private val testDispatcher = MainDispatcherExtension().dispatcher

  @BeforeEach
  fun setUp() {
    apiProviderConfigDao = mockk(relaxed = true)
    repository =
      ApiProviderConfigRepositoryImpl(
        apiProviderConfigDao = apiProviderConfigDao,
        ioDispatcher = testDispatcher,
      )
  }

  private fun createTestConfig(id: String) =
    ApiProviderConfig(
      providerId = id,
      providerName = "Test Provider",
      baseUrl = "https://test.com",
      apiKey = "test_key",
      apiType = APIType.OPENAI_COMPATIBLE,
      isEnabled = true,
    )

  private fun createTestEntity(id: String) =
    ApiProviderConfigEntity(
      providerId = id,
      providerName = "Test Provider",
      baseUrl = "https://test.com",
      apiKey = "test_key",
      apiType = APIType.OPENAI_COMPATIBLE,
      isEnabled = true,
    )

  @Test
  fun `getAllProviders should fetch and map providers from dao`() = runTest {
    // Given
    val entities = listOf(createTestEntity("1"), createTestEntity("2"))
    coEvery { apiProviderConfigDao.getAll() } returns entities

    // When
    val result = repository.getAllProviders()

    // Then
    assertThat(result).hasSize(2)
    assertThat(result[0].providerId).isEqualTo("1")
  }

  @Test
  fun `getProvider should fetch and map a single provider`() = runTest {
    // Given
    val entity = createTestEntity("1")
    coEvery { apiProviderConfigDao.getById("1") } returns entity

    // When
    val result = repository.getProvider("1")

    // Then
    assertThat(result).isNotNull()
    assertThat(result?.providerId).isEqualTo("1")
  }

  @Test
  fun `addProvider should call dao to insert provider`() = runTest {
    // Given
    val config = createTestConfig("1")

    // When
    repository.addProvider(config)

    // Then
    coVerify { apiProviderConfigDao.insert(any()) }
  }

  @Test
  fun `updateProvider should call dao to update provider`() = runTest {
    // Given
    val config = createTestConfig("1")

    // When
    repository.updateProvider(config)

    // Then
    coVerify { apiProviderConfigDao.update(any()) }
  }

  @Test
  fun `deleteProvider should call dao to delete provider`() = runTest {
    // Given
    val entity = createTestEntity("1")
    coEvery { apiProviderConfigDao.getById("1") } returns entity

    // When
    repository.deleteProvider("1")

    // Then
    coVerify { apiProviderConfigDao.delete(entity) }
  }

  @Test
  fun `getEnabledProviders should fetch and map enabled providers`() = runTest {
    // Given
    val entities = listOf(createTestEntity("1").copy(isEnabled = true))
    coEvery { apiProviderConfigDao.getEnabled() } returns entities

    // When
    val result = repository.getEnabledProviders()

    // Then
    assertThat(result).hasSize(1)
    assertThat(result[0].isEnabled).isTrue()
  }

  @Test
  fun `observeAllProviders should return a flow of mapped providers`() = runTest {
    // Given
    val entities = listOf(createTestEntity("1"), createTestEntity("2"))
    coEvery { apiProviderConfigDao.observeAll() } returns flowOf(entities)

    // When
    val result = repository.observeAllProviders().first()

    // Then
    assertThat(result).hasSize(2)
    assertThat(result[0].providerId).isEqualTo("1")
  }

  @Test
  fun `repository should implement ApiProviderConfigRepository interface`() {
    // Verify that the repository implements the correct interface
    assertThat(repository).isInstanceOf(ApiProviderConfigRepository::class.java)
  }

  @Test
  fun `repository should be properly constructed`() {
    // Verify that the repository can be constructed with dependencies
    assertThat(repository).isNotNull()
  }
}
