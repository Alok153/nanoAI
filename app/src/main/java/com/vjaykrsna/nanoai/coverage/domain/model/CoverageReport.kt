package com.vjaykrsna.nanoai.coverage.domain.model

data class CoverageReport(val totalCoverage: Float, val fileCoverage: Map<String, Float>)
