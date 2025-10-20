package com.vjaykrsna.nanoai.feature.library.data.huggingface.cache

import com.vjaykrsna.nanoai.feature.library.data.huggingface.entities.HuggingFaceModelCacheEntity
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceModelSummary
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/** Maps cached Hugging Face models between Room entities and domain models. */
internal object HuggingFaceModelCacheMapper {
  fun toEntity(
    model: HuggingFaceModelSummary,
    fetchedAt: Instant = Clock.System.now(),
  ): HuggingFaceModelCacheEntity {
    return HuggingFaceModelCacheEntity(
      modelId = model.modelId,
      displayName = model.displayName,
      author = model.author,
      pipelineTag = model.pipelineTag,
      libraryName = model.libraryName,
      tags = model.tags,
      likes = model.likes,
      downloads = model.downloads,
      license = model.license,
      languages = model.languages,
      baseModel = model.baseModel,
      datasets = model.datasets,
      architectures = model.architectures,
      modelType = model.modelType,
      baseModelRelations = model.baseModelRelations,
      hasGatedAccess = model.hasGatedAccess,
      isDisabled = model.isDisabled,
      totalSizeBytes = model.totalSizeBytes,
      summary = model.summary,
      description = model.description,
      trendingScore = model.trendingScore,
      createdAt = model.createdAt,
      lastModified = model.lastModified,
      isPrivate = model.isPrivate,
      fetchedAt = fetchedAt,
    )
  }

  fun toDomain(entity: HuggingFaceModelCacheEntity): HuggingFaceModelSummary {
    return HuggingFaceModelSummary(
      modelId = entity.modelId,
      displayName = entity.displayName,
      author = entity.author,
      pipelineTag = entity.pipelineTag,
      libraryName = entity.libraryName,
      tags = entity.tags,
      likes = entity.likes,
      downloads = entity.downloads,
      license = entity.license,
      languages = entity.languages,
      baseModel = entity.baseModel,
      datasets = entity.datasets,
      architectures = entity.architectures,
      modelType = entity.modelType,
      baseModelRelations = entity.baseModelRelations,
      hasGatedAccess = entity.hasGatedAccess,
      isDisabled = entity.isDisabled,
      totalSizeBytes = entity.totalSizeBytes,
      summary = entity.summary,
      description = entity.description,
      trendingScore = entity.trendingScore,
      createdAt = entity.createdAt,
      lastModified = entity.lastModified,
      isPrivate = entity.isPrivate,
    )
  }
}
