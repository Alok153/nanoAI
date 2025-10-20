package com.vjaykrsna.nanoai.feature.uiux.domain

import com.vjaykrsna.nanoai.feature.uiux.domain.UiUxDomainTestHelper.loadClass
import com.vjaykrsna.nanoai.feature.uiux.domain.UiUxDomainTestHelper.primaryConstructor
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Validation tests for the forthcoming LayoutSnapshot domain model (T031). Ensures layout names,
 * pinned tool limits, and compact-mode consistency are enforced.
 */
class LayoutSnapshotModelTest {
  @Test
  fun layoutSnapshot_nameLong_throws() {
    val ctor = layoutSnapshotConstructor()
    val args = defaultLayoutSnapshotArgs().apply { this[1] = "N".repeat(65) }

    val exception = assertFailsWith<InvocationTargetException> { ctor.newInstance(*args) }

    assertTrue(
      exception.cause is IllegalArgumentException,
      "Expected IllegalArgumentException when layout name exceeds 64 characters",
    )
  }

  @Test
  fun layoutSnapshot_pinnedToolsOverCap_throws() {
    val ctor = layoutSnapshotConstructor()
    val args = defaultLayoutSnapshotArgs().apply { this[3] = List(11) { "tool-$it" } }

    val exception = assertFailsWith<InvocationTargetException> { ctor.newInstance(*args) }

    assertTrue(
      exception.cause is IllegalArgumentException,
      "Expected IllegalArgumentException when pinned tools exceed 10",
    )
  }

  @Test
  fun layoutSnapshot_compactFlag_requiresSmallerPinnedSet() {
    val ctor = layoutSnapshotConstructor()
    val args =
      defaultLayoutSnapshotArgs().apply {
        this[3] = List(7) { "tool-$it" }
        this[4] = true
      }

    val exception = assertFailsWith<InvocationTargetException> { ctor.newInstance(*args) }

    assertTrue(
      exception.cause is IllegalArgumentException,
      "Expected IllegalArgumentException when compact layouts pin more than six tools",
    )
  }

  @Test
  fun layoutSnapshot_validInputs_preserveFields() {
    val ctor = layoutSnapshotConstructor()
    val args = defaultLayoutSnapshotArgs()
    val instance = assertValidInstance(ctor.newInstance(*args))

    assertEquals("layout-1", instance.first)
    assertEquals(false, instance.second)
  }

  private fun layoutSnapshotConstructor() =
    primaryConstructor(loadClass("com.vjaykrsna.nanoai.core.domain.model.uiux.LayoutSnapshot"))

  private fun defaultLayoutSnapshotArgs(): Array<Any?> =
    arrayOf("layout-1", "Daily Focus", "home_screen", listOf("tool-1", "tool-2"), false)

  private fun assertValidInstance(instance: Any): Pair<String, Boolean> {
    val clazz = instance.javaClass
    val id = clazz.getMethod("getId").invoke(instance) as? String ?: ""
    val isCompact = clazz.getMethod("isCompact").invoke(instance) as? Boolean ?: false
    return id to isCompact
  }
}
