package com.vjaykrsna.nanoai.feature.uiux.domain

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Records onboarding step completions and dismissed contextual tips.
 */
class RecordOnboardingProgressUseCase
    @Inject
    constructor(
        private val repository: UserProfileRepository,
        @IoDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + dispatcher)
        private val dismissedTips = mutableMapOf<String, Boolean>()

        fun recordDismissal(
            tipId: String?,
            dismissed: Boolean,
            completed: Boolean,
            userId: String = UIUX_DEFAULT_USER_ID,
        ) {
            if (!tipId.isNullOrBlank()) {
                if (dismissed) {
                    dismissedTips[tipId] = true
                } else {
                    dismissedTips.remove(tipId)
                }
            }

            val snapshot = dismissedTips.toMap()

            scope.launch {
                repository.recordOnboardingProgress(userId, snapshot, completed)
                if (completed) {
                    repository.refreshUserProfile(userId, force = true)
                }
            }
        }

        fun recordDismissal(
            tipId: String?,
            dismissed: Boolean,
            completed: Boolean,
        ) {
            recordDismissal(tipId, dismissed, completed, UIUX_DEFAULT_USER_ID)
        }
    }
