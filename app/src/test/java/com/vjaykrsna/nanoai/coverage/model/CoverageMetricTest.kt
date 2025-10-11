package com.vjaykrsna.nanoai.coverage.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CoverageMetricTest {

  @Test
  fun `status is EXCEEDS_TARGET when coverage surpasses threshold`() {
    val metric = CoverageMetric(coverage = 82.5, threshold = 75.0)

    assertThat(metric.status).isEqualTo(CoverageMetric.Status.EXCEEDS_TARGET)
  }

  @Test
  fun `deltaFromThreshold is positive when coverage exceeds threshold`() {
    val metric = CoverageMetric(coverage = 78.0, threshold = 70.0)

    assertThat(metric.deltaFromThreshold).isGreaterThan(0.0)
    assertThat(metric.deltaFromThreshold).isWithin(0.0001).of(8.0)
  }
}
