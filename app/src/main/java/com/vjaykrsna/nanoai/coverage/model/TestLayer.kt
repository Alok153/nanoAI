package com.vjaykrsna.nanoai.coverage.model

/** Canonical coverage layers used across reports and dashboards. */
enum class TestLayer(val displayName: String) {
  VIEW_MODEL("View Model"),
  UI("UI"),
  DATA("Data");

  /** Machine-friendly identifier used when serialising layer names. */
  val machineName: String =
    name
      .lowercase()
      .split("_")
      .mapIndexed { index, part ->
        if (index == 0) part else part.replaceFirstChar { it.titlecase() }
      }
      .joinToString(separator = "")

  /** Analytics-friendly identifier used for structured logging keys. */
  val analyticsKey: String = name.lowercase().replace('_', '-')
}
