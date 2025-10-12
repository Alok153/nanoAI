package com.vjaykrsna.nanoai.coverage.ui

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vjaykrsna.nanoai.coverage.model.CoverageMetric
import com.vjaykrsna.nanoai.coverage.model.TestLayer
import com.vjaykrsna.nanoai.ui.theme.NanoAITheme
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoverageDashboardTest {

  @get:Rule val composeRule = createComposeRule()

  private val mockWebServer = MockWebServer()

  @Before
  fun setUp() {
    mockWebServer.start()
  }

  @After
  fun tearDown() {
    mockWebServer.shutdown()
  }

  @Test
  fun coverageLayersDisplayPercentagesAndTargets() {
    val state =
      CoverageDashboardUiState(
        buildId = "build-2025-10-10",
        generatedAtIso = "2025-10-10T12:30:00Z",
        isRefreshing = false,
        layers =
          listOf(
            LayerCoverageState(
              layer = TestLayer.VIEW_MODEL,
              metric = CoverageMetric(coverage = 81.0, threshold = 75.0),
            ),
            LayerCoverageState(
              layer = TestLayer.UI,
              metric = CoverageMetric(coverage = 66.0, threshold = 65.0),
            ),
          ),
        risks = emptyList(),
        trendDelta = mapOf(TestLayer.VIEW_MODEL to 2.4, TestLayer.UI to -0.5),
        errorMessage = null,
      )

    composeRule.setContent {
      NanoAITheme {
        CoverageDashboardScreen(
          state = state,
          onRefresh = {},
          onRiskSelect = {},
          onShareRequest = {},
        )
      }
    }

    composeRule.waitForIdle()

    composeRule.onNodeWithText("View Model").assertExists()
    composeRule.onNodeWithTag("coverage-layer-ViewModel").assertExists().assertTextContains("81.0%")
    composeRule.onNodeWithTag("coverage-layer-Ui").assertExists().assertTextContains("66.0%")
  }

  @Test
  fun riskChipAnnouncesSeverityAndStatus() {
    val state =
      CoverageDashboardUiState(
        buildId = "build-2025-10-10",
        generatedAtIso = "2025-10-10T12:30:00Z",
        isRefreshing = false,
        layers =
          listOf(
            LayerCoverageState(
              layer = TestLayer.DATA,
              metric = CoverageMetric(coverage = 71.0, threshold = 70.0),
            ),
          ),
        risks =
          listOf(
            RiskChipState(
              riskId = "risk-critical-data",
              title = "Offline writes failing",
              severity = "CRITICAL",
              status = "OPEN",
            ),
          ),
        trendDelta = emptyMap(),
        errorMessage = null,
      )

    composeRule.setContent {
      NanoAITheme {
        CoverageDashboardScreen(
          state = state,
          onRefresh = {},
          onRiskSelect = {},
          onShareRequest = {},
        )
      }
    }

    composeRule.waitForIdle()

    composeRule
      .onNodeWithContentDescription("Risk risk-critical-data, severity CRITICAL, status OPEN")
      .assertExists()
      .assertContentDescriptionEquals("Risk risk-critical-data, severity CRITICAL, status OPEN")
  }

  @Test
  fun offlineFallback_showsErrorBannerWithAccessibleAnnouncement() {
    mockWebServer.enqueue(MockResponse().setResponseCode(503).setBody("Device farm offline"))
    val offlineMessage = "Device farm offline — showing cached coverage while tests reroute."
    val offlineAnnouncement =
      "Offline coverage fallback active. Showing cached metrics until device farm recovers."

    val state =
      CoverageDashboardUiState(
        buildId = "build-2025-10-10",
        generatedAtIso = "2025-10-10T12:30:00Z",
        isRefreshing = false,
        layers =
          listOf(
            LayerCoverageState(
              layer = TestLayer.VIEW_MODEL,
              metric = CoverageMetric(coverage = 81.0, threshold = 75.0),
            ),
            LayerCoverageState(
              layer = TestLayer.UI,
              metric = CoverageMetric(coverage = 66.0, threshold = 65.0),
            ),
          ),
        risks = emptyList(),
        trendDelta = emptyMap(),
        errorMessage = offlineMessage,
      )

    composeRule.setContent {
      NanoAITheme {
        CoverageDashboardScreen(
          state = state,
          onRefresh = {},
          onRiskSelect = {},
          onShareRequest = {},
        )
      }
    }

    composeRule.waitForIdle()

    composeRule
      .onNodeWithTag("coverage-dashboard-error-banner")
      .assertExists()
      .assertTextContains("Device farm offline")

    composeRule
      .onNodeWithContentDescription(offlineAnnouncement)
      .assertExists()
      .assertContentDescriptionEquals(offlineAnnouncement)
  }
}
