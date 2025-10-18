package com.vjaykrsna.nanoai.feature.image.domain.model

import java.util.UUID
import kotlinx.datetime.Instant

/**
 * Domain model for a generated image with metadata.
 *
 * Stores prompt, generation parameters, timestamp, and file path for gallery display.
 */
data class GeneratedImage(
  val id: UUID,
  val prompt: String,
  val negativePrompt: String,
  val width: Int,
  val height: Int,
  val steps: Int,
  val guidanceScale: Float,
  val filePath: String,
  val thumbnailPath: String? = null,
  val createdAt: Instant,
)
