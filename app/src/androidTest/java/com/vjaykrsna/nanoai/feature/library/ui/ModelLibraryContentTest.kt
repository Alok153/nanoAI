package com.vjaykrsna.nanoai.feature.library.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryTab
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibrarySections
import com.vjaykrsna.nanoai.shared.testing.TestingTheme
import org.junit.Rule
import org.junit.Test

class ModelLibraryContentTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun showsEmptyStateWhenNoSectionsHaveData() {
    composeTestRule.setContent {
      TestingTheme {
        ModelLibraryContent(
          sections = ModelLibrarySections(),
          selectedTab = ModelLibraryTab.CURATED,
          onDownload = {},
          onDelete = {},
          onPause = {},
          onResume = {},
          onCancel = {},
          onRetry = {},
          onImportLocalModel = null,
        )
      }
    }

    composeTestRule
      .onNodeWithText("No models to show", substring = false, useUnmergedTree = true)
      .assertExists()
  }
}
