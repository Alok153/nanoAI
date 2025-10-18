package com.vjaykrsna.nanoai.feature.image.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vjaykrsna.nanoai.feature.image.domain.model.GeneratedImage
import java.util.UUID
import kotlinx.datetime.Instant

/** Room entity for storing generated images with metadata. */
@Entity(tableName = "generated_images")
data class GeneratedImageEntity(
  @PrimaryKey @ColumnInfo(name = "id") val id: String,
  @ColumnInfo(name = "prompt") val prompt: String,
  @ColumnInfo(name = "negative_prompt") val negativePrompt: String,
  @ColumnInfo(name = "width") val width: Int,
  @ColumnInfo(name = "height") val height: Int,
  @ColumnInfo(name = "steps") val steps: Int,
  @ColumnInfo(name = "guidance_scale") val guidanceScale: Float,
  @ColumnInfo(name = "file_path") val filePath: String,
  @ColumnInfo(name = "thumbnail_path") val thumbnailPath: String?,
  @ColumnInfo(name = "created_at") val createdAt: Instant,
)

/** Extension to convert entity to domain model. */
fun GeneratedImageEntity.toDomain(): GeneratedImage =
  GeneratedImage(
    id = UUID.fromString(id),
    prompt = prompt,
    negativePrompt = negativePrompt,
    width = width,
    height = height,
    steps = steps,
    guidanceScale = guidanceScale,
    filePath = filePath,
    thumbnailPath = thumbnailPath,
    createdAt = createdAt,
  )

/** Extension to convert domain model to entity. */
fun GeneratedImage.toEntity(): GeneratedImageEntity =
  GeneratedImageEntity(
    id = id.toString(),
    prompt = prompt,
    negativePrompt = negativePrompt,
    width = width,
    height = height,
    steps = steps,
    guidanceScale = guidanceScale,
    filePath = filePath,
    thumbnailPath = thumbnailPath,
    createdAt = createdAt,
  )
