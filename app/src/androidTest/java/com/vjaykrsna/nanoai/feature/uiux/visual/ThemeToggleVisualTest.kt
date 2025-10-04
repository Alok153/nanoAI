package com.vjaykrsna.nanoai.feature.uiux.visual

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.ui.components.ThemeToggle
import com.vjaykrsna.nanoai.ui.theme.NanoAITheme
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Visual regression snapshots for the Theme toggle component.
 *
 * Captures both light and dark variants for manual accessibility review. Outputs are stored under
 * `<app internal storage>/files/visual/uiux/` and can be retrieved via adb pull.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ThemeToggleVisualTest {
  @get:Rule val composeRule = createComposeRule()

  private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
  private val outputDir: File by lazy {
    File(targetContext.filesDir, "visual/uiux").apply { mkdirs() }
  }

  @Test
  fun captureThemeToggleLightAndDark() {
    captureSnapshot(ThemePreference.LIGHT, "light")
    captureSnapshot(ThemePreference.DARK, "dark")
  }

  private fun captureSnapshot(theme: ThemePreference, suffix: String) {
    val file = File(outputDir, "theme-toggle-$suffix-${timestamp()}.png")

    composeRule.setContent {
      NanoAITheme(themePreference = theme, dynamicColor = false) {
        Box(
          modifier =
            Modifier.padding(24.dp).testTag(ROOT_TAG).background(MaterialTheme.colorScheme.surface),
        ) {
          ThemeToggle(currentTheme = theme, onThemeChange = {})
        }
      }
    }

    composeRule.waitForIdle()

    val node = composeRule.onNodeWithTag(ROOT_TAG)
    node.assertIsDisplayed()

    val bitmap = node.captureToImage().asAndroidBitmap()
    saveBitmap(bitmap, file)

    assertThat(file.exists()).isTrue()
    assertThat(file.length()).isGreaterThan(0L)
    Log.i(TAG, "Saved ThemeToggle snapshot to ${file.absolutePath}")
  }

  private fun saveBitmap(bitmap: Bitmap, file: File) {
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
  }

  private fun timestamp(): String =
    DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.US)
      .format(LocalDateTime.now(ZoneOffset.UTC))

  companion object {
    private const val TAG = "ThemeToggleVisualTest"
    private const val ROOT_TAG = "theme_toggle_visual_root"
  }
}
