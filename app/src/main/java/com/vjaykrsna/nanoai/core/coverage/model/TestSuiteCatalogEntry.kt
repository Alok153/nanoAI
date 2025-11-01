package com.vjaykrsna.nanoai.core.coverage.model

/** Metadata describing an automated suite and the coverage gaps it mitigates. */
class TestSuiteCatalogEntry(
  val suiteId: String,
  val owner: String,
  val layer: TestLayer,
  val journey: String,
  coverageContribution: Double,
  riskTags: Set<String>,
) {
  val coverageContribution: Double
  val riskTags: Set<String>

  init {
    require(suiteId.isNotBlank()) { "suiteId must not be blank" }
    require(owner.isNotBlank()) { "owner must not be blank" }
    require(journey.isNotBlank()) { "journey must not be blank" }
    require(coverageContribution >= 0.0) { "coverageContribution cannot be negative" }

    this.coverageContribution = coverageContribution.roundToSingleDecimal()
    val normalizedTags =
      riskTags
        .map(::normalizeRiskTag)
        .also { normalized ->
          require(normalized.none { it.isEmpty() }) { "riskTags must not contain blank entries" }
        }
        .toSet()
    this.riskTags = normalizedTags
  }

  fun mitigatesRisk(riskId: String): Boolean {
    val normalizedRiskId = normalizeRiskTag(riskId)
    if (normalizedRiskId.isEmpty()) return false
    return riskTags.contains(normalizedRiskId)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TestSuiteCatalogEntry) return false

    return suiteId == other.suiteId &&
      owner == other.owner &&
      layer == other.layer &&
      journey == other.journey &&
      coverageContribution == other.coverageContribution &&
      riskTags == other.riskTags
  }

  override fun hashCode(): Int {
    var result = suiteId.hashCode()
    result = 31 * result + owner.hashCode()
    result = 31 * result + layer.hashCode()
    result = 31 * result + journey.hashCode()
    result = 31 * result + coverageContribution.hashCode()
    result = 31 * result + riskTags.hashCode()
    return result
  }

  override fun toString(): String =
    "TestSuiteCatalogEntry(suiteId=$suiteId, owner=$owner, layer=$layer, journey=$journey, " +
      "coverageContribution=$coverageContribution, riskTags=$riskTags)"

  companion object {
    private fun normalizeRiskTag(value: String): String = value.trim().lowercase()
  }
}
