package com.vjaykrsna.nanoai.core.data.repository.impl

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.uiux.UserProfileLocalDataSource
import com.vjaykrsna.nanoai.core.domain.model.uiux.LayoutSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UiPreferencesSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileRepositoryImplTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  private lateinit var localDataSource: UserProfileLocalDataSource
  private lateinit var repository: UserProfileRepositoryImpl

  private val userProfileFlow = MutableStateFlow<UserProfile?>(null)
  private val preferencesFlow = MutableStateFlow(UiPreferencesSnapshot())
  private val uiStateFlow = MutableStateFlow<UIStateSnapshot?>(null)

  @BeforeEach
  fun setUp() {
    localDataSource = mockk(relaxed = true)
    every { localDataSource.observeUserProfile(any()) } returns userProfileFlow
    every { localDataSource.observePreferences() } returns preferencesFlow
    every { localDataSource.observeUIStateSnapshot(any()) } returns uiStateFlow

    repository = UserProfileRepositoryImpl(local = localDataSource, ioDispatcher = dispatcher)
  }

  @Test
  fun `updateThemePreference should call local data source with DARK`() =
    runTest(dispatcher) {
      val userId = "testUser"
      repository.updateThemePreference(userId, "DARK")
      coVerify { localDataSource.updateThemePreference(userId, ThemePreference.DARK) }
    }

  @Test
  fun `updateThemePreference should call local data source with LIGHT`() =
    runTest(dispatcher) {
      val userId = "testUser"
      repository.updateThemePreference(userId, "LIGHT")
      coVerify { localDataSource.updateThemePreference(userId, ThemePreference.LIGHT) }
    }

  @Test
  fun `updateThemePreference should call local data source with SYSTEM`() =
    runTest(dispatcher) {
      val userId = "testUser"
      repository.updateThemePreference(userId, "SYSTEM")
      coVerify { localDataSource.updateThemePreference(userId, ThemePreference.SYSTEM) }
    }

  @Test
  fun `updateVisualDensity should call local data source with COMPACT`() =
    runTest(dispatcher) {
      val userId = "testUser"
      repository.updateVisualDensity(userId, "COMPACT")
      coVerify { localDataSource.updateVisualDensity(userId, VisualDensity.COMPACT) }
    }

  @Test
  fun `updateVisualDensity should default to DEFAULT for invalid value`() =
    runTest(dispatcher) {
      val userId = "testUser"
      repository.updateVisualDensity(userId, "INVALID")
      coVerify { localDataSource.updateVisualDensity(userId, VisualDensity.DEFAULT) }
    }

  @Test
  fun `updateCompactMode should call local data source with enabled true`() =
    runTest(dispatcher) {
      val userId = "testUser"
      repository.updateCompactMode(userId, true)
      coVerify { localDataSource.updateCompactMode(userId, true) }
    }

  @Test
  fun `updateCompactMode should call local data source with enabled false`() =
    runTest(dispatcher) {
      val userId = "testUser"
      repository.updateCompactMode(userId, false)
      coVerify { localDataSource.updateCompactMode(userId, false) }
    }

  @Test
  fun `updatePinnedTools should call local data source`() =
    runTest(dispatcher) {
      val userId = "testUser"
      val pinnedTools = listOf("tool1", "tool2")
      repository.updatePinnedTools(userId, pinnedTools)
      coVerify { localDataSource.updatePinnedTools(userId, pinnedTools) }
    }

  @Test
  fun `updateLeftDrawerOpen should call local data source`() =
    runTest(dispatcher) {
      val userId = "testUser"
      repository.updateLeftDrawerOpen(userId, true)
      coVerify { localDataSource.setLeftDrawerOpen(userId, true) }
    }

  @Test
  fun `updateRightDrawerState should call local data source`() =
    runTest(dispatcher) {
      val userId = "testUser"
      repository.updateRightDrawerState(userId, true, "testPanel")
      coVerify { localDataSource.setRightDrawerState(userId, true, "testPanel") }
    }

  @Test
  fun `updateActiveModeRoute should call local data source`() =
    runTest(dispatcher) {
      val userId = "testUser"
      repository.updateActiveModeRoute(userId, "testRoute")
      coVerify { localDataSource.setActiveModeRoute(userId, "testRoute") }
    }

  @Test
  fun `updateCommandPaletteVisibility should call local data source`() =
    runTest(dispatcher) {
      val userId = "testUser"
      repository.updateCommandPaletteVisibility(userId, true)
      coVerify { localDataSource.setCommandPaletteVisible(userId, true) }
    }

  @Test
  fun `recordCommandPaletteRecent should call local data source`() =
    runTest(dispatcher) {
      val commandId = "command-1"
      repository.recordCommandPaletteRecent(commandId)
      coVerify { localDataSource.recordCommandPaletteRecent(commandId) }
    }

  @Test
  fun `setCommandPaletteRecents should call local data source`() =
    runTest(dispatcher) {
      val commandIds = listOf("cmd1", "cmd2")
      repository.setCommandPaletteRecents(commandIds)
      coVerify { localDataSource.setCommandPaletteRecents(commandIds) }
    }

  @Test
  fun `setConnectivityBannerDismissed with null should call local data source`() =
    runTest(dispatcher) {
      repository.setConnectivityBannerDismissed(null)
      coVerify { localDataSource.setConnectivityBannerDismissed(null) }
    }

  @Test
  fun `setConnectivityBannerDismissed with instant should call local data source`() =
    runTest(dispatcher) {
      val now = Clock.System.now()
      repository.setConnectivityBannerDismissed(now)
      coVerify { localDataSource.setConnectivityBannerDismissed(any()) }
    }

  @Test
  fun `getUserProfile should call local data source`() =
    runTest(dispatcher) {
      val userId = "testUser"
      val expectedProfile = UserProfile.fromPreferences(userId, UiPreferencesSnapshot())
      coEvery { localDataSource.getUserProfile(userId) } returns expectedProfile

      val result = repository.getUserProfile(userId)

      assertThat(result).isEqualTo(expectedProfile)
      coVerify { localDataSource.getUserProfile(userId) }
    }

  @Test
  fun `observePreferences should return flow from local data source`() =
    runTest(dispatcher) {
      val expectedPrefs = UiPreferencesSnapshot(themePreference = ThemePreference.DARK)
      preferencesFlow.value = expectedPrefs

      val result = repository.observePreferences().first()

      assertThat(result).isEqualTo(expectedPrefs)
    }

  @Test
  fun `observeUIStateSnapshot should return flow from local data source`() =
    runTest(dispatcher) {
      val userId = "testUser"
      val snapshot =
        UIStateSnapshot(
          userId = userId,
          expandedPanels = emptyList(),
          recentActions = emptyList(),
          isSidebarCollapsed = false,
        )
      uiStateFlow.value = snapshot

      val result = repository.observeUIStateSnapshot(userId).first()

      assertThat(result).isEqualTo(snapshot)
    }

  @Test
  fun `observeOfflineStatus should return false by default`() =
    runTest(dispatcher) {
      val result = repository.observeOfflineStatus().first()
      assertThat(result).isFalse()
    }

  @Test
  fun `setOfflineOverride should update offline status`() =
    runTest(dispatcher) {
      repository.setOfflineOverride(true)
      val result = repository.observeOfflineStatus().first()
      assertThat(result).isTrue()
    }

  @Test
  fun `saveLayoutSnapshot should call local data source`() =
    runTest(dispatcher) {
      val userId = "testUser"
      val layout =
        LayoutSnapshot(
          id = "layout-1",
          name = "Test Layout",
          lastOpenedScreen = "home",
          pinnedTools = emptyList(),
          isCompact = false,
        )
      repository.saveLayoutSnapshot(userId, layout, 0)
      coVerify { localDataSource.saveLayoutSnapshot(userId, layout, 0) }
    }

  @Test
  fun `deleteLayoutSnapshot should call local data source`() =
    runTest(dispatcher) {
      val layoutId = "layout-1"
      repository.deleteLayoutSnapshot(layoutId)
      coVerify { localDataSource.deleteLayoutSnapshot(layoutId) }
    }
}
