package com.vjaykrsna.nanoai.core.data.repository.impl

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.daos.LayoutSnapshotDao
import com.vjaykrsna.nanoai.core.data.db.daos.UIStateSnapshotDao
import com.vjaykrsna.nanoai.core.data.db.daos.UserProfileDao
import com.vjaykrsna.nanoai.core.data.db.entities.LayoutSnapshotEntity
import com.vjaykrsna.nanoai.core.data.db.entities.UIStateSnapshotEntity
import com.vjaykrsna.nanoai.core.data.db.entities.UserProfileEntity
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferencesConverters
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferencesStore
import com.vjaykrsna.nanoai.core.domain.model.uiux.ScreenType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.feature.uiux.data.UserProfileLocalDataSource
import java.io.File
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val USER_ID = "user-123"

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileRepositoryImplTest {
  private val scheduler = TestCoroutineScheduler()
  private val dispatcher = StandardTestDispatcher(scheduler)
  private lateinit var context: Context
  private lateinit var userProfileDao: FakeUserProfileDao
  private lateinit var layoutSnapshotDao: FakeLayoutSnapshotDao
  private lateinit var uiStateSnapshotDao: FakeUiStateSnapshotDao
  private lateinit var preferencesStore: UiPreferencesStore
  private lateinit var repository: UserProfileRepositoryImpl

  @BeforeEach
  fun setUp() {
    context = TestContext()
    val json = Json {
      encodeDefaults = true
      ignoreUnknownKeys = true
    }
    preferencesStore = UiPreferencesStore(context, UiPreferencesConverters(json))
    runBlocking { preferencesStore.reset() }

    userProfileDao = FakeUserProfileDao()
    layoutSnapshotDao = FakeLayoutSnapshotDao()
    uiStateSnapshotDao = FakeUiStateSnapshotDao()

    val dataSource =
      UserProfileLocalDataSource(
        userProfileDao = userProfileDao,
        layoutSnapshotDao = layoutSnapshotDao,
        uiStateSnapshotDao = uiStateSnapshotDao,
        uiPreferencesStore = preferencesStore,
      )

    repository = UserProfileRepositoryImpl(dataSource, dispatcher)
  }

  @AfterEach
  fun tearDown() {
    runBlocking { preferencesStore.reset() }
  }

  @Test
  fun observeUserProfile_prefersDataStoreOverlay() =
    runTest(scheduler) {
      userProfileDao.insert(
        UserProfileEntity(
          userId = USER_ID,
          displayName = "Taylor",
          themePreference = ThemePreference.LIGHT,
          visualDensity = VisualDensity.DEFAULT,
          lastOpenedScreen = ScreenType.HOME,
          compactMode = false,
          pinnedTools = listOf("db-one", "db-two"),
        )
      )

      preferencesStore.setThemePreference(ThemePreference.DARK)
      preferencesStore.setVisualDensity(VisualDensity.COMPACT)
      preferencesStore.setPinnedToolIds(listOf("pref-one", "pref-two", "pref-three"))
      advanceUntilIdle()

      val profile = repository.observeUserProfile(USER_ID).first { it != null }!!

      assertThat(profile.themePreference).isEqualTo(ThemePreference.DARK)
      assertThat(profile.visualDensity).isEqualTo(VisualDensity.COMPACT)
      assertThat(profile.compactMode).isTrue()
      assertThat(profile.pinnedTools)
        .containsExactly("pref-one", "pref-two", "pref-three")
        .inOrder()
    }

  @Test
  fun updateCompactMode_updatesLayoutsAndPreferences() =
    runTest(scheduler) {
      userProfileDao.insert(
        UserProfileEntity(
          userId = USER_ID,
          displayName = "Taylor",
          themePreference = ThemePreference.SYSTEM,
          visualDensity = VisualDensity.DEFAULT,
          lastOpenedScreen = ScreenType.HOME,
          compactMode = false,
          pinnedTools = listOf("tool-1", "tool-2"),
        )
      )

      layoutSnapshotDao.insert(
        LayoutSnapshotEntity(
          layoutId = "layout-1",
          userId = USER_ID,
          name = "Workspace",
          lastOpenedScreen = "home",
          pinnedTools = listOf("a", "b", "c", "d", "e", "f", "g"),
          isCompact = false,
          position = 0,
        )
      )

      repository.updateCompactMode(USER_ID, true)
      advanceUntilIdle()

      val updatedProfile = userProfileDao.getById(USER_ID)
      requireNotNull(updatedProfile)
      assertThat(updatedProfile.compactMode).isTrue()
      assertThat(updatedProfile.visualDensity).isEqualTo(VisualDensity.COMPACT)

      val updatedLayout = layoutSnapshotDao.getAllByUserId(USER_ID).first()
      assertThat(updatedLayout.isCompact).isTrue()
      assertThat(updatedLayout.pinnedTools.size).isAtMost(VisualDensity.COMPACT_PINNED_TOOL_CAP)

      val preferences = preferencesStore.uiPreferences.first()
      assertThat(preferences.visualDensity).isEqualTo(VisualDensity.COMPACT)
    }

  @Test
  fun commandPaletteRecents_routeThroughPreferencesStore() =
    runTest(scheduler) {
      repository.setCommandPaletteRecents(listOf("alpha", "beta", "alpha", "gamma"))
      advanceUntilIdle()

      var preferences = preferencesStore.uiPreferences.first()
      assertThat(preferences.commandPaletteRecents)
        .containsExactly("alpha", "beta", "gamma")
        .inOrder()

      repository.recordCommandPaletteRecent("beta")
      advanceUntilIdle()

      preferences = preferencesStore.uiPreferences.first()
      assertThat(preferences.commandPaletteRecents.first()).isEqualTo("beta")
    }

  @Test
  fun updateProfile_validatesPinnedTools() =
    runTest(scheduler) {
      userProfileDao.insert(
        UserProfileEntity(
          userId = USER_ID,
          displayName = "Taylor",
          themePreference = ThemePreference.SYSTEM,
          visualDensity = VisualDensity.DEFAULT,
          lastOpenedScreen = ScreenType.HOME,
          compactMode = false,
          pinnedTools = listOf("db-one", "db-two"),
        )
      )

      val result = runCatching {
        repository.updatePinnedTools(USER_ID, listOf("primary-tool", " "))
      }

      assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
      advanceUntilIdle()

      val storedEntity = userProfileDao.getById(USER_ID)
      requireNotNull(storedEntity)
      assertThat(storedEntity.pinnedTools).containsExactly("db-one", "db-two").inOrder()

      val preferences = preferencesStore.uiPreferences.first()
      assertThat(preferences.pinnedToolIds).isEmpty()
    }

  @Test
  fun updateProfile_handlesThemePreferenceConflicts() =
    runTest(scheduler) {
      userProfileDao.insert(
        UserProfileEntity(
          userId = USER_ID,
          displayName = "Taylor",
          themePreference = ThemePreference.LIGHT,
          visualDensity = VisualDensity.DEFAULT,
          lastOpenedScreen = ScreenType.HOME,
          compactMode = false,
          pinnedTools = emptyList(),
        )
      )

      preferencesStore.setThemePreference(ThemePreference.SYSTEM)
      advanceUntilIdle()

      repository.updateThemePreference(USER_ID, "dark")
      advanceUntilIdle()

      val entity = userProfileDao.getById(USER_ID)
      requireNotNull(entity)
      assertThat(entity.themePreference).isEqualTo(ThemePreference.DARK)

      val preferences = preferencesStore.uiPreferences.first()
      assertThat(preferences.themePreference).isEqualTo(ThemePreference.DARK)
    }

  @Test
  fun deleteProfile_clearsLayoutSnapshots() =
    runTest(scheduler) {
      val layoutOne =
        LayoutSnapshotEntity(
          layoutId = "layout-1",
          userId = USER_ID,
          name = "Workspace",
          lastOpenedScreen = "home",
          pinnedTools = listOf("a", "b"),
          isCompact = false,
          position = 0,
        )
      val layoutTwo =
        LayoutSnapshotEntity(
          layoutId = "layout-2",
          userId = USER_ID,
          name = "Canvas",
          lastOpenedScreen = "canvas",
          pinnedTools = listOf("c"),
          isCompact = false,
          position = 1,
        )
      layoutSnapshotDao.insert(layoutOne)
      layoutSnapshotDao.insert(layoutTwo)

      repository.deleteLayoutSnapshot(layoutOne.layoutId)
      advanceUntilIdle()

      assertThat(layoutSnapshotDao.getById(layoutOne.layoutId)).isNull()
      assertThat(layoutSnapshotDao.getById(layoutTwo.layoutId)).isEqualTo(layoutTwo)
      val remaining = layoutSnapshotDao.getAllByUserId(USER_ID)
      assertThat(remaining).containsExactly(layoutTwo)
    }
}

private class TestContext : ContextWrapper(Application()) {
  private val baseDir =
    File(System.getProperty("java.io.tmpdir"), "nanoai-room-${UUID.randomUUID()}").apply {
      mkdirs()
      deleteOnExit()
    }

  override fun getApplicationContext(): Context = this

  override fun getPackageName(): String = "com.vjaykrsna.nanoai.test"

  override fun getDatabasePath(name: String?): File = File(baseDir, name ?: "room-db")

  override fun deleteDatabase(name: String?): Boolean = getDatabasePath(name).delete()

  override fun getFilesDir(): File = baseDir

  override fun getNoBackupFilesDir(): File = baseDir
}

private class FakeUserProfileDao : UserProfileDao {
  private val profilesState = MutableStateFlow<Map<String, UserProfileEntity>>(emptyMap())

  private val profiles: MutableMap<String, UserProfileEntity>
    get() = profilesState.value.toMutableMap()

  private fun updateProfiles(transform: (MutableMap<String, UserProfileEntity>) -> Unit) {
    val mutable = profiles
    transform(mutable)
    profilesState.value = mutable.toMap()
  }

  override fun observeById(userId: String): Flow<UserProfileEntity?> =
    profilesState.map { it[userId] }

  override suspend fun getById(userId: String): UserProfileEntity? = profilesState.value[userId]

  override fun observeAll(): Flow<List<UserProfileEntity>> =
    profilesState.map { it.values.sortedBy(UserProfileEntity::userId) }

  override suspend fun insert(profile: UserProfileEntity) {
    updateProfiles { it[profile.userId] = profile }
  }

  override suspend fun update(profile: UserProfileEntity): Int {
    var updated = 0
    updateProfiles {
      if (it.containsKey(profile.userId)) {
        it[profile.userId] = profile
        updated = 1
      }
    }
    return updated
  }

  override suspend fun deleteById(userId: String): Int {
    var removed = 0
    updateProfiles {
      if (it.remove(userId) != null) {
        removed = 1
      }
    }
    return removed
  }

  override suspend fun updateThemePreference(
    userId: String,
    themePreference: ThemePreference,
  ): Int = modify(userId) { it.copy(themePreference = themePreference) }

  override suspend fun updateVisualDensity(userId: String, visualDensity: VisualDensity): Int =
    modify(userId) { it.copy(visualDensity = visualDensity) }

  override suspend fun updateLastOpenedScreen(userId: String, screenType: ScreenType): Int =
    modify(userId) { it.copy(lastOpenedScreen = screenType) }

  override suspend fun updateCompactMode(userId: String, compactMode: Boolean): Int =
    modify(userId) { it.copy(compactMode = compactMode) }

  override suspend fun updatePinnedTools(userId: String, pinnedTools: List<String>): Int =
    modify(userId) { it.copy(pinnedTools = pinnedTools) }

  override suspend fun updateDisplayName(userId: String, displayName: String?): Int =
    modify(userId) { it.copy(displayName = displayName) }

  private inline fun modify(
    userId: String,
    crossinline block: (UserProfileEntity) -> UserProfileEntity,
  ): Int {
    var updated = 0
    updateProfiles {
      val current = it[userId]
      if (current != null) {
        it[userId] = block(current)
        updated = 1
      }
    }
    return updated
  }
}

private class FakeLayoutSnapshotDao : LayoutSnapshotDao {
  private val state = MutableStateFlow<Map<String, LayoutSnapshotEntity>>(emptyMap())

  private val snapshots: MutableMap<String, LayoutSnapshotEntity>
    get() = state.value.toMutableMap()

  private fun updateSnapshots(transform: (MutableMap<String, LayoutSnapshotEntity>) -> Unit) {
    val mutable = snapshots
    transform(mutable)
    state.value = mutable.toMap()
  }

  override fun observeByUserId(userId: String): Flow<List<LayoutSnapshotEntity>> =
    state.map { entries ->
      entries.values.filter { it.userId == userId }.sortedBy(LayoutSnapshotEntity::position)
    }

  override fun observeById(layoutId: String): Flow<LayoutSnapshotEntity?> =
    state.map { it[layoutId] }

  override suspend fun getById(layoutId: String): LayoutSnapshotEntity? = state.value[layoutId]

  override suspend fun getAllByUserId(userId: String): List<LayoutSnapshotEntity> =
    state.value.values.filter { it.userId == userId }.sortedBy(LayoutSnapshotEntity::position)

  override suspend fun getCountByUserId(userId: String): Int =
    state.value.values.count { it.userId == userId }

  override suspend fun insert(snapshot: LayoutSnapshotEntity) {
    updateSnapshots { it[snapshot.layoutId] = snapshot }
  }

  override suspend fun insertAll(snapshots: List<LayoutSnapshotEntity>) {
    updateSnapshots { map -> snapshots.forEach { map[it.layoutId] = it } }
  }

  override suspend fun update(snapshot: LayoutSnapshotEntity): Int =
    replace(snapshot.layoutId) { snapshot }

  override suspend fun deleteById(layoutId: String): Int = replace(layoutId) { null }

  override suspend fun deleteAllByUserId(userId: String): Int {
    var count = 0
    updateSnapshots {
      val iterator = it.iterator()
      while (iterator.hasNext()) {
        val entry = iterator.next()
        if (entry.value.userId == userId) {
          iterator.remove()
          count++
        }
      }
    }
    return count
  }

  override suspend fun updateName(layoutId: String, name: String): Int =
    replace(layoutId) { entity -> entity.copy(name = name) }

  override suspend fun updatePinnedTools(layoutId: String, pinnedTools: List<String>): Int =
    replace(layoutId) { entity -> entity.copy(pinnedTools = pinnedTools) }

  override suspend fun updateCompactMode(layoutId: String, isCompact: Boolean): Int =
    replace(layoutId) { entity -> entity.copy(isCompact = isCompact) }

  override suspend fun updatePosition(layoutId: String, position: Int): Int =
    replace(layoutId) { entity -> entity.copy(position = position) }

  override suspend fun reorderLayouts(userId: String, layoutIds: List<String>) {
    layoutIds.forEachIndexed { index, layoutId -> updatePosition(layoutId, index) }
  }

  override suspend fun getMaxPosition(userId: String): Int? =
    getAllByUserId(userId).maxOfOrNull(LayoutSnapshotEntity::position)

  override suspend fun findAllLayouts(userId: String): List<LayoutSnapshotEntity> =
    getAllByUserId(userId)

  private inline fun replace(
    key: String,
    crossinline transform: (LayoutSnapshotEntity) -> LayoutSnapshotEntity?,
  ): Int {
    var updated = 0
    updateSnapshots {
      val current = it[key]
      if (current != null) {
        val next = transform(current)
        if (next == null) {
          it.remove(key)
        } else {
          it[key] = next
        }
        updated = 1
      }
    }
    return updated
  }
}

private class FakeUiStateSnapshotDao : UIStateSnapshotDao {
  private val state = MutableStateFlow<Map<String, UIStateSnapshotEntity>>(emptyMap())

  private val snapshots: MutableMap<String, UIStateSnapshotEntity>
    get() = state.value.toMutableMap()

  private fun updateSnapshots(transform: (MutableMap<String, UIStateSnapshotEntity>) -> Unit) {
    val mutable = snapshots
    transform(mutable)
    state.value = mutable.toMap()
  }

  override fun observeByUserId(userId: String): Flow<UIStateSnapshotEntity?> =
    state.map { it[userId] }

  override suspend fun getByUserId(userId: String): UIStateSnapshotEntity? = state.value[userId]

  override suspend fun insert(snapshot: UIStateSnapshotEntity) {
    updateSnapshots { it[snapshot.userId] = snapshot }
  }

  override suspend fun update(snapshot: UIStateSnapshotEntity): Int =
    modify(snapshot.userId) { snapshot }

  override suspend fun deleteByUserId(userId: String): Int = modify(userId) { null }

  override suspend fun updateSidebarCollapsed(userId: String, collapsed: Boolean): Int =
    modify(userId) { it.copy(sidebarCollapsed = collapsed) }

  override suspend fun updateLeftDrawerOpen(userId: String, open: Boolean): Int =
    modify(userId) { it.copy(leftDrawerOpen = open) }

  override suspend fun updateRightDrawerState(userId: String, open: Boolean, panel: String?): Int =
    modify(userId) { it.copy(rightDrawerOpen = open, activeRightPanel = panel) }

  override suspend fun updateActiveModeRoute(userId: String, route: String): Int =
    modify(userId) { it.copy(activeMode = route) }

  override suspend fun updateCommandPaletteVisible(userId: String, visible: Boolean): Int =
    modify(userId) { it.copy(paletteVisible = visible) }

  override suspend fun updateExpandedPanels(userId: String, expandedPanels: List<String>): Int =
    modify(userId) { it.copy(expandedPanels = expandedPanels) }

  override suspend fun updateRecentActions(userId: String, recentActions: List<String>): Int =
    modify(userId) { it.copy(recentActions = recentActions.take(5)) }

  override suspend fun addExpandedPanel(userId: String, panelId: String) {
    val updated =
      modify(userId) { entity ->
        if (entity.expandedPanels.contains(panelId)) entity
        else entity.copy(expandedPanels = entity.expandedPanels + panelId)
      }
    if (updated == 0) {
      insert(
        UIStateSnapshotEntity(
          userId = userId,
          expandedPanels = listOf(panelId),
          recentActions = emptyList(),
          sidebarCollapsed = false,
          leftDrawerOpen = false,
          rightDrawerOpen = false,
          activeMode = UIStateSnapshot.DEFAULT_MODE_ROUTE,
          activeRightPanel = null,
          paletteVisible = false,
        )
      )
    }
  }

  override suspend fun removeExpandedPanel(userId: String, panelId: String) {
    modify(userId) { entity -> entity.copy(expandedPanels = entity.expandedPanels - panelId) }
  }

  override suspend fun addRecentAction(userId: String, actionId: String) {
    val updated =
      modify(userId) { entity ->
        val updated =
          buildList {
              add(actionId)
              entity.recentActions.filterTo(this) { it != actionId }
            }
            .take(5)
        entity.copy(recentActions = updated)
      }
    if (updated == 0) {
      insert(
        UIStateSnapshotEntity(
          userId = userId,
          expandedPanels = emptyList(),
          recentActions = listOf(actionId),
          sidebarCollapsed = false,
          leftDrawerOpen = false,
          rightDrawerOpen = false,
          activeMode = UIStateSnapshot.DEFAULT_MODE_ROUTE,
          activeRightPanel = null,
          paletteVisible = false,
        )
      )
    }
  }

  override suspend fun clearRecentActions(userId: String): Int =
    updateRecentActions(userId, emptyList())

  override suspend fun clearExpandedPanels(userId: String): Int =
    updateExpandedPanels(userId, emptyList())

  override suspend fun resetToDefaults(userId: String) {
    modify(userId) {
      it.copy(
        expandedPanels = emptyList(),
        recentActions = emptyList(),
        sidebarCollapsed = false,
        leftDrawerOpen = false,
        rightDrawerOpen = false,
        activeMode = UIStateSnapshot.DEFAULT_MODE_ROUTE,
        activeRightPanel = null,
        paletteVisible = false,
      )
    }
  }

  private inline fun modify(
    userId: String,
    crossinline transform: (UIStateSnapshotEntity) -> UIStateSnapshotEntity?,
  ): Int {
    var updated = 0
    updateSnapshots {
      val current = it[userId]
      if (current != null) {
        val next = transform(current)
        if (next == null) {
          it.remove(userId)
        } else {
          it[userId] = next
        }
        updated = 1
      }
    }
    return updated
  }
}
