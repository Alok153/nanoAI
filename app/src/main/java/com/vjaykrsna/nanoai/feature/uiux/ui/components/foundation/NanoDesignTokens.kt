package com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
object NanoSpacing {
  val none: Dp = 0.dp
  val xs: Dp = 4.dp
  val sm: Dp = 8.dp
  val md: Dp = 16.dp
  val lg: Dp = 24.dp
  val xl: Dp = 32.dp
  val xxl: Dp = 48.dp
}

@Immutable
object NanoRadii {
  val small: Dp = 12.dp
  val medium: Dp = 18.dp
  val large: Dp = 24.dp
  val extraLarge: Dp = 32.dp
}

@Immutable
object NanoElevation {
  val level0: Dp = 0.dp
  val level1: Dp = 1.dp
  val level2: Dp = 3.dp
  val level3: Dp = 6.dp
  val level4: Dp = 10.dp
}

@Immutable
object NanoLayoutDefaults {
  val ScreenPadding: PaddingValues =
    PaddingValues(
      horizontal = NanoSpacing.lg,
      vertical = NanoSpacing.lg,
    )
  val ScreenVerticalSpacing: Dp = NanoSpacing.lg
  val SectionSpacing: Dp = NanoSpacing.md
  val ItemSpacing: Dp = NanoSpacing.sm
}
