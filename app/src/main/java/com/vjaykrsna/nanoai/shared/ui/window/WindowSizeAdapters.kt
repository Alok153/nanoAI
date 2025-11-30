package com.vjaykrsna.nanoai.shared.ui.window

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellWindowHeightClass
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellWindowSizeClass
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellWindowWidthClass

/** Converts Compose window metrics into the domain-friendly representation. */
fun WindowSizeClass.toShellWindowSizeClass(): ShellWindowSizeClass =
  ShellWindowSizeClass(
    widthSizeClass = widthSizeClass.toShellWidthClass(),
    heightSizeClass = heightSizeClass.toShellHeightClass(),
  )

private fun WindowWidthSizeClass.toShellWidthClass(): ShellWindowWidthClass =
  when (this) {
    WindowWidthSizeClass.Compact -> ShellWindowWidthClass.COMPACT
    WindowWidthSizeClass.Medium -> ShellWindowWidthClass.MEDIUM
    WindowWidthSizeClass.Expanded -> ShellWindowWidthClass.EXPANDED
    else -> ShellWindowWidthClass.MEDIUM
  }

private fun WindowHeightSizeClass.toShellHeightClass(): ShellWindowHeightClass =
  when (this) {
    WindowHeightSizeClass.Compact -> ShellWindowHeightClass.COMPACT
    WindowHeightSizeClass.Medium -> ShellWindowHeightClass.MEDIUM
    WindowHeightSizeClass.Expanded -> ShellWindowHeightClass.EXPANDED
    else -> ShellWindowHeightClass.MEDIUM
  }
