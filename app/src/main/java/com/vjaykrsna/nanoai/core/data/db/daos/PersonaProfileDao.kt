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
interface PersonaProfileDao :
  PersonaProfileWriteDao, PersonaProfileReadDao, PersonaProfileMaintenanceDao

/** Write helpers for persona profiles. */
interface PersonaProfileWriteDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(persona: PersonaProfileEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(personas: List<PersonaProfileEntity>)

  @Update suspend fun update(persona: PersonaProfileEntity)

  @Delete suspend fun delete(persona: PersonaProfileEntity)
}

/** Query helpers for persona profiles. */
interface PersonaProfileReadDao {
  @Query("SELECT * FROM persona_profiles WHERE persona_id = :personaId")
  suspend fun getById(personaId: String): PersonaProfileEntity?

  @Query("SELECT * FROM persona_profiles WHERE persona_id = :personaId")
  fun observeById(personaId: String): Flow<PersonaProfileEntity?>

  @Query("SELECT * FROM persona_profiles ORDER BY created_at ASC")
  suspend fun getAll(): List<PersonaProfileEntity>

  @Query("SELECT * FROM persona_profiles ORDER BY created_at ASC")
  fun observeAll(): Flow<List<PersonaProfileEntity>>

  @Query("SELECT * FROM persona_profiles WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
  suspend fun searchByName(query: String): List<PersonaProfileEntity>

  @Query("SELECT * FROM persona_profiles WHERE default_model_preference = :modelId")
  suspend fun getByModelPreference(modelId: String): List<PersonaProfileEntity>

  @Query("SELECT * FROM persona_profiles ORDER BY updated_at DESC LIMIT :limit")
  suspend fun getRecentlyUpdated(limit: Int = 5): List<PersonaProfileEntity>
}

/** Maintenance helpers for persona profiles. */
interface PersonaProfileMaintenanceDao {
  @Query("DELETE FROM persona_profiles") suspend fun deleteAll()

  @Query("SELECT COUNT(*) FROM persona_profiles") suspend fun count(): Int
}
