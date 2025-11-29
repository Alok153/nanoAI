package com.vjaykrsna.nanoai.shared.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.vjaykrsna.nanoai.shared.ui.theme.ColorContrastUtil.calculateContrastRatio
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for color contrast ratios to ensure WCAG AA compliance.
 *
 * WCAG AA requires a minimum contrast ratio of 4.5:1 for normal text and 3:1 for large text (18pt+
 * or 14pt+ bold).
 */
class ColorContrastTest {

  private val wcagAAMinimumRatio = 4.5

  @Test
  fun `light color scheme meets WCAG AA contrast requirements`() {
    val colorScheme =
      lightColorScheme(
        primary = LightPrimary,
        onPrimary = LightOnPrimary,
        primaryContainer = LightPrimaryContainer,
        onPrimaryContainer = LightOnPrimaryContainer,
        secondary = LightSecondary,
        onSecondary = LightOnSecondary,
        secondaryContainer = LightSecondaryContainer,
        onSecondaryContainer = LightOnSecondaryContainer,
        tertiary = LightTertiary,
        onTertiary = LightOnTertiary,
        tertiaryContainer = LightTertiaryContainer,
        onTertiaryContainer = LightOnTertiaryContainer,
        background = LightBackground,
        onBackground = LightOnBackground,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        outline = LightOutline,
        outlineVariant = LightOutlineVariant,
        error = LightError,
        onError = LightOnError,
        errorContainer = LightErrorContainer,
        onErrorContainer = LightOnErrorContainer,
        inverseSurface = LightInverseSurface,
        inverseOnSurface = LightInverseOnSurface,
        inversePrimary = LightInversePrimary,
      )

    assertSchemeContrast("Light", colorScheme)
  }

  @Test
  fun `dark color scheme meets WCAG AA contrast requirements`() {
    val colorScheme =
      darkColorScheme(
        primary = DarkPrimary,
        onPrimary = DarkOnPrimary,
        primaryContainer = DarkPrimaryContainer,
        onPrimaryContainer = DarkOnPrimaryContainer,
        secondary = DarkSecondary,
        onSecondary = DarkOnSecondary,
        secondaryContainer = DarkSecondaryContainer,
        onSecondaryContainer = DarkOnSecondaryContainer,
        tertiary = DarkTertiary,
        onTertiary = DarkOnTertiary,
        tertiaryContainer = DarkTertiaryContainer,
        onTertiaryContainer = DarkOnTertiaryContainer,
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        outline = DarkOutline,
        outlineVariant = DarkOutlineVariant,
        error = DarkError,
        onError = DarkOnError,
        errorContainer = DarkErrorContainer,
        onErrorContainer = DarkOnErrorContainer,
        inverseSurface = DarkInverseSurface,
        inverseOnSurface = DarkInverseOnSurface,
        inversePrimary = DarkInversePrimary,
      )

    assertSchemeContrast("Dark", colorScheme)
  }

  @Test
  fun `amoled color scheme meets WCAG AA contrast requirements`() {
    val colorScheme =
      darkColorScheme(
        primary = DarkPrimary,
        onPrimary = DarkOnPrimary,
        primaryContainer = DarkPrimaryContainer,
        onPrimaryContainer = DarkOnPrimaryContainer,
        secondary = DarkSecondary,
        onSecondary = DarkOnSecondary,
        secondaryContainer = DarkSecondaryContainer,
        onSecondaryContainer = DarkOnSecondaryContainer,
        tertiary = DarkTertiary,
        onTertiary = DarkOnTertiary,
        tertiaryContainer = DarkTertiaryContainer,
        onTertiaryContainer = DarkOnTertiaryContainer,
        background = AmoledBackground,
        onBackground = DarkOnBackground,
        surface = AmoledSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = AmoledSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        outline = DarkOutline,
        outlineVariant = DarkOutlineVariant,
        error = DarkError,
        onError = DarkOnError,
        errorContainer = DarkErrorContainer,
        onErrorContainer = DarkOnErrorContainer,
        inverseSurface = DarkInverseSurface,
        inverseOnSurface = DarkInverseOnSurface,
        inversePrimary = DarkInversePrimary,
      )

    assertSchemeContrast("AMOLED", colorScheme)
  }

  /**
   * Asserts that the contrast ratio between two colors meets WCAG AA requirements.
   *
   * @param background The background color
   * @param foreground The foreground/text color
   * @param description A description of the color combination being tested
   */
  private fun assertContrastRatio(
    background: androidx.compose.ui.graphics.Color,
    foreground: androidx.compose.ui.graphics.Color,
    description: String,
  ) {
    val contrastRatio = calculateContrastRatio(background, foreground)
    assertTrue(
      "$description has contrast ratio $contrastRatio:1, which is below WCAG AA minimum of $wcagAAMinimumRatio:1",
      contrastRatio >= wcagAAMinimumRatio,
    )
  }

  private fun assertSchemeContrast(label: String, scheme: ColorScheme) {
    listOf(
        Triple("$label Primary on OnPrimary", scheme.primary, scheme.onPrimary),
        Triple(
          "$label PrimaryContainer on OnPrimaryContainer",
          scheme.primaryContainer,
          scheme.onPrimaryContainer,
        ),
        Triple("$label Secondary on OnSecondary", scheme.secondary, scheme.onSecondary),
        Triple(
          "$label SecondaryContainer on OnSecondaryContainer",
          scheme.secondaryContainer,
          scheme.onSecondaryContainer,
        ),
        Triple("$label Tertiary on OnTertiary", scheme.tertiary, scheme.onTertiary),
        Triple(
          "$label TertiaryContainer on OnTertiaryContainer",
          scheme.tertiaryContainer,
          scheme.onTertiaryContainer,
        ),
        Triple("$label Background on OnBackground", scheme.background, scheme.onBackground),
        Triple("$label Surface on OnSurface", scheme.surface, scheme.onSurface),
        Triple(
          "$label SurfaceVariant on OnSurfaceVariant",
          scheme.surfaceVariant,
          scheme.onSurfaceVariant,
        ),
        Triple("$label Error on OnError", scheme.error, scheme.onError),
        Triple(
          "$label ErrorContainer on OnErrorContainer",
          scheme.errorContainer,
          scheme.onErrorContainer,
        ),
        Triple(
          "$label InverseSurface on InverseOnSurface",
          scheme.inverseSurface,
          scheme.inverseOnSurface,
        ),
      )
      .forEach { (description, background, foreground) ->
        assertContrastRatio(background, foreground, description)
      }
  }
}
