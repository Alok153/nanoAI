package com.vjaykrsna.nanoai.core.data.repository.impl

import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.model.uiux.LayoutSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UiPreferencesSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
import com.vjaykrsna.nanoai.feature.uiux.data.UserProfileLocalDataSource
import com.vjaykrsna.nanoai.feature.uiux.data.UserProfileRemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first implementation of [UserProfileRepository].
 *
 * Observes local DB/DataStore state and attempts to refresh from remote when appropriate.
 */
@Singleton
class UserProfileRepositoryImpl
    @Inject
    constructor(
        private val local: UserProfileLocalDataSource,
        private val remote: UserProfileRemoteDataSource,
    ) : UserProfileRepository {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        override fun observeUserProfile(userId: String): Flow<UserProfile?> =
            local.observeUserProfile(userId).also { flow ->
                // Trigger a background refresh but don't block emission
                scope.launchWhenStarted {
                    try {
                        remote.fetchUserProfile().getOrNull()?.let { fetched ->
                            // Merge remote with local by saving
                            local.saveUserProfile(fetched)
                        }
                    } catch (_: Exception) {
                        // Best-effort: ignore remote failures here, local data remains authoritative
                    }
                }
            }

        override suspend fun getUserProfile(userId: String): UserProfile? =
            withContext(Dispatchers.IO) {
                // Try local first
                val localProfile = local.getUserProfile(userId)
                if (localProfile != null) return@withContext localProfile

                // If no local profile, try remote and persist
                val result = remote.fetchUserProfile()
                val fetched = result.getOrNull()
                if (fetched != null) {
                    local.saveUserProfile(fetched)
                }
                fetched
            }

        override fun observePreferences(): Flow<UiPreferencesSnapshot> =
            // Map UiPreferencesStore flow to domain snapshot via local helper
            local.uiPreferencesStore.uiPreferences
                .map { prefs -> prefs.toDomainSnapshot() }
                .shareIn(scope, SharingStarted.Eagerly, replay = 1)

        override suspend fun updateThemePreference(
            userId: String,
            themePreferenceName: String,
        ) {
            withContext(Dispatchers.IO) {
                // Map string to enum in local layer via ThemePreference.fromName if needed
                val theme =
                    try {
                        com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
                            .fromName(themePreferenceName)
                    } catch (e: Exception) {
                        com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference.SYSTEM
                    }
                local.updateThemePreference(userId, theme)
            }
        }

        override suspend fun updateVisualDensity(
            userId: String,
            visualDensityName: String,
        ) {
            withContext(Dispatchers.IO) {
                val density =
                    com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
                        .values()
                        .firstOrNull { it.name.equals(visualDensityName, ignoreCase = true) }
                        ?: com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity.DEFAULT
                local.updateVisualDensity(userId, density)
            }
        }

        override suspend fun recordOnboardingProgress(
            userId: String,
            dismissedTips: Map<String, Boolean>,
            completed: Boolean,
        ) {
            withContext(Dispatchers.IO) {
                local.updateOnboardingCompleted(userId, completed)
                // Update dismissed tips in DB and also DataStore via local
                local.dismissTip(userId, "__bulk_update__") // noop path: replace with direct call below
                // Direct update of dismissed tips in DB
                val profile = local.userProfileDao.getById(userId)
                if (profile != null) {
                    val updated = profile.dismissedTips.toMutableMap()
                    updated.putAll(dismissedTips)
                    local.userProfileDao.updateDismissedTips(userId, updated)
                }
                // Also persist to DataStore
                local.uiPreferencesStore.setDismissedTips(dismissedTips)
            }
        }

        override suspend fun updatePinnedTools(
            userId: String,
            pinnedTools: List<String>,
        ) {
            withContext(Dispatchers.IO) {
                // Enforce max 10
                val trimmed = pinnedTools.take(com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile.MAX_PINNED_TOOLS)
                local.updatePinnedTools(userId, trimmed)
            }
        }

        override suspend fun saveLayoutSnapshot(
            userId: String,
            layout: LayoutSnapshot,
            position: Int,
        ) {
            withContext(Dispatchers.IO) {
                local.saveLayoutSnapshot(userId, layout, position)
            }
        }

        override suspend fun deleteLayoutSnapshot(layoutId: String) {
            withContext(Dispatchers.IO) {
                local.deleteLayoutSnapshot(layoutId)
            }
        }

        override fun observeUIStateSnapshot(userId: String): Flow<UIStateSnapshot?> = local.observeUIStateSnapshot(userId)

        override suspend fun syncToRemote(userId: String): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    val profile = local.getUserProfile(userId) ?: return@withContext false
                    val updateResult = remote.updateUserProfile(profile)
                    updateResult.isSuccess
                } catch (e: Exception) {
                    false
                }
            }
    }
