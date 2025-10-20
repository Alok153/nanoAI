package com.vjaykrsna.nanoai.feature.library.data.huggingface.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vjaykrsna.nanoai.feature.library.data.huggingface.entities.HuggingFaceModelCacheEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/** DAO for persisting Hugging Face model metadata. */
@Dao
interface HuggingFaceModelCacheDao {
  @Query("SELECT * FROM huggingface_models ORDER BY downloads DESC LIMIT :limit OFFSET :offset")
  fun observeModels(limit: Int, offset: Int): Flow<List<HuggingFaceModelCacheEntity>>

  @Query("SELECT * FROM huggingface_models ORDER BY downloads DESC LIMIT :limit OFFSET :offset")
  suspend fun getModels(limit: Int, offset: Int): List<HuggingFaceModelCacheEntity>

  @Query(
    "SELECT * FROM huggingface_models WHERE fetched_at > :expiryThreshold ORDER BY downloads DESC LIMIT :limit OFFSET :offset"
  )
  suspend fun getFreshModels(
    expiryThreshold: Instant,
    limit: Int,
    offset: Int,
  ): List<HuggingFaceModelCacheEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(models: List<HuggingFaceModelCacheEntity>)

  @Query("DELETE FROM huggingface_models WHERE fetched_at <= :expiryThreshold")
  suspend fun deleteOlderThan(expiryThreshold: Instant)

  @Query("DELETE FROM huggingface_models") suspend fun clear()

  @Transaction
  suspend fun replaceAll(models: List<HuggingFaceModelCacheEntity>) {
    clear()
    if (models.isNotEmpty()) {
      upsertAll(models)
    }
  }
}
