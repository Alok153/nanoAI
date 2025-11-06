package com.vjaykrsna.nanoai.core.domain.settings

data class ImportSummary(
  val personasImported: Int,
  val personasUpdated: Int,
  val providersImported: Int,
  val providersUpdated: Int,
)
