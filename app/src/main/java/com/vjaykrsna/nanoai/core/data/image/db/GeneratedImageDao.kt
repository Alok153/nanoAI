package com.vjaykrsna.nanoai.core.data.image.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** DAO for generated images with metadata. */
@Dao
interface GeneratedImageDao {

  @Query("SELECT * FROM generated_images ORDER BY created_at DESC")
  fun observeAll(): Flow<List<GeneratedImageEntity>>

  @Query("SELECT * FROM generated_images WHERE id = :id")
  suspend fun getById(id: String): GeneratedImageEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(image: GeneratedImageEntity)

  @Query("DELETE FROM generated_images WHERE id = :id") suspend fun deleteById(id: String)

  @Query("DELETE FROM generated_images") suspend fun deleteAll()
}
