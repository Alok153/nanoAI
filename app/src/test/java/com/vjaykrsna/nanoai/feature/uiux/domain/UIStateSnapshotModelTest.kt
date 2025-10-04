package com.vjaykrsna.nanoai.feature.uiux.domain

import com.vjaykrsna.nanoai.feature.uiux.domain.UiUxDomainTestHelper.loadClass
import com.vjaykrsna.nanoai.feature.uiux.domain.UiUxDomainTestHelper.primaryConstructor
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Validation-oriented tests for the UIStateSnapshot domain model (T032). Verifies recent action
 * rotation, expanded panel dedupe, and sidebar collapsed persistence.
 */
class UIStateSnapshotModelTest {
  @Test
  fun uiState_recentActionsTrimmedToFive() {
    val ctor = uiStateSnapshotConstructor()
    val args = defaultUiStateSnapshotArgs().apply { this[2] = List(6) { index -> "action-$index" } }

    val instance = ctor.newInstance(*args)
    val recent = getListProperty(instance, "getRecentActions")

    assertEquals(5, recent.size, "Recent actions should retain only the last five entries")
    assertEquals(
      listOf("action-1", "action-2", "action-3", "action-4", "action-5"),
      recent,
      "Recent actions should drop the oldest entry when exceeding five",
    )
  }

  @Test
  fun uiState_expandedPanelsDeduped() {
    val ctor = uiStateSnapshotConstructor()
    val args =
      defaultUiStateSnapshotArgs().apply { this[1] = listOf("panel-1", "panel-1", "panel-2") }

    val instance = ctor.newInstance(*args)
    val expanded = getListProperty(instance, "getExpandedPanels")

    assertEquals(listOf("panel-1", "panel-2"), expanded)
  }

  @Test
  fun uiState_sidebarCollapsedPersists() {
    val ctor = uiStateSnapshotConstructor()
    val args = defaultUiStateSnapshotArgs().apply { this[3] = true }

    val instance = ctor.newInstance(*args)
    val sidebarCollapsed =
      instance.javaClass.getMethod("isSidebarCollapsed").invoke(instance) as Boolean

    assertTrue(sidebarCollapsed)
  }

  @Test
  fun uiState_invalidUserId_throws() {
    val ctor = uiStateSnapshotConstructor()
    val args = defaultUiStateSnapshotArgs().apply { this[0] = "" }

    val exception = assertFailsWith<InvocationTargetException> { ctor.newInstance(*args) }

    assertTrue(
      exception.cause is IllegalArgumentException,
      "Expected IllegalArgumentException when userId is blank",
    )
  }

  private fun uiStateSnapshotConstructor() =
    primaryConstructor(
      loadClass("com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot"),
    )

  private fun defaultUiStateSnapshotArgs(): Array<Any?> =
    arrayOf(
      "user-123",
      listOf("panel-1"),
      listOf("action-0"),
      false,
    )

  private fun getListProperty(instance: Any, getterName: String): List<String> {
    val method = instance.javaClass.getMethod(getterName)
    @Suppress("UNCHECKED_CAST") return method.invoke(instance) as List<String>
  }
}
