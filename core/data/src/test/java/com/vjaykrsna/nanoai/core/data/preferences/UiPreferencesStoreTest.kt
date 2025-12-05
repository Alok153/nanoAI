package com.vjaykrsna.nanoai.core.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UserPreferencesConstraints
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class UiPreferencesStoreTest {

  private lateinit var context: Context
  private lateinit var store: UiPreferencesStore

  @Before
  fun setUp() = runTest {
    context = ApplicationProvider.getApplicationContext()
    val converters =
      UiPreferencesConverters(
        Json {
          encodeDefaults = true
          ignoreUnknownKeys = true
        }
      )
    store = UiPreferencesStore(context, converters)
    store.reset()
  }

  @After
  fun tearDown() = runTest {
    if (::store.isInitialized) {
      store.reset()
    }
  }

  @Test
  fun setThemeAndDensityPersistValues() = runTest {
    store.setThemePreference(ThemePreference.DARK)
    store.setVisualDensity(VisualDensity.COMPACT)

    val prefs = store.uiPreferences.first()

    assertThat(prefs.themePreference).isEqualTo(ThemePreference.DARK)
    assertThat(prefs.visualDensity).isEqualTo(VisualDensity.COMPACT)
  }

  @Test
  fun setPinnedToolIdsEnforcesMaximumSize() = runTest {
    val tools = (0 until 12).map { "tool-$it" }

    store.setPinnedToolIds(tools)

    val prefs = store.uiPreferences.first()
    assertThat(prefs.pinnedToolIds).containsExactlyElementsIn(tools.take(10)).inOrder()
  }

  @Test
  fun setHighContrastEnabledPersistsFlag() = runTest {
    store.setHighContrastEnabled(true)
    var prefs = store.uiPreferences.first()
    assertThat(prefs.highContrastEnabled).isTrue()

    store.setHighContrastEnabled(false)
    prefs = store.uiPreferences.first()
    assertThat(prefs.highContrastEnabled).isFalse()
  }

  @Test
  fun recordCommandPaletteRecentMovesToFrontAndDeduplicates() = runTest {
    store.setCommandPaletteRecents(listOf("cmd-1", "cmd-2", "cmd-3"))
    store.recordCommandPaletteRecent("cmd-2")
    store.recordCommandPaletteRecent("cmd-4")

    var prefs = store.uiPreferences.first()
    assertThat(prefs.commandPaletteRecents.first()).isEqualTo("cmd-4")
    assertThat(prefs.commandPaletteRecents[1]).isEqualTo("cmd-2")
    assertThat(prefs.commandPaletteRecents).hasSize(4)

    repeat(UserPreferencesConstraints.MAX_RECENT_COMMANDS) { index ->
      store.recordCommandPaletteRecent("cmd-extra-$index")
    }

    prefs = store.uiPreferences.first()
    assertThat(prefs.commandPaletteRecents.first())
      .isEqualTo("cmd-extra-${UserPreferencesConstraints.MAX_RECENT_COMMANDS - 1}")
    assertThat(prefs.commandPaletteRecents.size)
      .isEqualTo(UserPreferencesConstraints.MAX_RECENT_COMMANDS)
  }

  @Test
  fun setConnectivityBannerDismissedStoresAndClearsInstant() = runTest {
    val dismissedAt = Clock.System.now()

    store.setConnectivityBannerDismissed(dismissedAt)
    var prefs = store.uiPreferences.first()
    assertThat(prefs.connectivityBannerLastDismissed).isEqualTo(dismissedAt)

    store.setConnectivityBannerDismissed(null)
    prefs = store.uiPreferences.first()
    assertThat(prefs.connectivityBannerLastDismissed).isNull()
  }

  @Test
  fun addPinnedToolIgnoresDuplicatesAndMaxLimit() = runTest {
    val base = (0 until 9).map { "tool-$it" }
    store.setPinnedToolIds(base)

    store.addPinnedTool("tool-9")
    store.addPinnedTool("tool-9")
    store.addPinnedTool("tool-overflow")

    val prefs = store.uiPreferences.first()

    assertThat(prefs.pinnedToolIds).containsExactlyElementsIn(base + "tool-9").inOrder()
    assertThat(prefs.pinnedToolIds).doesNotContain("tool-overflow")
  }

  @Test
  fun removePinnedToolDropsEntryWhenPresent() = runTest {
    store.setPinnedToolIds(listOf("tool-a", "tool-b", "tool-c"))

    store.removePinnedTool("tool-b")

    val prefs = store.uiPreferences.first()
    assertThat(prefs.pinnedToolIds).containsExactly("tool-a", "tool-c").inOrder()
  }
}
