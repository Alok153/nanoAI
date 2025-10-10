package com.vjaykrsna.nanoai.feature.uiux.domain

import java.lang.reflect.Constructor
import kotlin.test.fail

internal object UiUxDomainTestHelper {
  private const val DEFAULT_CONSTRUCTOR_MARKER = "kotlin.jvm.internal.DefaultConstructorMarker"

  fun loadClass(name: String): Class<*> =
    try {
      Class.forName(name)
    } catch (error: ClassNotFoundException) {
      // Test infrastructure exception - intentionally swallowed to provide clear test failure
      fail(
        "Missing class $name. Implement the corresponding UI/UX domain task before " +
          "running tests. Error: ${error.message}"
      )
    }

  fun loadEnumConstant(className: String, constant: String): Any {
    val clazz = loadClass(className)
    if (!clazz.isEnum) {
      fail("Expected $className to be an enum type")
    }
    @Suppress("UNCHECKED_CAST") val enumClass = clazz as Class<out Enum<*>>
    return try {
      java.lang.Enum.valueOf(enumClass, constant)
    } catch (error: IllegalArgumentException) {
      // Test infrastructure exception - intentionally swallowed to provide clear test failure
      fail(
        "Enum $className must expose constant $constant to satisfy UI/UX contracts. " +
          "Error: ${error.message}"
      )
    }
  }

  fun primaryConstructor(clazz: Class<*>): Constructor<*> =
    clazz.declaredConstructors
      .firstOrNull { constructor ->
        constructor.parameterTypes.none { it.name == DEFAULT_CONSTRUCTOR_MARKER }
      }
      ?.apply { isAccessible = true }
      ?: fail(
        "Expected ${clazz.name} to declare a primary constructor without " +
          "DefaultConstructorMarker mask"
      )
}
