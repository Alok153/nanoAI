package com.vjaykrsna.nanoai.core.data.repository

import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import java.util.UUID
import kotlinx.coroutines.flow.Flow

interface ProgressRepository : BaseRepository {
  val progressJobs: Flow<List<ProgressJob>>

  suspend fun queueJob(job: ProgressJob)

  suspend fun completeJob(jobId: UUID)
}
