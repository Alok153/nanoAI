@file:Suppress("CyclomaticComplexMethod") // Complex test setup with reflection

package com.vjaykrsna.nanoai.feature.uiux.domain

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.fail
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class RecordOnboardingProgressUseCaseTest {
  @Test
  fun `records dismissed tips and completion flags`() = runTest {
    val spy = UserProfileRepositorySpy()
    spy.preferencesFlow.value =
      UiUxDomainReflection.newUiPreferences(
        onboardingCompleted = false,
        dismissedTips = emptyMap(),
      )

    val dispatcher = StandardTestDispatcher(testScheduler)
    val useCase =
      instantiateUseCase(
        className = "com.vjaykrsna.nanoai.feature.uiux.domain.RecordOnboardingProgressUseCase",
        repository = spy.asProxy(),
        dispatcher = dispatcher,
      )

    invokeOnboarding(useCase, tipId = "welcome", dismissed = true, completed = false)

    advanceUntilIdle()

    val intermediateRecord =
      spy.lastOnboardingRecord ?: fail("Expected onboarding record for dismissal")
    assertThat(intermediateRecord.first).containsExactlyEntriesIn(mapOf("welcome" to true))
    assertThat(intermediateRecord.second).isFalse()

    val updatedPrefs =
      spy.preferencesFlow.value ?: fail("Expected preferences emission after dismissal")

    @Suppress("UNCHECKED_CAST")
    val dismissedTips =
      UiUxDomainReflection.getProperty(updatedPrefs, "dismissedTips") as Map<String, Boolean>
    assertThat(dismissedTips).containsExactlyEntriesIn(mapOf("welcome" to true))
    val onboardingComplete =
      UiUxDomainReflection.getProperty(updatedPrefs, "onboardingCompleted") as Boolean
    assertThat(onboardingComplete).isFalse()

    invokeOnboarding(useCase, tipId = null, dismissed = false, completed = true)

    advanceUntilIdle()

    val finalPrefs =
      spy.preferencesFlow.value ?: fail("Expected preferences emission after completion")
    val finalCompleted =
      UiUxDomainReflection.getProperty(finalPrefs, "onboardingCompleted") as Boolean
    assertThat(finalCompleted).isTrue()
    @Suppress("UNCHECKED_CAST")
    val finalTips =
      UiUxDomainReflection.getProperty(finalPrefs, "dismissedTips") as Map<String, Boolean>
    assertThat(finalTips).containsKey("welcome")

    assertThat(spy.invocations.any { it.contains("onboarding", ignoreCase = true) }).isTrue()
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

  private fun invokeOnboarding(
    instance: Any,
    tipId: String?,
    dismissed: Boolean,
    completed: Boolean
  ) {
    val candidate =
      instance.javaClass.methods.firstOrNull { method ->
        method.parameterTypes.none { it.name.contains("Continuation") } &&
          method.parameterCount == 3 &&
          method.parameterTypes[0].name.contains("String") &&
          isBooleanType(method.parameterTypes[1]) &&
          isBooleanType(method.parameterTypes[2])
      }
        ?: fail(
          "Expected (String?, Boolean, Boolean) onboarding method on " + instance.javaClass.name,
        )
    candidate.invoke(instance, tipId, dismissed, completed)
  }

  private fun isBooleanType(type: Class<*>): Boolean =
    type == java.lang.Boolean.TYPE || type == java.lang.Boolean::class.java
}
