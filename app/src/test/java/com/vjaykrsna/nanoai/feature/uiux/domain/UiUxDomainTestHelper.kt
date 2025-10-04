package com.vjaykrsna.nanoai.feature.uiux.domain

import java.lang.reflect.Constructor
import kotlin.test.fail

internal object UiUxDomainTestHelper {
  private const val DEFAULT_CONSTRUCTOR_MARKER = "kotlin.jvm.internal.DefaultConstructorMarker"

  fun loadClass(name: String): Class<*> =
    try {
      Class.forName(name)
    } catch (error: ClassNotFoundException) {
      fail(
        "Missing class $name. Implement the corresponding UI/UX domain task before running tests."
      )
    }

  @Suppress("UNCHECKED_CAST")
  fun loadEnumConstant(className: String, constant: String): Any {
    val enumClass = loadClass(className) as Class<out Enum<*>>
    return try {
      java.lang.Enum.valueOf(enumClass, constant)
    } catch (error: IllegalArgumentException) {
      fail("Enum $className must expose constant $constant to satisfy UI/UX contracts.")
    }
  }

  fun primaryConstructor(clazz: Class<*>): Constructor<*> =
    clazz.declaredConstructors
      .firstOrNull { constructor ->
        constructor.parameterTypes.none { it.name == DEFAULT_CONSTRUCTOR_MARKER }
      }
      ?.apply { isAccessible = true }
      ?: fail(
        "Expected ${clazz.name} to declare a primary constructor without DefaultConstructorMarker mask"
      )
}
