package com.vjaykrsna.nanoai.feature.uiux.ui

import com.vjaykrsna.nanoai.feature.uiux.presentation.BadgeInfo
import com.vjaykrsna.nanoai.feature.uiux.presentation.BadgeType
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.text.titlecase

private const val MINUTES_THRESHOLD_IMMEDIATE = 1L
private const val MINUTES_PER_HOUR = 60L
private const val HOURS_PER_DAY = 24L
private const val DAYS_PER_WEEK = 7L

internal fun badgeText(info: BadgeInfo): String =
  when (info.type) {
    BadgeType.NEW -> if (info.count > 0) "${info.count} new" else "New"
    BadgeType.PRO -> "Pro"
    BadgeType.SYNCING -> if (info.count > 0) "${info.count}" else "Sync"
  }

internal fun formatRelativeTime(timestamp: Instant, reference: Instant = Instant.now()): String {
  val duration = Duration.between(timestamp, reference)
  val totalSeconds = max(0L, duration.seconds)
  val minutes = totalSeconds / MINUTES_PER_HOUR
  val hours = minutes / MINUTES_PER_HOUR
  val days = hours / HOURS_PER_DAY
  return when {
    minutes < MINUTES_THRESHOLD_IMMEDIATE -> "Just now"
    minutes < MINUTES_PER_HOUR -> "${minutes}m ago"
    hours < HOURS_PER_DAY -> "${hours}h ago"
    days < DAYS_PER_WEEK -> "${days}d ago"
    else ->
      DateTimeFormatter.ofPattern("MMM d")
        .withLocale(Locale.getDefault())
        .format(timestamp.atZone(ZoneId.systemDefault()))
  }
}

internal fun formatAbsoluteTime(timestamp: Instant): String {
  val localDateTime = timestamp.atZone(ZoneId.systemDefault()).toLocalDateTime()
  val formatter = DateTimeFormatter.ofPattern("MMM d · HH:mm")
  return formatter.format(localDateTime)
}

internal fun ModeId.displayName(): String =
  name.lowercase(Locale.getDefault()).replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
  }
