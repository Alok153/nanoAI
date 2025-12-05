package com.vjaykrsna.nanoai.core.data.uiux

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.DataStoreUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ProgressJob
import com.vjaykrsna.nanoai.core.domain.repository.ConnectivityRepository
import com.vjaykrsna.nanoai.core.domain.repository.ProgressRepository
import com.vjaykrsna.nanoai.core.domain.repository.UserProfileRepository
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ConnectivityRepositoryImplTest {

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension()

  private lateinit var userProfileRepository: UserProfileRepository
  private lateinit var progressRepository: TestProgressRepository
  private lateinit var repository: ConnectivityRepositoryImpl

  @BeforeEach
  fun setUp() {
    userProfileRepository = mockk(relaxed = true)
    coEvery { userProfileRepository.observePreferences() } returns flowOf(DataStoreUiPreferences())
    progressRepository = TestProgressRepository(mainDispatcherExtension.dispatcher)

    repository =
      ConnectivityRepositoryImpl(
        userProfileRepository = userProfileRepository,
        progressRepository = progressRepository,
        ioDispatcher = mainDispatcherExtension.dispatcher,
      )
  }

  @Test
  fun `updateConnectivity should update the connectivity status`() = runTest {
    // Given
    val status = ConnectivityStatus.OFFLINE

    // When
    repository.updateConnectivity(status)
    advanceUntilIdle()
    val bannerState = repository.connectivityBannerState.first()

    // Then
    assertThat(bannerState.status).isEqualTo(status)
    coVerify { userProfileRepository.setOfflineOverride(true) }
  }

  @Test
  fun `connectivityBannerState should show CTA when offline`() = runTest {
    // Given
    val status = ConnectivityStatus.OFFLINE

    // When
    repository.updateConnectivity(status)
    advanceUntilIdle()
    val bannerState = repository.connectivityBannerState.first()

    // Then
    assertThat(bannerState.cta).isNotNull()
  }

  @Test
  fun `connectivityBannerState should not show CTA when online`() = runTest {
    // Given
    val status = ConnectivityStatus.ONLINE

    // When
    repository.updateConnectivity(status)
    advanceUntilIdle()
    val bannerState = repository.connectivityBannerState.first()

    // Then
    assertThat(bannerState.cta).isNull()
  }

  @Test
  fun `connectivityBannerState reflects queued job count`() = runTest {
    val job =
      ProgressJob(
        jobId = UUID.randomUUID(),
        type = JobType.MODEL_DOWNLOAD,
        status = JobStatus.PENDING,
        progress = 0f,
        queuedAt = Instant.now(),
      )

    progressRepository.queueJob(job)
    advanceUntilIdle()

    val bannerState = repository.connectivityBannerState.first()

    assertThat(bannerState.queuedActionCount).isEqualTo(1)
  }

  @Test
  fun `repository should implement ConnectivityRepository interface`() {
    // Then
    assertThat(repository).isInstanceOf(ConnectivityRepository::class.java)
  }

  @Test
  fun `repository should be properly constructed`() {
    // Verify that the repository can be constructed with dependencies
    assertThat(repository).isNotNull()
  }
}

private class TestProgressRepository(override val ioDispatcher: CoroutineDispatcher) :
  ProgressRepository {
  private val _jobs = MutableStateFlow<List<ProgressJob>>(emptyList())
  override val progressJobs = _jobs

  override suspend fun queueJob(job: ProgressJob) {
    _jobs.value = _jobs.value + job
  }

  override suspend fun completeJob(jobId: UUID) {
    _jobs.value = _jobs.value.filterNot { it.jobId == jobId }
  }
}
