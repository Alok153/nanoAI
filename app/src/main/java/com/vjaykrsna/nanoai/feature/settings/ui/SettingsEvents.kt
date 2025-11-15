package com.vjaykrsna.nanoai.feature.settings.ui

import com.vjaykrsna.nanoai.core.domain.settings.ImportSummary

internal fun exportSuccessMessage(path: String): String =
  "Backup exported to ${path.substringAfterLast('/')}…"

internal fun buildImportSuccessMessage(summary: ImportSummary): String {
  val personasTotal = summary.personasImported + summary.personasUpdated
  val providersTotal = summary.providersImported + summary.providersUpdated
  return buildString {
    append("Imported backup: ")
    append("$personasTotal persona${if (personasTotal == 1) "" else "s"}")
    append(", ")
    append("$providersTotal provider${if (providersTotal == 1) "" else "s"}")
  }
}
