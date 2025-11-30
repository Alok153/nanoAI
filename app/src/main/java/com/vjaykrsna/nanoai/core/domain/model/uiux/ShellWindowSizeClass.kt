package com.vjaykrsna.nanoai.core.domain.model.uiux

/**
 * Platform-agnostic representation of the current window size buckets.
 *
 * Keeps the domain layer independent from Compose-specific `WindowSizeClass` while still exposing
 * the semantic width/height groupings required by navigation and layout logic.
 */
data class ShellWindowSizeClass(
  val widthSizeClass: ShellWindowWidthClass,
  val heightSizeClass: ShellWindowHeightClass,
) {
  companion object {
    val Default =
      ShellWindowSizeClass(
        widthSizeClass = ShellWindowWidthClass.MEDIUM,
        heightSizeClass = ShellWindowHeightClass.MEDIUM,
      )
  }
}

/** Discrete categories for width to mirror Material3 window size guidance without Compose deps. */
enum class ShellWindowWidthClass {
  COMPACT,
  MEDIUM,
  EXPANDED,
}

/** Discrete categories for height to mirror Material3 window size guidance without Compose deps. */
enum class ShellWindowHeightClass {
  COMPACT,
  MEDIUM,
  EXPANDED,
}
