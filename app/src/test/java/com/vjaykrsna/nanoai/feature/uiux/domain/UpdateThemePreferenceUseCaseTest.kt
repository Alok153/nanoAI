@file:Suppress("CyclomaticComplexMethod") // Complex test setup with reflection

package com.vjaykrsna.nanoai.feature.uiux.domain

import android.os.Build
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.fail
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class UpdateThemePreferenceUseCaseTest {
  @Test
  fun `updates theme preferences, syncs repository, and notifies observers`() = runTest {
    val spy = UserProfileRepositorySpy()
    spy.preferencesFlow.value =
      UiUxDomainReflection.newUiPreferences(
        themePreference = UiUxDomainReflection.themePreference("LIGHT"),
      )
    val dispatcher = StandardTestDispatcher(testScheduler)
    val useCase =
      instantiateUseCase(
        className = "com.vjaykrsna.nanoai.feature.uiux.domain.UpdateThemePreferenceUseCase",
        repository = spy.asProxy(),
        dispatcher = dispatcher,
      )

    val themeEmissions = mutableListOf<Any>()
    val themeJob = launch { spy.themeEvents.take(1).toList(themeEmissions) }

    val newTheme = UiUxDomainReflection.themePreference("DARK")
    invokeThemeUpdate(useCase, newTheme)

    advanceUntilIdle()

    val updatedPreferences = spy.preferencesFlow.value ?: fail("Expected preferences emission")
    val theme = UiUxDomainReflection.getProperty(updatedPreferences, "themePreference")
    assertThat(theme.toString()).isEqualTo("DARK")

    // No remote sync - only local theme update
    assertThat(spy.invocations.any { it.contains("theme", ignoreCase = true) }).isTrue()
    advanceUntilIdle()
    themeJob.cancel()

    assertThat(themeEmissions).isNotEmpty()
    assertThat(themeEmissions.first().toString()).isEqualTo("DARK")
  }

  private fun instantiateUseCase(
    className: String,
    repository: Any,
    dispatcher: CoroutineDispatcher
  ): Any {
    val clazz = UiUxDomainTestHelper.loadClass(className)
    val constructors = clazz.constructors.sortedBy { it.parameterCount }
    constructors.forEach { constructor ->
      val args = mutableListOf<Any?>()
      var supported = true
      constructor.parameterTypes.forEach { parameter ->
        when {
          parameter.isAssignableFrom(repository.javaClass) -> args += repository
          parameter.name.contains("UserProfileRepository") -> args += repository
          parameter.name == CoroutineDispatcher::class.java.name -> args += dispatcher
          parameter.name == "kotlinx.coroutines.CoroutineDispatcher" -> args += dispatcher
          parameter.name == "kotlinx.coroutines.CoroutineScope" -> args += TestScope(dispatcher)
          parameter.name == "kotlin.coroutines.CoroutineContext" -> args += dispatcher
          else -> supported = false
        }
      }
      if (supported && args.size == constructor.parameterCount) {
        return constructor.newInstance(*args.toTypedArray())
      }
    }
    fail("Unable to instantiate $className with repository/dispatcher test doubles")
  }

  private fun invokeThemeUpdate(instance: Any, theme: Any) {
    val method =
      instance.javaClass.methods.firstOrNull { method ->
        method.name == "updateTheme" &&
          method.parameterCount == 1 &&
          method.parameterTypes[0].name.contains("ThemePreference")
      }
        ?: instance.javaClass.methods.firstOrNull { method ->
          method.parameterCount >= 1 &&
            method.parameterTypes[0].name.contains("ThemePreference") &&
            method.parameterTypes.none { it.name.contains("Continuation") }
        }
        ?: fail("Expected non-suspend theme update method on ${instance.javaClass.name}")
    method.invoke(instance, theme)
  }
}
