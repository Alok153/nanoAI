package com.vjaykrsna.nanoai.core.data.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vjaykrsna.nanoai.core.data.db.entities.PersonaProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for PersonaProfile entities.
 *
 * Provides methods to manage AI assistant persona configurations.
 */
@Dao
interface PersonaProfileDao {
    /**
     * Insert a new persona profile. Replaces if persona with same ID exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(persona: PersonaProfileEntity)

    /**
     * Insert multiple personas (for seeding defaults).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(personas: List<PersonaProfileEntity>)

    /**
     * Update an existing persona profile.
     */
    @Update
    suspend fun update(persona: PersonaProfileEntity)

    /**
     * Delete a persona profile.
     */
    @Delete
    suspend fun delete(persona: PersonaProfileEntity)

    /**
     * Get a specific persona by ID.
     */
    @Query("SELECT * FROM persona_profiles WHERE persona_id = :personaId")
    suspend fun getById(personaId: String): PersonaProfileEntity?

    /**
     * Observe a specific persona by ID (reactive).
     */
    @Query("SELECT * FROM persona_profiles WHERE persona_id = :personaId")
    fun observeById(personaId: String): Flow<PersonaProfileEntity?>

    /**
     * Get all personas ordered by creation date.
     */
    @Query("SELECT * FROM persona_profiles ORDER BY created_at ASC")
    suspend fun getAll(): List<PersonaProfileEntity>

    /**
     * Observe all personas (reactive).
     */
    @Query("SELECT * FROM persona_profiles ORDER BY created_at ASC")
    fun observeAll(): Flow<List<PersonaProfileEntity>>

    /**
     * Search personas by name (case-insensitive).
     */
    @Query("SELECT * FROM persona_profiles WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchByName(query: String): List<PersonaProfileEntity>

    /**
     * Get personas that prefer a specific model.
     */
    @Query("SELECT * FROM persona_profiles WHERE default_model_preference = :modelId")
    suspend fun getByModelPreference(modelId: String): List<PersonaProfileEntity>

    /**
     * Delete all personas (for testing/debugging).
     */
    @Query("DELETE FROM persona_profiles")
    suspend fun deleteAll()

    /**
     * Count total personas.
     */
    @Query("SELECT COUNT(*) FROM persona_profiles")
    suspend fun count(): Int

    /**
     * Get recently updated personas (for quick access).
     */
    @Query("SELECT * FROM persona_profiles ORDER BY updated_at DESC LIMIT :limit")
    suspend fun getRecentlyUpdated(limit: Int = 5): List<PersonaProfileEntity>
}
