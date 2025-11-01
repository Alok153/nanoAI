package com.vjaykrsna.nanoai.core.coverage.model

/** Lightweight reference to a risk register entry used for cross-linking coverage reports. */
data class RiskRegisterItemRef(val riskId: String) {
  init {
    val normalized = riskId.trim()
    require(normalized.isNotEmpty()) { "riskId must not be blank" }
    require(normalized == normalized.lowercase()) { "riskId must be lowercase kebab-case" }
    require(normalized == riskId) { "riskId must be trimmed" }
    require(ID_PATTERN.matches(normalized)) { "riskId must match kebab-case format: $riskId" }
  }

  companion object {
    private val ID_PATTERN = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")
  }
}
