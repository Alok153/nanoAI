package com.vjaykrsna.nanoai.feature.uiux.domain

import com.vjaykrsna.nanoai.feature.uiux.domain.UiUxDomainTestHelper.loadClass
import com.vjaykrsna.nanoai.feature.uiux.domain.UiUxDomainTestHelper.loadEnumConstant
import com.vjaykrsna.nanoai.feature.uiux.domain.UiUxDomainTestHelper.primaryConstructor
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests describing validation expectations for the upcoming UI/UX UserProfile domain model
 * (T030). These tests currently fail because the production class has not been implemented. Once
 * the model exists, it must enforce:
 * - displayName length ≤ 50 characters
 * - pinned tools count ≤ 10
 * - saved layouts may exceed legacy limits while preserving unique identifiers
 */
class UserProfileModelTest {
  @Test
  fun userProfile_displayNameLong_throws() {
    val ctor = userProfileConstructor()
    val args = defaultUserProfileArgs().apply { this[1] = "A".repeat(51) }

    val exception = assertFailsWith<InvocationTargetException> { ctor.newInstance(*args) }

    assertTrue(
      exception.cause is IllegalArgumentException,
      "Expected IllegalArgumentException when displayName exceeds 50 characters",
    )
  }

  @Test
  fun userProfile_pinnedToolsOverLimit_throws() {
    val ctor = userProfileConstructor()
    val args = defaultUserProfileArgs().apply { this[6] = List(11) { "tool-$it" } }

    val exception = assertFailsWith<InvocationTargetException> { ctor.newInstance(*args) }

    assertTrue(
      exception.cause is IllegalArgumentException,
      "Expected IllegalArgumentException when pinned tools exceed 10",
    )
  }

  @Test
  fun userProfile_allowsMoreThanFiveLayouts() {
    val ctor = userProfileConstructor()
    val layouts = List(6) { index -> UiUxDomainReflection.newLayoutSnapshot(id = "layout-$index") }
    val args = defaultUserProfileArgs().apply { this[7] = layouts }

    val instance = ctor.newInstance(*args)
    val savedLayouts = UiUxDomainReflection.getProperty(instance, "savedLayouts") as? List<*>
    assertTrue(savedLayouts?.size == 6, "Expected saved layouts to retain all six entries")
  }

  private fun userProfileConstructor() =
    primaryConstructor(loadClass("com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile"))

  private fun defaultUserProfileArgs(): Array<Any?> {
    val themePreference =
      loadEnumConstant("com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference", "LIGHT")
    val visualDensity =
      loadEnumConstant("com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity", "DEFAULT")
    val screenType =
      loadEnumConstant("com.vjaykrsna.nanoai.core.domain.model.uiux.ScreenType", "HOME")

    return arrayOf(
      "user-123",
      "Taylor",
      themePreference,
      visualDensity,
      screenType,
      false,
      listOf("tool-1", "tool-2"),
      listOf(UiUxDomainReflection.newLayoutSnapshot(id = "layout-default")),
    )
  }
}
