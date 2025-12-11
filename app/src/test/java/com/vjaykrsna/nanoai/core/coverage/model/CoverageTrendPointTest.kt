package com.vjaykrsna.nanoai.core.coverage.model

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.jupiter.api.Nested
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
  fun `deltaFromThreshold returns negative difference when coverage lower`() {
    val point =
      CoverageTrendPoint(
        buildId = "build-1",
        layer = TestLayer.DATA,
        coverage = 65.0,
        threshold = 70.0,
        recordedAt = Instant.parse("2025-10-10T12:00:00Z"),
      )

    assertThat(point.deltaFromThreshold()).isWithin(0.0001).of(-5.0)
  }

  @Test
  fun `deltaFromThreshold returns zero when coverage equals threshold`() {
    val point =
      CoverageTrendPoint(
        buildId = "build-1",
        layer = TestLayer.UI,
        coverage = 65.0,
        threshold = 65.0,
        recordedAt = Instant.parse("2025-10-10T12:00:00Z"),
      )

    assertThat(point.deltaFromThreshold()).isWithin(0.0001).of(0.0)
  }

  @Nested
  inner class InitValidation {

    @Test
    fun `throws when buildId is blank`() {
      assertThrows<IllegalArgumentException> {
        CoverageTrendPoint(
          buildId = "",
          layer = TestLayer.VIEW_MODEL,
          coverage = 75.0,
          threshold = 75.0,
          recordedAt = Instant.now(),
        )
      }
    }

    @Test
    fun `throws when buildId is whitespace only`() {
      assertThrows<IllegalArgumentException> {
        CoverageTrendPoint(
          buildId = "   ",
          layer = TestLayer.VIEW_MODEL,
          coverage = 75.0,
          threshold = 75.0,
          recordedAt = Instant.now(),
        )
      }
    }

    @Test
    fun `throws when coverage is negative`() {
      assertThrows<IllegalArgumentException> {
        CoverageTrendPoint(
          buildId = "build-1",
          layer = TestLayer.DATA,
          coverage = -1.0,
          threshold = 70.0,
          recordedAt = Instant.now(),
        )
      }
    }

    @Test
    fun `throws when coverage exceeds 100`() {
      assertThrows<IllegalArgumentException> {
        CoverageTrendPoint(
          buildId = "build-1",
          layer = TestLayer.DATA,
          coverage = 100.1,
          threshold = 70.0,
          recordedAt = Instant.now(),
        )
      }
    }

    @Test
    fun `throws when threshold is negative`() {
      assertThrows<IllegalArgumentException> {
        CoverageTrendPoint(
          buildId = "build-1",
          layer = TestLayer.UI,
          coverage = 65.0,
          threshold = -5.0,
          recordedAt = Instant.now(),
        )
      }
    }

    @Test
    fun `throws when threshold exceeds 100`() {
      assertThrows<IllegalArgumentException> {
        CoverageTrendPoint(
          buildId = "build-1",
          layer = TestLayer.UI,
          coverage = 65.0,
          threshold = 101.0,
          recordedAt = Instant.now(),
        )
      }
    }

    @Test
    fun `accepts edge case of 0 percent coverage`() {
      val point =
        CoverageTrendPoint(
          buildId = "build-1",
          layer = TestLayer.DATA,
          coverage = 0.0,
          threshold = 70.0,
          recordedAt = Instant.now(),
        )
      assertThat(point.coverage).isEqualTo(0.0)
    }

    @Test
    fun `accepts edge case of 100 percent coverage`() {
      val point =
        CoverageTrendPoint(
          buildId = "build-1",
          layer = TestLayer.DATA,
          coverage = 100.0,
          threshold = 70.0,
          recordedAt = Instant.now(),
        )
      assertThat(point.coverage).isEqualTo(100.0)
    }
  }

  @Nested
  inner class FactoryMethods {

    @Test
    fun `fromMetric creates point with correct values`() {
      val metric = CoverageMetric(coverage = 78.5, threshold = 75.0)
      val recordedAt = Instant.parse("2025-10-10T12:00:00Z")

      val point =
        CoverageTrendPoint.fromMetric(
          buildId = "build-1",
          layer = TestLayer.VIEW_MODEL,
          metric = metric,
          recordedAt = recordedAt,
        )

      assertThat(point.buildId).isEqualTo("build-1")
      assertThat(point.layer).isEqualTo(TestLayer.VIEW_MODEL)
      assertThat(point.coverage).isEqualTo(78.5)
      assertThat(point.threshold).isEqualTo(75.0)
      assertThat(point.recordedAt).isEqualTo(recordedAt)
    }

    @Test
    fun `fromSummary creates point with threshold from summary`() {
      val timestamp = Instant.parse("2025-10-10T12:00:00Z")
      val thresholds =
        mapOf(TestLayer.VIEW_MODEL to 75.0, TestLayer.UI to 65.0, TestLayer.DATA to 70.0)
      val summary =
        CoverageSummary(
          buildId = "build-1",
          timestamp = timestamp,
          layerMetrics =
            linkedMapOf(
              TestLayer.VIEW_MODEL to CoverageMetric(78.5, 75.0),
              TestLayer.UI to CoverageMetric(68.0, 65.0),
              TestLayer.DATA to CoverageMetric(72.0, 70.0),
            ),
          thresholds = thresholds,
          trendDelta = emptyMap(),
          riskItems = emptyList(),
        )

      val recordedAt = Instant.parse("2025-10-11T12:00:00Z")
      val point =
        CoverageTrendPoint.fromSummary(
          buildId = "build-2",
          layer = TestLayer.DATA,
          coverage = 73.0,
          summary = summary,
          recordedAt = recordedAt,
        )

      assertThat(point.buildId).isEqualTo("build-2")
      assertThat(point.layer).isEqualTo(TestLayer.DATA)
      assertThat(point.coverage).isEqualTo(73.0)
      assertThat(point.threshold).isEqualTo(70.0)
      assertThat(point.recordedAt).isEqualTo(recordedAt)
    }
  }

  @Nested
  inner class ValidateSequence {

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

    @Test
    fun `validateSequence passes for empty list`() {
      CoverageTrendPoint.validateSequence(emptyList())
    }

    @Test
    fun `validateSequence passes for single point`() {
      val points =
        listOf(
          CoverageTrendPoint(
            buildId = "build-1",
            layer = TestLayer.VIEW_MODEL,
            coverage = 75.0,
            threshold = 75.0,
            recordedAt = Instant.now(),
          )
        )

      CoverageTrendPoint.validateSequence(points)
    }

    @Test
    fun `validateSequence passes for different layers`() {
      val points =
        listOf(
          CoverageTrendPoint(
            buildId = "build-1",
            layer = TestLayer.VIEW_MODEL,
            coverage = 78.0,
            threshold = 75.0,
            recordedAt = Instant.parse("2025-10-08T12:00:00Z"),
          ),
          CoverageTrendPoint(
            buildId = "build-1",
            layer = TestLayer.DATA,
            coverage = 65.0,
            threshold = 70.0,
            recordedAt = Instant.parse("2025-10-08T12:00:00Z"),
          ),
        )

      CoverageTrendPoint.validateSequence(points)
    }

    @Test
    fun `validateSequence passes for same timestamps`() {
      val sameTime = Instant.parse("2025-10-08T12:00:00Z")
      val points =
        listOf(
          CoverageTrendPoint(
            buildId = "build-1",
            layer = TestLayer.VIEW_MODEL,
            coverage = 75.0,
            threshold = 75.0,
            recordedAt = sameTime,
          ),
          CoverageTrendPoint(
            buildId = "build-2",
            layer = TestLayer.VIEW_MODEL,
            coverage = 76.0,
            threshold = 75.0,
            recordedAt = sameTime,
          ),
        )

      CoverageTrendPoint.validateSequence(points)
    }

    @Test
    fun `validateSequence passes when coverage stays same`() {
      val points =
        listOf(
          CoverageTrendPoint(
            buildId = "build-1",
            layer = TestLayer.UI,
            coverage = 67.0,
            threshold = 65.0,
            recordedAt = Instant.parse("2025-10-08T12:00:00Z"),
          ),
          CoverageTrendPoint(
            buildId = "build-2",
            layer = TestLayer.UI,
            coverage = 67.0,
            threshold = 65.0,
            recordedAt = Instant.parse("2025-10-09T12:00:00Z"),
          ),
        )

      CoverageTrendPoint.validateSequence(points)
    }
  }
}
