package com.vjaykrsna.nanoai.feature.uiux.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import com.vjaykrsna.nanoai.feature.uiux.domain.UIUX_DEFAULT_USER_ID
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Periodic worker that synchronizes UI state snapshots with the remote API. */
@HiltWorker
class SyncUiStateWorker
@AssistedInject
constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  private val userProfileRepository: UserProfileRepository,
) : CoroutineWorker(appContext, workerParams) {
  override suspend fun doWork(): Result =
    withContext(Dispatchers.IO) {
      val userId = inputData.getString(KEY_USER_ID) ?: UIUX_DEFAULT_USER_ID
      runCatching {
          val synced = userProfileRepository.syncToRemote(userId)
          if (!synced) {
            return@withContext if (runAttemptCount >= MAX_RETRIES) {
              Result.failure()
            } else {
              Result.retry()
            }
          }
          userProfileRepository.refreshUserProfile(userId, force = true)
          Result.success()
        }
        .getOrElse { throwable ->
          if (runAttemptCount >= MAX_RETRIES) {
            Result.failure()
          } else {
            Result.retry()
          }
        }
    }

  companion object {
    const val WORK_NAME: String = "SyncUiStateWorker"
    const val KEY_USER_ID: String = "user_id"
    const val REPEAT_INTERVAL_HOURS: Long = 6L
    private const val MAX_RETRIES = 3
  }
}
