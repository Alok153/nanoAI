package com.vjaykrsna.nanoai.feature.library.presentation.model

import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceCatalogQuery

internal fun HuggingFaceFilterState.toQuery(): HuggingFaceCatalogQuery {
  val normalizedSearch = searchQuery.trim().takeIf { it.isNotEmpty() }
  val sortField = sort.sortField
  val sortDirection = sort.direction?.takeIf { sortField != null }
  return HuggingFaceCatalogQuery(
    search = normalizedSearch,
    sortField = sortField,
    sortDirection = sortDirection,
    pipelineTag = pipelineTag,
    library = library,
    includePrivate = includePrivate,
    offset = offset,
  )
}
