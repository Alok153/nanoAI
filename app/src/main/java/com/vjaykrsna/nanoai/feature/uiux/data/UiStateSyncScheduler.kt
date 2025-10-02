package com.vjaykrsna.nanoai.feature.uiux.data

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.vjaykrsna.nanoai.feature.uiux.domain.UIUX_DEFAULT_USER_ID
import javax.inject.Inject
import javax.inject.Singleton

/** Schedules background sync work for UI/UX state. */
@Singleton
class UiStateSyncScheduler
    @Inject
    constructor(
        private val workManager: WorkManager,
    ) {
        fun ensurePeriodicSync(request: PeriodicWorkRequest) {
            workManager.enqueueUniquePeriodicWork(
                SyncUiStateWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun enqueueImmediateSync(userId: String = UIUX_DEFAULT_USER_ID) {
            val workRequest =
                OneTimeWorkRequestBuilder<SyncUiStateWorker>()
                    .setInputData(
                        workDataOf(SyncUiStateWorker.KEY_USER_ID to userId),
                    ).build()
            workManager.enqueue(workRequest)
        }
    }
