package com.vjaykrsna.nanoai.core.data.library.huggingface.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/** Cached representation of a Hugging Face model listing. */
@Entity(tableName = "huggingface_models")
data class HuggingFaceModelCacheEntity(
  @PrimaryKey @ColumnInfo(name = "model_id") val modelId: String,
  @ColumnInfo(name = "display_name") val displayName: String,
  @ColumnInfo(name = "author") val author: String?,
  @ColumnInfo(name = "pipeline_tag") val pipelineTag: String?,
  @ColumnInfo(name = "library_name") val libraryName: String?,
  @ColumnInfo(name = "tags") val tags: List<String>,
  @ColumnInfo(name = "likes") val likes: Long,
  @ColumnInfo(name = "downloads") val downloads: Long,
  @ColumnInfo(name = "license") val license: String?,
  @ColumnInfo(name = "languages") val languages: List<String>,
  @ColumnInfo(name = "base_model") val baseModel: String?,
  @ColumnInfo(name = "datasets") val datasets: List<String>,
  @ColumnInfo(name = "architectures") val architectures: List<String>,
  @ColumnInfo(name = "model_type") val modelType: String?,
  @ColumnInfo(name = "base_relations") val baseModelRelations: List<String>,
  @ColumnInfo(name = "gated") val hasGatedAccess: Boolean,
  @ColumnInfo(name = "disabled") val isDisabled: Boolean,
  @ColumnInfo(name = "total_size_bytes") val totalSizeBytes: Long?,
  @ColumnInfo(name = "summary") val summary: String?,
  @ColumnInfo(name = "description") val description: String?,
  @ColumnInfo(name = "trending_score") val trendingScore: Long?,
  @ColumnInfo(name = "created_at") val createdAt: Instant?,
  @ColumnInfo(name = "last_modified") val lastModified: Instant?,
  @ColumnInfo(name = "private") val isPrivate: Boolean,
  @ColumnInfo(name = "fetched_at") val fetchedAt: Instant,
)
