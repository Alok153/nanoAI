package com.vjaykrsna.nanoai.feature.library.data.huggingface

import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceCatalogQuery
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.model.huggingface.network.HuggingFaceService
import com.vjaykrsna.nanoai.model.huggingface.network.dto.HuggingFaceModelListingDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Instant

/** Retrofit-backed implementation for [HuggingFaceCatalogRepository]. */
@Singleton
class HuggingFaceCatalogRepositoryImpl
@Inject
constructor(
  private val service: HuggingFaceService,
) : HuggingFaceCatalogRepository {
  override suspend fun listModels(
    query: HuggingFaceCatalogQuery,
  ): Result<List<HuggingFaceModelSummary>> = runCatching {
    service
      .listModels(
        search = query.search?.takeIf { it.isNotBlank() },
        sort = query.sortField?.apiValue,
        direction = query.sortDirection?.apiValue?.takeIf { query.sortField != null },
        limit = query.limit,
        pipelineTag = query.pipelineTag?.takeIf { it.isNotBlank() },
        library = query.library?.takeIf { it.isNotBlank() },
        includePrivate = query.includePrivate.takeIf { it },
        expand = DEFAULT_EXPANSIONS,
      )
      .map { it.toDomain() }
  }

  private fun HuggingFaceModelListingDto.toDomain(): HuggingFaceModelSummary {
    val resolvedId = modelId ?: id ?: error("Hugging Face model missing identifier")
    return HuggingFaceModelSummary(
      modelId = resolvedId,
      displayName = resolvedId,
      author = author?.takeIf { it.isNotBlank() },
      pipelineTag = pipelineTag,
      libraryName = libraryName?.takeIf { it.isNotBlank() },
      tags = tags.mapNotNull { it?.trim() }.filter { it.isNotBlank() },
      likes = likes ?: 0L,
      downloads = downloads ?: 0L,
      trendingScore = trendingScore,
      createdAt = createdAt?.let(Instant::parse),
      lastModified = lastModified?.let(Instant::parse),
      isPrivate = isPrivate ?: false,
    )
  }

  private companion object {
    val DEFAULT_EXPANSIONS =
      listOf(
        "author",
        "downloads",
        "likes",
        "pipeline_tag",
        "tags",
        "library_name",
        "createdAt",
        "lastModified",
        "trendingScore",
        "private",
      )
  }
}
