package com.vjaykrsna.nanoai.coverage.ui

import java.util.Locale

/**
 * Centralises offline fallback messaging so Compose UI and tests share the same copy and accessible
 * announcement.
 */
internal object CoverageDashboardBanner {
  const val OFFLINE_MESSAGE = "Device farm offline — showing cached coverage while tests reroute."
  const val OFFLINE_ANNOUNCEMENT =
    "Offline coverage fallback active. Showing cached metrics until device farm recovers."

  fun offline(cause: Throwable?): CoverageBanner =
    CoverageBanner(
      message = decorateMessage(OFFLINE_MESSAGE, cause),
      announcement = OFFLINE_ANNOUNCEMENT,
    )

  private fun decorateMessage(base: String, cause: Throwable?): String {
    val details = cause?.message?.takeIf { !it.isNullOrBlank() }?.trim()
    if (details.isNullOrEmpty()) {
      return base
    }
    val capitalised =
      details.replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(Locale.US) else ch.toString()
      }
    return buildString {
      append(base)
      append('\n')
      append("Reason: ")
      append(capitalised)
    }
  }
}

internal data class CoverageBanner(val message: String, val announcement: String)
