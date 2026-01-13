package com.vjaykrsna.nanoai.core.data.uiux

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.daos.LayoutSnapshotDao
import com.vjaykrsna.nanoai.core.data.db.daos.UIStateSnapshotDao
import com.vjaykrsna.nanoai.core.data.db.daos.UserProfileDao
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferences
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferencesStore
import com.vjaykrsna.nanoai.core.domain.model.uiux.DataStoreUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.ScreenType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class UserProfileLocalDataSourceTest {

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension()

  private lateinit var userProfileDao: UserProfileDao
  private lateinit var layoutSnapshotDao: LayoutSnapshotDao
  private lateinit var uiStateSnapshotDao: UIStateSnapshotDao
  private lateinit var uiPreferencesStore: UiPreferencesStore
  private lateinit var dataSource: UserProfileLocalDataSource

  private val uiPreferencesFlow = MutableStateFlow(UiPreferences())

  @BeforeEach
  fun setUp() {
    userProfileDao = mockk(relaxed = true)
    layoutSnapshotDao = mockk(relaxed = true)
    uiStateSnapshotDao = mockk(relaxed = true)
    uiPreferencesStore = mockk(relaxed = true)
    every { uiPreferencesStore.uiPreferences } returns uiPreferencesFlow
    dataSource =
      UserProfileLocalDataSource(
        userProfileDao = userProfileDao,
        layoutSnapshotDao = layoutSnapshotDao,
        uiStateSnapshotDao = uiStateSnapshotDao,
        uiPreferencesStore = uiPreferencesStore,
      )
  }

  @Test
  fun `saveUserProfile should insert both the profile and layout snapshots`() = runTest {
    // Given
    val profile = UserProfile.fromPreferences("testUser", DataStoreUiPreferences())

    // When
    dataSource.saveUserProfile(profile)

    // Then
    coVerify { userProfileDao.insert(any()) }
    coVerify { layoutSnapshotDao.insertAll(any()) }
  }

  @Test
  fun `updateThemePreference should update both the store and the dao`() = runTest {
    // Given
    val userId = "testUser"
    val theme = ThemePreference.DARK

    // When
    dataSource.updateThemePreference(userId, theme)

    // Then
    coVerify { uiPreferencesStore.setThemePreference(theme) }
    coVerify { userProfileDao.updateThemePreference(userId, theme) }
  }

  @Test
  fun `updateVisualDensity should update both the store and the dao`() = runTest {
    // Given
    val userId = "testUser"
    val density = VisualDensity.COMPACT

    // When
    dataSource.updateVisualDensity(userId, density)

    // Then
    coVerify { uiPreferencesStore.setVisualDensity(density) }
    coVerify { userProfileDao.updateVisualDensity(userId, density) }
  }

  @Test
  fun `updatePinnedTools should update both the store and the dao`() = runTest {
    // Given
    val userId = "testUser"
    val pinnedTools = listOf("tool1", "tool2")

    // When
    dataSource.updatePinnedTools(userId, pinnedTools)

    // Then
    coVerify { uiPreferencesStore.setPinnedToolIds(pinnedTools) }
    coVerify { userProfileDao.updatePinnedTools(userId, pinnedTools) }
  }

  @Test
  fun `updateCompactMode should update store, dao, and layout snapshots`() = runTest {
    // Given
    val userId = "testUser"
    coEvery { layoutSnapshotDao.getAllByUserId(userId) } returns emptyList()

    // When
    dataSource.updateCompactMode(userId, true)

    // Then
    coVerify { uiPreferencesStore.setVisualDensity(VisualDensity.COMPACT) }
    coVerify { userProfileDao.updateVisualDensity(userId, VisualDensity.COMPACT) }
    coVerify { userProfileDao.updateCompactMode(userId, true) }
  }

  @Test
  fun `updateLastOpenedScreen should update dao`() = runTest {
    // Given
    val userId = "testUser"
    val screenType = ScreenType.SETTINGS

    // When
    dataSource.updateLastOpenedScreen(userId, screenType)

    // Then
    coVerify { userProfileDao.updateLastOpenedScreen(userId, screenType) }
  }

  @Test
  fun `deleteLayoutSnapshot should delete from dao`() = runTest {
    // Given
    val layoutId = "layout-123"

    // When
    dataSource.deleteLayoutSnapshot(layoutId)

    // Then
    coVerify { layoutSnapshotDao.deleteById(layoutId) }
  }

  @Test
  fun `updateSidebarCollapsed should update dao`() = runTest {
    // Given
    val userId = "testUser"

    // When
    dataSource.updateSidebarCollapsed(userId, true)

    // Then
    coVerify { uiStateSnapshotDao.updateSidebarCollapsed(userId, true) }
  }

  @Test
  fun `addRecentAction should update dao`() = runTest {
    // Given
    val userId = "testUser"
    val actionId = "action-1"

    // When
    dataSource.addRecentAction(userId, actionId)

    // Then
    coVerify { uiStateSnapshotDao.addRecentAction(userId, actionId) }
  }

  @Test
  fun `getCachedPreferences should return current preferences`() = runTest {
    // Given
    val preferences =
      UiPreferences(
        themePreference = ThemePreference.DARK,
        visualDensity = VisualDensity.DEFAULT,
        pinnedToolIds = emptyList(),
        commandPaletteRecents = emptyList(),
        connectivityBannerLastDismissed = null,
        highContrastEnabled = false,
      )
    uiPreferencesFlow.value = preferences

    // When
    val result = dataSource.getCachedPreferences()

    // Then
    assertThat(result.themePreference).isEqualTo(ThemePreference.DARK)
  }

  @Test
  fun `observePreferences should return flow of domain snapshots`() = runTest {
    // Given
    val preferences =
      UiPreferences(
        themePreference = ThemePreference.DARK,
        visualDensity = VisualDensity.DEFAULT,
        pinnedToolIds = emptyList(),
        commandPaletteRecents = emptyList(),
        connectivityBannerLastDismissed = null,
        highContrastEnabled = false,
      )
    uiPreferencesFlow.value = preferences

    // When
    val result = dataSource.observePreferences().first()

    // Then
    assertThat(result.themePreference).isEqualTo(ThemePreference.DARK)
  }

  @Test
  fun `recordCommandPaletteRecent should update store`() = runTest {
    // Given
    val commandId = "command-1"

    // When
    dataSource.recordCommandPaletteRecent(commandId)

    // Then
    coVerify { uiPreferencesStore.recordCommandPaletteRecent(commandId) }
  }

  @Test
  fun `setCommandPaletteRecents should update store`() = runTest {
    // Given
    val commandIds = listOf("cmd1", "cmd2")

    // When
    dataSource.setCommandPaletteRecents(commandIds)

    // Then
    coVerify { uiPreferencesStore.setCommandPaletteRecents(commandIds) }
  }

  @Test
  fun `setConnectivityBannerDismissed should update store with null`() = runTest {
    // When
    dataSource.setConnectivityBannerDismissed(null)

    // Then
    coVerify { uiPreferencesStore.setConnectivityBannerDismissed(null) }
  }
}
