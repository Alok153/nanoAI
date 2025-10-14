package com.vjaykrsna.nanoai.feature.uiux.data

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.daos.LayoutSnapshotDao
import com.vjaykrsna.nanoai.core.data.db.daos.UIStateSnapshotDao
import com.vjaykrsna.nanoai.core.data.db.daos.UserProfileDao
import com.vjaykrsna.nanoai.core.data.db.entities.LayoutSnapshotEntity
import com.vjaykrsna.nanoai.core.data.db.entities.UIStateSnapshotEntity
import com.vjaykrsna.nanoai.core.data.db.entities.UserProfileEntity
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferences
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferencesStore
import com.vjaykrsna.nanoai.core.domain.model.uiux.ScreenType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest

class UserProfileLocalDataSourceTest {
  private val userId = "user-123"

  @Test
  fun observeUserProfile_mergesPreferencesOverlay() = runTest {
    val profileDao = InMemoryUserProfileDao()
    val layoutDao = InMemoryLayoutSnapshotDao()
    val uiStateDao = InMemoryUiStateSnapshotDao()
    val (preferencesStore, preferencesFlow) = preferencesStore()

    profileDao.insert(
      UserProfileEntity(
        userId = userId,
        displayName = "Taylor",
        themePreference = ThemePreference.SYSTEM,
        visualDensity = VisualDensity.DEFAULT,
        lastOpenedScreen = ScreenType.HOME,
        compactMode = false,
        pinnedTools = listOf("one", "two"),
      ),
    )

    layoutDao.insertAll(
      listOf(
        LayoutSnapshotEntity(
          layoutId = "layout-1",
          userId = userId,
          name = "Default",
          lastOpenedScreen = "home",
          pinnedTools = listOf("alpha", "beta"),
          isCompact = false,
          position = 0,
        ),
      ),
    )

    preferencesFlow.value =
      preferencesFlow.value.copy(
        themePreference = ThemePreference.DARK,
        visualDensity = VisualDensity.COMPACT,
        pinnedToolIds = listOf("pref-1", "pref-2"),
      )

    val dataSource =
      UserProfileLocalDataSource(
        userProfileDao = profileDao,
        layoutSnapshotDao = layoutDao,
        uiStateSnapshotDao = uiStateDao,
        uiPreferencesStore = preferencesStore,
      )

    val result = dataSource.observeUserProfile(userId).first()

    assertThat(result).isNotNull()
    result!!
    assertThat(result.themePreference).isEqualTo(ThemePreference.DARK)
    assertThat(result.visualDensity).isEqualTo(VisualDensity.COMPACT)
    assertThat(result.compactMode).isTrue()
    assertThat(result.pinnedTools).containsExactly("pref-1", "pref-2").inOrder()
    assertThat(result.savedLayouts).hasSize(1)
  }

  @Test
  fun updateCompactMode_updatesPreferencesAndLayouts() = runTest {
    val profileDao = InMemoryUserProfileDao()
    val layoutDao = InMemoryLayoutSnapshotDao()
    val uiStateDao = InMemoryUiStateSnapshotDao()
    val (preferencesStore, preferencesFlow) = preferencesStore()

    profileDao.insert(
      UserProfileEntity(
        userId = userId,
        displayName = "Taylor",
        themePreference = ThemePreference.LIGHT,
        visualDensity = VisualDensity.DEFAULT,
        lastOpenedScreen = ScreenType.HOME,
        compactMode = false,
        pinnedTools = listOf("one", "two"),
      ),
    )

    layoutDao.insertAll(
      listOf(
        LayoutSnapshotEntity(
          layoutId = "layout-compact",
          userId = userId,
          name = "Workspace",
          lastOpenedScreen = "home",
          pinnedTools = listOf("a", "b", "c", "d", "e", "f", "g"),
          isCompact = false,
          position = 0,
        ),
      ),
    )

    val dataSource =
      UserProfileLocalDataSource(
        userProfileDao = profileDao,
        layoutSnapshotDao = layoutDao,
        uiStateSnapshotDao = uiStateDao,
        uiPreferencesStore = preferencesStore,
      )

    dataSource.updateCompactMode(userId, true)

    assertThat(preferencesFlow.value.visualDensity).isEqualTo(VisualDensity.COMPACT)

    val persistedProfile = profileDao.getById(userId)
    assertThat(persistedProfile?.compactMode).isTrue()
    assertThat(persistedProfile?.visualDensity).isEqualTo(VisualDensity.COMPACT)

    val updatedLayouts = layoutDao.getAllByUserId(userId)
    assertThat(updatedLayouts).hasSize(1)
    val layout = updatedLayouts.first()
    assertThat(layout.isCompact).isTrue()
    assertThat(layout.pinnedTools).containsExactly("a", "b", "c", "d", "e", "f").inOrder()
  }

  private fun preferencesStore(): Pair<UiPreferencesStore, MutableStateFlow<UiPreferences>> {
    val flow = MutableStateFlow(UiPreferences())
    val store = mockk<UiPreferencesStore>(relaxed = true)

    every { store.uiPreferences } returns flow
    coEvery { store.setThemePreference(any()) } answers
      {
        val theme = firstArg<ThemePreference>()
        flow.value = flow.value.copy(themePreference = theme)
        Unit
      }
    coEvery { store.setVisualDensity(any()) } answers
      {
        val density = firstArg<VisualDensity>()
        flow.value = flow.value.copy(visualDensity = density)
        Unit
      }
    coEvery { store.setPinnedToolIds(any()) } answers
      {
        val tools = firstArg<List<String>>()
        flow.value = flow.value.copy(pinnedToolIds = tools)
        Unit
      }
    coJustRun { store.setCommandPaletteRecents(any()) }
    coJustRun { store.recordCommandPaletteRecent(any()) }
    coJustRun { store.setConnectivityBannerDismissed(any()) }

    return store to flow
  }
}

private class InMemoryUserProfileDao : UserProfileDao {
  private val state = MutableStateFlow<UserProfileEntity?>(null)

  override fun observeById(userId: String): Flow<UserProfileEntity?> =
    state.map { entity -> entity?.takeIf { it.userId == userId } }

  override suspend fun getById(userId: String): UserProfileEntity? =
    state.value?.takeIf { it.userId == userId }

  override fun observeAll(): Flow<List<UserProfileEntity>> =
    state.map { entity -> entity?.let(::listOf) ?: emptyList() }

  override suspend fun insert(profile: UserProfileEntity) {
    state.value = profile
  }

  override suspend fun update(profile: UserProfileEntity): Int {
    state.value = profile
    return 1
  }

  override suspend fun deleteById(userId: String): Int {
    val removed = if (state.value?.userId == userId) 1 else 0
    if (removed == 1) state.value = null
    return removed
  }

  override suspend fun updateThemePreference(
    userId: String,
    themePreference: ThemePreference
  ): Int {
    val current = state.value ?: return 0
    if (current.userId != userId) return 0
    state.value = current.copy(themePreference = themePreference)
    return 1
  }

  override suspend fun updateVisualDensity(userId: String, visualDensity: VisualDensity): Int {
    val current = state.value ?: return 0
    if (current.userId != userId) return 0
    state.value = current.copy(visualDensity = visualDensity)
    return 1
  }

  override suspend fun updateLastOpenedScreen(userId: String, screenType: ScreenType): Int {
    val current = state.value ?: return 0
    if (current.userId != userId) return 0
    state.value = current.copy(lastOpenedScreen = screenType)
    return 1
  }

  override suspend fun updateCompactMode(userId: String, compactMode: Boolean): Int {
    val current = state.value ?: return 0
    if (current.userId != userId) return 0
    state.value = current.copy(compactMode = compactMode)
    return 1
  }

  override suspend fun updatePinnedTools(userId: String, pinnedTools: List<String>): Int {
    val current = state.value ?: return 0
    if (current.userId != userId) return 0
    state.value = current.copy(pinnedTools = pinnedTools)
    return 1
  }

  override suspend fun updateDisplayName(userId: String, displayName: String?): Int {
    val current = state.value ?: return 0
    if (current.userId != userId) return 0
    state.value = current.copy(displayName = displayName)
    return 1
  }
}

private class InMemoryLayoutSnapshotDao : LayoutSnapshotDao {
  private val state = MutableStateFlow<List<LayoutSnapshotEntity>>(emptyList())

  override fun observeByUserId(userId: String): Flow<List<LayoutSnapshotEntity>> =
    state.map { list ->
      list.filter { it.userId == userId }.sortedBy(LayoutSnapshotEntity::position)
    }

  override fun observeById(layoutId: String): Flow<LayoutSnapshotEntity?> =
    state.map { list -> list.firstOrNull { it.layoutId == layoutId } }

  override suspend fun getById(layoutId: String): LayoutSnapshotEntity? =
    state.value.firstOrNull { it.layoutId == layoutId }

  override suspend fun getAllByUserId(userId: String): List<LayoutSnapshotEntity> =
    state.value.filter { it.userId == userId }.sortedBy(LayoutSnapshotEntity::position)

  override suspend fun getCountByUserId(userId: String): Int =
    state.value.count { it.userId == userId }

  override suspend fun insert(snapshot: LayoutSnapshotEntity) {
    insertAll(listOf(snapshot))
  }

  override suspend fun insertAll(snapshots: List<LayoutSnapshotEntity>) {
    val existing = state.value.toMutableList()
    snapshots.forEach { snapshot ->
      existing.removeAll { it.layoutId == snapshot.layoutId }
      existing += snapshot
    }
    state.value = existing.sortedBy(LayoutSnapshotEntity::position)
  }

  override suspend fun update(snapshot: LayoutSnapshotEntity): Int {
    insert(snapshot)
    return 1
  }

  override suspend fun deleteById(layoutId: String): Int {
    val existing = state.value
    val updated = existing.filterNot { it.layoutId == layoutId }
    val removed = existing.size - updated.size
    state.value = updated
    return removed
  }

  override suspend fun deleteAllByUserId(userId: String): Int {
    val existing = state.value
    val updated = existing.filterNot { it.userId == userId }
    val removed = existing.size - updated.size
    state.value = updated
    return removed
  }

  override suspend fun updateName(layoutId: String, name: String): Int =
    throw UnsupportedOperationException("Not used in test")

  override suspend fun updatePinnedTools(layoutId: String, pinnedTools: List<String>): Int =
    throw UnsupportedOperationException("Not used in test")

  override suspend fun updateCompactMode(layoutId: String, isCompact: Boolean): Int =
    throw UnsupportedOperationException("Not used in test")

  override suspend fun updatePosition(layoutId: String, position: Int): Int =
    throw UnsupportedOperationException("Not used in test")

  override suspend fun reorderLayouts(userId: String, layoutIds: List<String>) =
    throw UnsupportedOperationException("Not used in test")

  override suspend fun getMaxPosition(userId: String): Int? =
    state.value.filter { it.userId == userId }.maxOfOrNull(LayoutSnapshotEntity::position)

  override suspend fun findAllLayouts(userId: String): List<LayoutSnapshotEntity> =
    getAllByUserId(userId)
}

private class InMemoryUiStateSnapshotDao : UIStateSnapshotDao {
  private val state = MutableStateFlow<UIStateSnapshotEntity?>(null)

  override fun observeByUserId(userId: String): Flow<UIStateSnapshotEntity?> =
    state.map { entity -> entity?.takeIf { it.userId == userId } }

  override suspend fun getByUserId(userId: String): UIStateSnapshotEntity? =
    state.value?.takeIf { it.userId == userId }

  override suspend fun insert(snapshot: UIStateSnapshotEntity) {
    state.value = snapshot
  }

  override suspend fun update(snapshot: UIStateSnapshotEntity): Int {
    state.value = snapshot
    return 1
  }

  override suspend fun deleteByUserId(userId: String): Int {
    val removed = if (state.value?.userId == userId) 1 else 0
    if (removed == 1) state.value = null
    return removed
  }

  override suspend fun updateSidebarCollapsed(userId: String, collapsed: Boolean): Int =
    throw UnsupportedOperationException("Not used in test")

  override suspend fun updateLeftDrawerOpen(userId: String, open: Boolean): Int =
    throw UnsupportedOperationException("Not used in test")

  override suspend fun updateRightDrawerState(userId: String, open: Boolean, panel: String?): Int =
    throw UnsupportedOperationException("Not used in test")

  override suspend fun updateActiveModeRoute(userId: String, route: String): Int =
    throw UnsupportedOperationException("Not used in test")

  override suspend fun updateCommandPaletteVisible(userId: String, visible: Boolean): Int =
    throw UnsupportedOperationException("Not used in test")

  override suspend fun updateExpandedPanels(userId: String, expandedPanels: List<String>): Int =
    throw UnsupportedOperationException("Not used in test")

  override suspend fun updateRecentActions(userId: String, recentActions: List<String>): Int =
    throw UnsupportedOperationException("Not used in test")

  override suspend fun clearRecentActions(userId: String): Int {
    val current = state.value ?: return 0
    if (current.userId != userId) return 0
    state.value = current.copy(recentActions = emptyList())
    return 1
  }

  override suspend fun clearExpandedPanels(userId: String): Int {
    val current = state.value ?: return 0
    if (current.userId != userId) return 0
    state.value = current.copy(expandedPanels = emptyList())
    return 1
  }
}
