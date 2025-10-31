package com.vjaykrsna.nanoai.core.coverage.model

/** Canonical coverage layers used across reports and dashboards. */
enum class TestLayer(val displayName: String) {
  VIEW_MODEL("View Model"),
  UI("UI"),
  DATA("Data");

  /** Machine-friendly identifier used when serialising layer names. */
  val machineName: String = displayName.replace(" ", "")

  /** Analytics-friendly identifier used for structured logging keys. */
  val analyticsKey: String = name.lowercase().replace('_', '-')

  companion object {
    fun fromAnalyticsKey(value: String): TestLayer? {
      if (value.isBlank()) return null
      val normalized = value.trim().lowercase().replace('_', '-').replace(' ', '-')
      return entries.firstOrNull { layer -> layer.analyticsKey == normalized }
    }
  }
}
