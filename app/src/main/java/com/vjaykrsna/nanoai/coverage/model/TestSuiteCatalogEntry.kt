package com.vjaykrsna.nanoai.coverage.model

/** Metadata describing an automated suite and the coverage gaps it mitigates. */
data class TestSuiteCatalogEntry(
  val suiteId: String,
  val owner: String,
  val layer: TestLayer,
  val journey: String,
  val coverageContribution: Double,
  val riskTags: Set<String>,
) {
  init {
    require(suiteId.isNotBlank()) { "suiteId must not be blank" }
    require(owner.isNotBlank()) { "owner must not be blank" }
    require(journey.isNotBlank()) { "journey must not be blank" }
    require(coverageContribution >= 0.0) { "coverageContribution cannot be negative" }
    require(riskTags.none { it.isBlank() }) { "riskTags must not contain blank entries" }
  }

  fun mitigatesRisk(riskId: String): Boolean {
    if (riskId.isBlank()) return false
    return riskTags.any { tag -> tag.equals(riskId.trim(), ignoreCase = false) }
  }
}
