package com.vjaykrsna.nanoai.core.domain.library

import kotlinx.datetime.Instant

/** Snapshot describing the most recent catalog refresh outcomes. */
data class ModelCatalogRefreshStatus(
  val lastSuccessAt: Instant? = null,
  val lastSuccessSource: String? = null,
  val lastSuccessCount: Int = 0,
  val lastFallbackAt: Instant? = null,
  val lastFallbackReason: String? = null,
  val lastFallbackCachedCount: Int = 0,
  val lastFallbackMessage: String? = null,
)
