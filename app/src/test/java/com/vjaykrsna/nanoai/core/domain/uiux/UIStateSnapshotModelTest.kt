package com.vjaykrsna.nanoai.core.domain.uiux

import android.os.Build
import com.vjaykrsna.nanoai.core.domain.uiux.UiUxDomainTestHelper.loadClass
import com.vjaykrsna.nanoai.core.domain.uiux.UiUxDomainTestHelper.primaryConstructor
import java.lang.reflect.InvocationTargetException
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Validation-oriented tests for the UIStateSnapshot domain model (T032). Verifies recent action
 * rotation, expanded panel dedupe, and sidebar collapsed persistence.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class UIStateSnapshotModelTest {
  @Test
  fun uiState_recentActionsTrimmedToFive() {
    val ctor = uiStateSnapshotConstructor()
    val args = defaultUiStateSnapshotArgs().apply { this[2] = List(6) { index -> "action-$index" } }

    val instance = ctor.newInstance(*args)
    val recent = getListProperty(instance, "getRecentActions")

    Assert.assertEquals("Recent actions should retain only the last five entries", 5, recent.size)
    Assert.assertEquals(
      "Recent actions should drop the oldest entry when exceeding five",
      listOf("action-1", "action-2", "action-3", "action-4", "action-5"),
      recent,
    )
  }

  @Test
  fun uiState_expandedPanelsDeduped() {
    val ctor = uiStateSnapshotConstructor()
    val args =
      defaultUiStateSnapshotArgs().apply { this[1] = listOf("panel-1", "panel-1", "panel-2") }

    val instance = ctor.newInstance(*args)
    val expanded = getListProperty(instance, "getExpandedPanels")

    Assert.assertEquals(listOf("panel-1", "panel-2"), expanded)
  }

  @Test
  fun uiState_sidebarCollapsedPersists() {
    val ctor = uiStateSnapshotConstructor()
    val args = defaultUiStateSnapshotArgs().apply { this[3] = true }

    val instance = ctor.newInstance(*args)
    val sidebarCollapsed =
      instance.javaClass.getMethod("isSidebarCollapsed").invoke(instance) as Boolean

    Assert.assertTrue(sidebarCollapsed)
  }

  @Test
  fun uiState_invalidUserId_throws() {
    val ctor = uiStateSnapshotConstructor()
    val args = defaultUiStateSnapshotArgs().apply { this[0] = "" }

    try {
      ctor.newInstance(*args)
      Assert.fail("Expected InvocationTargetException")
    } catch (exception: InvocationTargetException) {
      if (!(exception.cause is IllegalArgumentException)) {
        Assert.fail("Expected IllegalArgumentException when userId is blank")
      }
    }
  }

  private fun uiStateSnapshotConstructor() =
    primaryConstructor(loadClass("com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot"))

  private fun defaultUiStateSnapshotArgs(): Array<Any?> =
    arrayOf(
      "user-123",
      listOf("panel-1"),
      listOf("action-0"),
      false,
      false,
      false,
      "home",
      null,
      false,
    )

  private fun getListProperty(instance: Any, getterName: String): List<String> {
    val method = instance.javaClass.getMethod(getterName)
    val raw =
      method.invoke(instance) as? List<*>
        ?: error("Expected list return from $getterName on ${instance.javaClass.name}")
    return raw.map { it?.toString() ?: "" }
  }
}
