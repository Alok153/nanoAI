package com.vjaykrsna.nanoai.feature.uiux.data

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.data.repository.ProgressRepository
import com.vjaykrsna.nanoai.feature.uiux.presentation.ProgressJob
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class ProgressRepositoryImpl
@Inject
constructor(@IoDispatcher override val ioDispatcher: CoroutineDispatcher) : ProgressRepository {

  private val _progressJobs = MutableStateFlow<List<ProgressJob>>(emptyList())

  override val progressJobs: Flow<List<ProgressJob>> = _progressJobs.asStateFlow()

  override suspend fun queueJob(job: ProgressJob) {
    _progressJobs.value = (_progressJobs.value + job).sortedBy(ProgressJob::queuedAt)
  }

  override suspend fun completeJob(jobId: UUID) {
    _progressJobs.value = _progressJobs.value.filterNot { it.jobId == jobId }
  }
}
