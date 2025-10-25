package com.vjaykrsna.nanoai.feature.uiux.data

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.repository.ProgressRepository
import com.vjaykrsna.nanoai.feature.uiux.presentation.JobStatus
import com.vjaykrsna.nanoai.feature.uiux.presentation.JobType
import com.vjaykrsna.nanoai.feature.uiux.presentation.ProgressJob
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ProgressRepositoryImplTest {

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension()

  private lateinit var repository: ProgressRepositoryImpl

  @BeforeEach
  fun setUp() {
    repository = ProgressRepositoryImpl(mainDispatcherExtension.dispatcher)
  }

  private fun createTestJob() =
    ProgressJob(
      jobId = UUID.randomUUID(),
      type = JobType.OTHER,
      status = JobStatus.PENDING,
      progress = 0f,
      queuedAt = Instant.now(),
    )

  @Test
  fun `queueJob should add a new job to the list`() = runTest {
    // Given
    val job = createTestJob()

    // When
    repository.queueJob(job)
    val jobs = repository.progressJobs.first()

    // Then
    assertThat(jobs).hasSize(1)
    assertThat(jobs[0].jobId).isEqualTo(job.jobId)
  }

  @Test
  fun `completeJob should remove a job from the list`() = runTest {
    // Given
    val job = createTestJob()
    repository.queueJob(job)

    // When
    repository.completeJob(job.jobId)
    val jobs = repository.progressJobs.first()

    // Then
    assertThat(jobs).isEmpty()
  }

  @Test
  fun `repository should implement ProgressRepository interface`() {
    // Verify that the repository implements the correct interface
    assertThat(repository).isInstanceOf(ProgressRepository::class.java)
  }

  @Test
  fun `repository should have ioDispatcher property`() {
    // Verify that the repository has the ioDispatcher property from BaseRepository
    // This ensures the repository follows the BaseRepository pattern
    assertThat(repository.ioDispatcher).isNotNull()
    assertThat(repository.ioDispatcher).isEqualTo(mainDispatcherExtension.dispatcher)
  }
}
