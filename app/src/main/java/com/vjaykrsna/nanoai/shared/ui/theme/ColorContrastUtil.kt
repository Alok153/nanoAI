@file:Suppress("MagicNumber")

package com.vjaykrsna.nanoai.shared.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Utility functions for calculating color contrast ratios according to WCAG guidelines. */
object ColorContrastUtil {

  /**
   * Calculates the contrast ratio between two colors according to WCAG 2.1 guidelines.
   *
   * The contrast ratio is calculated as: (L1 + 0.05) / (L2 + 0.05) where L1 and L2 are the relative
   * luminances of the two colors, with the larger luminance value in the numerator.
   *
   * @param color1 The first color
   * @param color2 The second color
   * @return The contrast ratio between the two colors
   */
  fun calculateContrastRatio(color1: Color, color2: Color): Double {
    val luminance1 = calculateRelativeLuminance(color1)
    val luminance2 = calculateRelativeLuminance(color2)

    val lighter = max(luminance1, luminance2)
    val darker = min(luminance1, luminance2)

    return (lighter + 0.05) / (darker + 0.05)
  }

  /**
   * Calculates the relative luminance of a color according to WCAG 2.1 guidelines.
   *
   * The formula uses the sRGB color space and applies gamma correction: L = 0.2126 * R' + 0.7152 *
   * G' + 0.0722 * B'
   *
   * Where R', G', B' are the gamma-corrected RGB values in the range [0, 1].
   *
   * @param color The color to calculate luminance for
   * @return The relative luminance value in the range [0, 1]
   */
  private fun calculateRelativeLuminance(color: Color): Double {
    val r = gammaCorrect(color.red)
    val g = gammaCorrect(color.green)
    val b = gammaCorrect(color.blue)

    return 0.2126 * r + 0.7152 * g + 0.0722 * b
  }

  /**
   * Applies gamma correction to an sRGB color component.
   *
   * For values <= 0.03928: linear = value / 12.92 For values > 0.03928: linear = ((value + 0.055) /
   * 1.055) ^ 2.4
   *
   * @param value The sRGB color component value in the range [0, 1]
   * @return The gamma-corrected linear value
   */
  private fun gammaCorrect(value: Float): Double {
    return if (value <= 0.03928f) {
      value / 12.92
    } else {
      ((value + 0.055) / 1.055).toDouble().pow(2.4)
    }
  }
}
