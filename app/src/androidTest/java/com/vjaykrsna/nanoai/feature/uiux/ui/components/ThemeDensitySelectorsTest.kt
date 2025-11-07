package com.vjaykrsna.nanoai.feature.uiux.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeDensitySelectorsTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun unsupportedDensityInvokesCallback() {
    var selected by mutableStateOf(VisualDensity.DEFAULT)
    var unsupportedSelection: VisualDensity? = null

    composeRule.setContent {
      MaterialTheme {
        Surface {
          VisualDensityChips(
            selected = selected,
            onSelect = { selected = it },
            supportedDensities = listOf(VisualDensity.DEFAULT),
            onUnsupportedSelect = { unsupportedSelection = it },
            chipModifier = { density -> Modifier.testTag("density_${density.name}") },
          )
        }
      }
    }

    composeRule.onNodeWithTag("density_COMPACT").assertIsEnabled().performClick()

    composeRule.runOnIdle { assertEquals(VisualDensity.COMPACT, unsupportedSelection) }
  }

  @Test
  fun unsupportedDensityDisabledWhenNoCallback() {
    var selected by mutableStateOf(VisualDensity.DEFAULT)

    composeRule.setContent {
      MaterialTheme {
        Surface {
          VisualDensityChips(
            selected = selected,
            onSelect = { selected = it },
            supportedDensities = listOf(VisualDensity.DEFAULT),
            onUnsupportedSelect = null,
            chipModifier = { density -> Modifier.testTag("density_${density.name}") },
          )
        }
      }
    }

    composeRule.onNodeWithTag("density_COMPACT").assertIsNotEnabled()
  }
}
