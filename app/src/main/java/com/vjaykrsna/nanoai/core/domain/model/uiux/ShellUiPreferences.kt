package com.vjaykrsna.nanoai.core.domain.model.uiux

private const val MIN_FONT_SCALE = 0.85f
private const val MAX_FONT_SCALE = 1.4f

/**
 * Lightweight snapshot of UI preferences relevant to the shell/presentation layer.
 *
 * This is a simplified view of UI settings used by the shell and composables. For the full
 * persistence model used by DataStore, see [DataStoreUiPreferences].
 */
data class ShellUiPreferences(
  val theme: ThemePreference = ThemePreference.SYSTEM,
  val density: VisualDensity = VisualDensity.DEFAULT,
  val fontScale: Float = 1f,
  val dismissedTooltips: Set<String> = emptySet(),
  val highContrastEnabled: Boolean = false,
)

/** True when a tooltip with the provided [id] has been dismissed previously. */
fun ShellUiPreferences.isTooltipDismissed(id: String): Boolean = dismissedTooltips.contains(id)

/** Returns a copy with an additional dismissed tooltip tracked. */
fun ShellUiPreferences.withTooltipDismissed(id: String): ShellUiPreferences =
  copy(dismissedTooltips = dismissedTooltips + id)

/** Normalized font scale applied to typography (min 0.85f, max 1.4f). */
val ShellUiPreferences.normalizedFontScale: Float
  get() = fontScale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)

/** Creates a [ShellUiPreferences] from [UserProfile] domain model. */
fun UserProfile.toShellUiPreferences(): ShellUiPreferences =
  ShellUiPreferences(
    theme = themePreference,
    density = visualDensity,
    fontScale = 1f, // UserProfile does not track font scale; default to normal
    dismissedTooltips = emptySet(), // Tooltips stored separately in DataStore
    highContrastEnabled = false, // TODO: add to UserProfile if needed
  )
