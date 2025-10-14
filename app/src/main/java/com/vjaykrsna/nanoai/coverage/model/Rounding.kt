package com.vjaykrsna.nanoai.coverage.model

import kotlin.math.roundToInt

// Shared helper to round floating point metrics to a single decimal place.
internal fun Double.roundToSingleDecimal(): Double =
  (this * ONE_DECIMAL_SCALE).roundToInt() / ONE_DECIMAL_SCALE

private const val ONE_DECIMAL_SCALE = 10.0
