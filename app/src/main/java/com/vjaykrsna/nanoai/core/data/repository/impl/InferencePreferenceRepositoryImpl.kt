package com.vjaykrsna.nanoai.core.data.repository.impl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vjaykrsna.nanoai.core.data.repository.InferencePreferenceRepository
import com.vjaykrsna.nanoai.core.domain.model.InferencePreference
import com.vjaykrsna.nanoai.core.model.InferenceMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class InferencePreferenceRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : InferencePreferenceRepository {
        override fun observeInferencePreference(): Flow<InferencePreference> =
            context.dataStore.data.map { preferences ->
                val modeName = preferences[KEY_INFERENCE_MODE]
                val mode = modeName?.let { runCatching { InferenceMode.valueOf(it) }.getOrNull() } ?: InferenceMode.LOCAL_FIRST
                InferencePreference(mode = mode)
            }

        override suspend fun setInferenceMode(mode: InferenceMode) {
            context.dataStore.edit { preferences ->
                preferences[KEY_INFERENCE_MODE] = mode.name
            }
        }

        private companion object {
            private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "inference_preferences")
            private val KEY_INFERENCE_MODE = stringPreferencesKey("inference_mode")
        }
    }
