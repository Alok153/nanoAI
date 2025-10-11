package com.vjaykrsna.nanoai.coverage.verification

import com.vjaykrsna.nanoai.coverage.model.CoverageSummary
import com.vjaykrsna.nanoai.coverage.model.TestLayer

/** Validates that coverage results honour the agreed upon minimum thresholds per layer. */
class CoverageThresholdVerifier(
  private val minimums: Map<TestLayer, Double> = DEFAULT_MINIMUMS,
) {

  data class Result(val belowThresholdLayers: Set<TestLayer>)

  class ThresholdViolation(val layers: Set<TestLayer>) :
    IllegalStateException(
      "Coverage below threshold for layers: ${layers.joinToString()}",
    )

  fun verify(summary: CoverageSummary): Result {
    val failingLayers =
      summary.layerMetrics
        .mapNotNull { (layer, metric) ->
          val minimum = minimums[layer] ?: summary.thresholdFor(layer)
          if (metric.coverage < minimum) layer else null
        }
        .toSet()

    if (failingLayers.isNotEmpty()) {
      throw ThresholdViolation(failingLayers)
    }

    return Result(emptySet())
  }

  companion object {
    private val DEFAULT_MINIMUMS =
      mapOf(
        TestLayer.VIEW_MODEL to 75.0,
        TestLayer.UI to 65.0,
        TestLayer.DATA to 70.0,
      )
  }
}
