package com.vjaykrsna.nanoai.feature.library.domain

/** Parameters controlling Hugging Face Hub listing queries. */
data class HuggingFaceCatalogQuery(
  val search: String? = null,
  val sortField: HuggingFaceSortField? = null,
  val sortDirection: HuggingFaceSortDirection? = null,
  val pipelineTag: String? = null,
  val library: String? = null,
  val includePrivate: Boolean = false,
  val limit: Int = DEFAULT_LIMIT,
  val offset: Int = 0,
)

/** Supported sort fields for Hugging Face listings. */
enum class HuggingFaceSortField(val apiValue: String) {
  DOWNLOADS("downloads"),
  LIKES("likes"),
  LAST_MODIFIED("last_modified"),
  CREATED("created_at"),
  TRENDING_SCORE("trending_score"),
}

/** Sort direction wrapper ensuring we only send valid values to the API. */
enum class HuggingFaceSortDirection(val apiValue: Int) {
  ASCENDING(1),
  DESCENDING(-1),
}

private const val DEFAULT_LIMIT = 50
