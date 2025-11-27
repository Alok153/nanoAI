package com.vjaykrsna.nanoai.core.domain.model.uiux

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class UiPreferenceSnapshotTest {

  @Test
  fun `default snapshot has system theme`() {
    val snapshot = UiPreferenceSnapshot()
    assertThat(snapshot.theme).isEqualTo(ThemePreference.SYSTEM)
  }

  @Test
  fun `default snapshot has default density`() {
    val snapshot = UiPreferenceSnapshot()
    assertThat(snapshot.density).isEqualTo(VisualDensity.DEFAULT)
  }

  @Test
  fun `default snapshot has font scale of one`() {
    val snapshot = UiPreferenceSnapshot()
    assertThat(snapshot.fontScale).isEqualTo(1f)
  }

  @Test
  fun `default snapshot has empty dismissed tooltips`() {
    val snapshot = UiPreferenceSnapshot()
    assertThat(snapshot.dismissedTooltips).isEmpty()
  }

  @Test
  fun `isTooltipDismissed returns true for dismissed tooltip`() {
    val snapshot = UiPreferenceSnapshot(dismissedTooltips = setOf("tip1", "tip2"))
    assertThat(snapshot.isTooltipDismissed("tip1")).isTrue()
  }

  @Test
  fun `isTooltipDismissed returns false for non-dismissed tooltip`() {
    val snapshot = UiPreferenceSnapshot(dismissedTooltips = setOf("tip1"))
    assertThat(snapshot.isTooltipDismissed("tip3")).isFalse()
  }

  @Test
  fun `withTooltipDismissed adds tooltip to set`() {
    val snapshot = UiPreferenceSnapshot(dismissedTooltips = setOf("tip1"))
    val updated = snapshot.withTooltipDismissed("tip2")
    assertThat(updated.dismissedTooltips).containsExactly("tip1", "tip2")
  }

  @Test
  fun `withTooltipDismissed preserves existing tooltips`() {
    val snapshot = UiPreferenceSnapshot(dismissedTooltips = setOf("tip1"))
    val updated = snapshot.withTooltipDismissed("tip1")
    assertThat(updated.dismissedTooltips).containsExactly("tip1")
  }

  @Test
  fun `normalizedFontScale returns 1f for normal scale`() {
    val snapshot = UiPreferenceSnapshot(fontScale = 1f)
    assertThat(snapshot.normalizedFontScale).isEqualTo(1f)
  }

  @Test
  fun `normalizedFontScale coerces scale below minimum to 0_85`() {
    val snapshot = UiPreferenceSnapshot(fontScale = 0.5f)
    assertThat(snapshot.normalizedFontScale).isEqualTo(0.85f)
  }

  @Test
  fun `normalizedFontScale coerces scale above maximum to 1_4`() {
    val snapshot = UiPreferenceSnapshot(fontScale = 2f)
    assertThat(snapshot.normalizedFontScale).isEqualTo(1.4f)
  }

  @Test
  fun `normalizedFontScale returns scale within range unchanged`() {
    val snapshot = UiPreferenceSnapshot(fontScale = 1.2f)
    assertThat(snapshot.normalizedFontScale).isEqualTo(1.2f)
  }

  @Test
  fun `toUiPreferenceSnapshot converts UserProfile correctly`() {
    val userProfile =
      UserProfile(
        id = "test-user",
        displayName = "Test User",
        themePreference = ThemePreference.DARK,
        visualDensity = VisualDensity.COMPACT,
        lastOpenedScreen = ScreenType.HOME,
        compactMode = true,
        pinnedTools = listOf("tool1"),
        savedLayouts = emptyList(),
      )

    val snapshot = userProfile.toUiPreferenceSnapshot()

    assertThat(snapshot.theme).isEqualTo(ThemePreference.DARK)
    assertThat(snapshot.density).isEqualTo(VisualDensity.COMPACT)
    assertThat(snapshot.fontScale).isEqualTo(1f)
    assertThat(snapshot.dismissedTooltips).isEmpty()
  }

  @Test
  fun `copy preserves all fields`() {
    val original =
      UiPreferenceSnapshot(
        theme = ThemePreference.DARK,
        density = VisualDensity.EXPANDED,
        fontScale = 1.2f,
        dismissedTooltips = setOf("tip1"),
        highContrastEnabled = true,
      )

    val copy = original.copy()

    assertThat(copy).isEqualTo(original)
  }

  @Test
  fun `copy allows overriding specific fields`() {
    val original = UiPreferenceSnapshot(theme = ThemePreference.LIGHT)
    val copy = original.copy(theme = ThemePreference.DARK)
    assertThat(copy.theme).isEqualTo(ThemePreference.DARK)
  }
}
