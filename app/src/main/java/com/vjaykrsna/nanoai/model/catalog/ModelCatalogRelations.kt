package com.vjaykrsna.nanoai.model.catalog

import androidx.room.Embedded
import androidx.room.Relation

/** Container mapping a model package to its cached manifests. */
data class ModelPackageWithManifests(
  @Embedded val model: ModelPackageEntity,
  @Relation(parentColumn = "model_id", entityColumn = "model_id")
  val manifests: List<DownloadManifestEntity>,
)
