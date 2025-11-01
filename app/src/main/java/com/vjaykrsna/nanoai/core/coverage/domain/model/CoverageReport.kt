package com.vjaykrsna.nanoai.core.coverage.domain.model

data class CoverageReport(val totalCoverage: Float, val fileCoverage: Map<String, Float>)
