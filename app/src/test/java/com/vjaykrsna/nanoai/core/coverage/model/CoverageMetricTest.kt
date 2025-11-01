package com.vjaykrsna.nanoai.core.coverage.model

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

  @Test
  fun `deltaFromThreshold rounds to single decimal place`() {
    val metric = CoverageMetric(coverage = 70.08, threshold = 65.0)

    assertThat(metric.deltaFromThreshold).isEqualTo(5.1)
  }

  @Test
  fun `meetsThreshold treats small deficits as passing`() {
    val metric = CoverageMetric(coverage = 64.96, threshold = 65.0)

    assertThat(metric.meetsThreshold()).isTrue()
  }

  @Test
  fun `status transitions respect rounded coverage`() {
    val below = CoverageMetric(coverage = 64.94, threshold = 65.0)
    val onTarget = CoverageMetric(coverage = 64.96, threshold = 65.0)
    val exceeds = CoverageMetric(coverage = 65.12, threshold = 65.0)

    assertThat(below.status).isEqualTo(CoverageMetric.Status.BELOW_TARGET)
    assertThat(onTarget.status).isEqualTo(CoverageMetric.Status.ON_TARGET)
    assertThat(exceeds.status).isEqualTo(CoverageMetric.Status.EXCEEDS_TARGET)
  }
}
