package com.vjaykrsna.nanoai.core.data.repository.impl

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.model.uiux.LayoutSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UiPreferencesSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.feature.uiux.data.UserProfileLocalDataSource
import com.vjaykrsna.nanoai.feature.uiux.data.UserProfileRemoteDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepositoryImpl
    @Inject
    constructor(
        private val local: UserProfileLocalDataSource,
        private val remote: UserProfileRemoteDataSource,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : UserProfileRepository {
        private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
        private val offlineState = MutableStateFlow(false)

        override fun observeUserProfile(userId: String): Flow<UserProfile?> =
            local
                .observeUserProfile(userId)
                .onStart { scope.launch { refreshUserProfile(userId) } }

        override fun observeOfflineStatus(): Flow<Boolean> = offlineState.asStateFlow()

        override suspend fun setOfflineOverride(isOffline: Boolean) {
            withContext(ioDispatcher) { offlineState.emit(isOffline) }
        }

        override suspend fun getUserProfile(userId: String): UserProfile? = withContext(ioDispatcher) { local.getUserProfile(userId) }

        override fun observePreferences(): Flow<UiPreferencesSnapshot> = local.observePreferences()

        override suspend fun updateThemePreference(
            userId: String,
            themePreferenceName: String,
        ) {
            val theme = ThemePreference.fromName(themePreferenceName)
            withContext(ioDispatcher) { local.updateThemePreference(userId, theme) }
        }

        override suspend fun updateVisualDensity(
            userId: String,
            visualDensityName: String,
        ) {
            val density =
                VisualDensity
                    .values()
                    .firstOrNull { it.name.equals(visualDensityName, ignoreCase = true) }
                    ?: VisualDensity.DEFAULT
            withContext(ioDispatcher) { local.updateVisualDensity(userId, density) }
        }

        override suspend fun updateCompactMode(
            userId: String,
            enabled: Boolean,
        ) {
            withContext(ioDispatcher) { local.updateCompactMode(userId, enabled) }
        }

        override suspend fun recordOnboardingProgress(
            userId: String,
            dismissedTips: Map<String, Boolean>,
            completed: Boolean,
        ) {
            withContext(ioDispatcher) {
                local.updateOnboardingCompleted(userId, completed)
                local.setDismissedTips(userId, dismissedTips)
            }
        }

        override suspend fun updatePinnedTools(
            userId: String,
            pinnedTools: List<String>,
        ) {
            withContext(ioDispatcher) { local.updatePinnedTools(userId, pinnedTools) }
        }

        override suspend fun saveLayoutSnapshot(
            userId: String,
            layout: LayoutSnapshot,
            position: Int,
        ) {
            withContext(ioDispatcher) { local.saveLayoutSnapshot(userId, layout, position) }
        }

        override suspend fun deleteLayoutSnapshot(layoutId: String) {
            withContext(ioDispatcher) { local.deleteLayoutSnapshot(layoutId) }
        }

        override fun observeUIStateSnapshot(userId: String): Flow<UIStateSnapshot?> = local.observeUIStateSnapshot(userId)

        override suspend fun syncToRemote(userId: String): Boolean =
            withContext(ioDispatcher) {
                runCatching {
                    val profile = local.getUserProfile(userId) ?: return@withContext false
                    remote.updateUserProfile(profile).isSuccess
                }.getOrElse { false }
            }

        override suspend fun refreshUserProfile(
            userId: String,
            force: Boolean,
        ): Boolean =
            withContext(ioDispatcher) {
                runCatching {
                    val result = remote.fetchUserProfile()
                    val profile = result.getOrNull()
                    if (profile != null) {
                        local.saveUserProfile(profile)
                        true
                    } else {
                        false
                    }
                }.getOrElse { false }
            }
    }
