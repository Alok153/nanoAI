package com.vjaykrsna.nanoai.feature.uiux.domain

import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Toggles compact UI density, updating repositories and notifying observers.
 */
class ToggleCompactModeUseCase
    @Inject
    constructor(
        private val repository: UserProfileRepository,
        private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + dispatcher)

        fun toggle(
            enabled: Boolean,
            userId: String = UIUX_DEFAULT_USER_ID,
        ) {
            scope.launch {
                repository.updateCompactMode(userId, enabled)
                repository.refreshUserProfile(userId, force = true)
            }
        }
    }
