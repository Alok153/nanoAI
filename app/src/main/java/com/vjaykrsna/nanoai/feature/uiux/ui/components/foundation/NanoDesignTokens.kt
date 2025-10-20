package com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material Design 3 Spacing Scale. All values are multiples of 4dp base unit, following Material
 * Design guidelines. Reference:
 * https://m3.material.io/foundations/layout/understanding-layout/spacing
 */
@Immutable
object NanoSpacing {
  // 0 * 4dp = 0dp
  val none: Dp = 0.dp
  // 1 * 4dp = 4dp
  val xs: Dp = 4.dp
  // 2 * 4dp = 8dp
  val sm: Dp = 8.dp
  // 4 * 4dp = 16dp
  val md: Dp = 16.dp
  // 6 * 4dp = 24dp
  val lg: Dp = 24.dp
  // 8 * 4dp = 32dp
  val xl: Dp = 32.dp
  // 12 * 4dp = 48dp (touch target minimum size for accessibility)
  val xxl: Dp = 48.dp
}

/**
 * Material Design 3 Corner Radius Scale. Provides consistent corner radii across UI components.
 * Reference: https://m3.material.io/foundations/components-basics/shape
 */
@Immutable
object NanoRadii {
  val small: Dp = 12.dp
  val medium: Dp = 18.dp
  val large: Dp = 24.dp
  val extraLarge: Dp = 32.dp
}

/**
 * Material Design 3 Elevation Scale. Used for shadows and depth hierarchy. Follows Material You
 * elevation levels. Reference: https://m3.material.io/foundations/design-tokens/elevation
 */
@Immutable
object NanoElevation {
  val level0: Dp = 0.dp
  val level1: Dp = 1.dp
  val level2: Dp = 3.dp
  val level3: Dp = 6.dp
  val level4: Dp = 10.dp
}

/**
 * Layout defaults that compose spacing tokens for common screen patterns. Ensures consistent
 * spacing hierarchy across all screens.
 */
@Immutable
object NanoLayoutDefaults {
  val ScreenPadding: PaddingValues =
    PaddingValues(horizontal = NanoSpacing.lg, vertical = NanoSpacing.lg)
  val ScreenVerticalSpacing: Dp = NanoSpacing.lg
  val SectionSpacing: Dp = NanoSpacing.md
  val ItemSpacing: Dp = NanoSpacing.sm
}
