package com.vjaykrsna.nanoai.shared.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.vjaykrsna.nanoai.shared.ui.theme.ColorContrastUtil.calculateContrastRatio
import org.junit.Assert.assertTrue
import org.junit.Test

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

    // Test primary color combinations
    assertContrastRatio(colorScheme.primary, colorScheme.onPrimary, "Light Primary on OnPrimary")

    assertContrastRatio(
      colorScheme.primaryContainer,
      colorScheme.onPrimaryContainer,
      "Light PrimaryContainer on OnPrimaryContainer",
    )

    // Test secondary color combinations
    assertContrastRatio(
      colorScheme.secondary,
      colorScheme.onSecondary,
      "Light Secondary on OnSecondary",
    )

    assertContrastRatio(
      colorScheme.secondaryContainer,
      colorScheme.onSecondaryContainer,
      "Light SecondaryContainer on OnSecondaryContainer",
    )

    // Test tertiary color combinations
    assertContrastRatio(
      colorScheme.tertiary,
      colorScheme.onTertiary,
      "Light Tertiary on OnTertiary",
    )

    assertContrastRatio(
      colorScheme.tertiaryContainer,
      colorScheme.onTertiaryContainer,
      "Light TertiaryContainer on OnTertiaryContainer",
    )

    // Test background combinations
    assertContrastRatio(
      colorScheme.background,
      colorScheme.onBackground,
      "Light Background on OnBackground",
    )

    // Test surface combinations
    assertContrastRatio(colorScheme.surface, colorScheme.onSurface, "Light Surface on OnSurface")

    assertContrastRatio(
      colorScheme.surfaceVariant,
      colorScheme.onSurfaceVariant,
      "Light SurfaceVariant on OnSurfaceVariant",
    )

    // Test error combinations
    assertContrastRatio(colorScheme.error, colorScheme.onError, "Light Error on OnError")

    assertContrastRatio(
      colorScheme.errorContainer,
      colorScheme.onErrorContainer,
      "Light ErrorContainer on OnErrorContainer",
    )

    // Test inverse combinations
    assertContrastRatio(
      colorScheme.inverseSurface,
      colorScheme.inverseOnSurface,
      "Light InverseSurface on InverseOnSurface",
    )
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

    // Test primary color combinations
    assertContrastRatio(colorScheme.primary, colorScheme.onPrimary, "Dark Primary on OnPrimary")

    assertContrastRatio(
      colorScheme.primaryContainer,
      colorScheme.onPrimaryContainer,
      "Dark PrimaryContainer on OnPrimaryContainer",
    )

    // Test secondary color combinations
    assertContrastRatio(
      colorScheme.secondary,
      colorScheme.onSecondary,
      "Dark Secondary on OnSecondary",
    )

    assertContrastRatio(
      colorScheme.secondaryContainer,
      colorScheme.onSecondaryContainer,
      "Dark SecondaryContainer on OnSecondaryContainer",
    )

    // Test tertiary color combinations
    assertContrastRatio(colorScheme.tertiary, colorScheme.onTertiary, "Dark Tertiary on OnTertiary")

    assertContrastRatio(
      colorScheme.tertiaryContainer,
      colorScheme.onTertiaryContainer,
      "Dark TertiaryContainer on OnTertiaryContainer",
    )

    // Test background combinations
    assertContrastRatio(
      colorScheme.background,
      colorScheme.onBackground,
      "Dark Background on OnBackground",
    )

    // Test surface combinations
    assertContrastRatio(colorScheme.surface, colorScheme.onSurface, "Dark Surface on OnSurface")

    assertContrastRatio(
      colorScheme.surfaceVariant,
      colorScheme.onSurfaceVariant,
      "Dark SurfaceVariant on OnSurfaceVariant",
    )

    // Test error combinations
    assertContrastRatio(colorScheme.error, colorScheme.onError, "Dark Error on OnError")

    assertContrastRatio(
      colorScheme.errorContainer,
      colorScheme.onErrorContainer,
      "Dark ErrorContainer on OnErrorContainer",
    )

    // Test inverse combinations
    assertContrastRatio(
      colorScheme.inverseSurface,
      colorScheme.inverseOnSurface,
      "Dark InverseSurface on InverseOnSurface",
    )
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

    // Test primary color combinations
    assertContrastRatio(colorScheme.primary, colorScheme.onPrimary, "AMOLED Primary on OnPrimary")

    assertContrastRatio(
      colorScheme.primaryContainer,
      colorScheme.onPrimaryContainer,
      "AMOLED PrimaryContainer on OnPrimaryContainer",
    )

    // Test secondary color combinations
    assertContrastRatio(
      colorScheme.secondary,
      colorScheme.onSecondary,
      "AMOLED Secondary on OnSecondary",
    )

    assertContrastRatio(
      colorScheme.secondaryContainer,
      colorScheme.onSecondaryContainer,
      "AMOLED SecondaryContainer on OnSecondaryContainer",
    )

    // Test tertiary color combinations
    assertContrastRatio(
      colorScheme.tertiary,
      colorScheme.onTertiary,
      "AMOLED Tertiary on OnTertiary",
    )

    assertContrastRatio(
      colorScheme.tertiaryContainer,
      colorScheme.onTertiaryContainer,
      "AMOLED TertiaryContainer on OnTertiaryContainer",
    )

    // Test background combinations (AMOLED uses pure black background)
    assertContrastRatio(
      colorScheme.background,
      colorScheme.onBackground,
      "AMOLED Background on OnBackground",
    )

    // Test surface combinations (AMOLED uses pure black surface)
    assertContrastRatio(colorScheme.surface, colorScheme.onSurface, "AMOLED Surface on OnSurface")

    assertContrastRatio(
      colorScheme.surfaceVariant,
      colorScheme.onSurfaceVariant,
      "AMOLED SurfaceVariant on OnSurfaceVariant",
    )

    // Test error combinations
    assertContrastRatio(colorScheme.error, colorScheme.onError, "AMOLED Error on OnError")

    assertContrastRatio(
      colorScheme.errorContainer,
      colorScheme.onErrorContainer,
      "AMOLED ErrorContainer on OnErrorContainer",
    )

    // Test inverse combinations
    assertContrastRatio(
      colorScheme.inverseSurface,
      colorScheme.inverseOnSurface,
      "AMOLED InverseSurface on InverseOnSurface",
    )
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
}
