package com.vjaykrsna.nanoai.coverage.model

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CoverageTrendPointTest {

  @Test
  fun `deltaFromThreshold returns positive difference when coverage higher`() {
    val point =
      CoverageTrendPoint(
        buildId = "build-1",
        layer = TestLayer.VIEW_MODEL,
        coverage = 78.5,
        threshold = 75.0,
        recordedAt = Instant.parse("2025-10-10T12:00:00Z"),
      )

    assertThat(point.deltaFromThreshold()).isWithin(0.0001).of(3.5)
  }

  @Test
  fun `validateSequence throws when recordedAt decreases`() {
    val points =
      listOf(
        CoverageTrendPoint(
          buildId = "build-1",
          layer = TestLayer.VIEW_MODEL,
          coverage = 70.0,
          threshold = 75.0,
          recordedAt = Instant.parse("2025-10-09T12:00:00Z"),
        ),
        CoverageTrendPoint(
          buildId = "build-2",
          layer = TestLayer.VIEW_MODEL,
          coverage = 71.0,
          threshold = 75.0,
          recordedAt = Instant.parse("2025-10-08T12:00:00Z"),
        ),
      )

    assertThrows<IllegalArgumentException> { CoverageTrendPoint.validateSequence(points) }
  }

  @Test
  fun `validateSequence throws when coverage regresses`() {
    val points =
      listOf(
        CoverageTrendPoint(
          buildId = "build-1",
          layer = TestLayer.DATA,
          coverage = 71.0,
          threshold = 70.0,
          recordedAt = Instant.parse("2025-10-08T12:00:00Z"),
        ),
        CoverageTrendPoint(
          buildId = "build-2",
          layer = TestLayer.DATA,
          coverage = 69.0,
          threshold = 70.0,
          recordedAt = Instant.parse("2025-10-09T12:00:00Z"),
        ),
      )

    assertThrows<IllegalArgumentException> { CoverageTrendPoint.validateSequence(points) }
  }

  @Test
  fun `validateSequence throws when thresholds differ across points`() {
    val points =
      listOf(
        CoverageTrendPoint(
          buildId = "build-1",
          layer = TestLayer.VIEW_MODEL,
          coverage = 74.0,
          threshold = 75.0,
          recordedAt = Instant.parse("2025-10-08T12:00:00Z"),
        ),
        CoverageTrendPoint(
          buildId = "build-2",
          layer = TestLayer.VIEW_MODEL,
          coverage = 76.0,
          threshold = 74.0,
          recordedAt = Instant.parse("2025-10-09T12:00:00Z"),
        ),
      )

    assertThrows<IllegalArgumentException> { CoverageTrendPoint.validateSequence(points) }
  }
}
