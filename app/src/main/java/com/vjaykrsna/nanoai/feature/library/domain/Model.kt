package com.vjaykrsna.nanoai.feature.library.domain

data class Model(
  val modelId: String,
  val displayName: String,
  val size: Long,
  val parameter: String,
)

fun com.vjaykrsna.nanoai.core.domain.model.ModelPackage.toModel(): Model {
  return Model(
    modelId = this.modelId,
    displayName = this.displayName,
    size = this.sizeBytes,
    parameter = this.architectures.firstOrNull() ?: "N/A",
  )
}
