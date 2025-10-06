package com.vjaykrsna.nanoai.feature.uiux.presentation

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.uiux.domain.AnalyticsRecorder
import com.vjaykrsna.nanoai.feature.uiux.domain.UiUxDomainReflection
import com.vjaykrsna.nanoai.feature.uiux.domain.UserProfileRepositorySpy
import com.vjaykrsna.nanoai.feature.uiux.domain.instantiateUiUxUseCase
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.fail
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class WelcomeViewModelTest {
  @Test
  fun `uiState branches based on onboarding completion`() = runTest {
    val repository =
      UserProfileRepositorySpy().apply {
        val profile = UiUxDomainReflection.newUserProfile(onboardingCompleted = false)
        profileFlow.value = profile
        preferencesFlow.value = UiUxDomainReflection.newUiPreferences(onboardingCompleted = false)
      }
    val analytics = AnalyticsRecorder()
    val dispatcher = StandardTestDispatcher(testScheduler)
    val viewModel = instantiateViewModel(repository, analytics, dispatcher)

    advanceUntilIdle()

    val stateFlow = resolveStateFlow(viewModel)
    val initialState = stateFlow.value
    val onboardingVisible =
      resolveBoolean(
        initialState,
        "showOnboarding",
        "isOnboarding",
        "onboardingVisible",
        "shouldShowOnboarding"
      )
    assertThat(onboardingVisible).isTrue()

    repository.profileFlow.value = UiUxDomainReflection.newUserProfile(onboardingCompleted = true)
    repository.preferencesFlow.value =
      UiUxDomainReflection.newUiPreferences(onboardingCompleted = true)

    advanceUntilIdle()

    val updatedState = stateFlow.value
    val onboardingAfter =
      resolveBoolean(
        updatedState,
        "showOnboarding",
        "isOnboarding",
        "onboardingVisible",
        "shouldShowOnboarding"
      )
    assertThat(onboardingAfter).isFalse()
  }

  @Test
  fun `cta interactions emit analytics events`() = runTest {
    val repository =
      UserProfileRepositorySpy().apply {
        val profile = UiUxDomainReflection.newUserProfile(onboardingCompleted = false)
        profileFlow.value = profile
        preferencesFlow.value = UiUxDomainReflection.newUiPreferences(onboardingCompleted = false)
      }
    val analytics = AnalyticsRecorder()
    val dispatcher = StandardTestDispatcher(testScheduler)
    val viewModel = instantiateViewModel(repository, analytics, dispatcher)

    advanceUntilIdle()

    invokeMethod(viewModel, arrayOf("onGetStarted", "onGetStartedClick", "handleGetStarted"))
    invokeMethod(viewModel, arrayOf("onExploreFeatures", "onExplore", "handleExploreFeatures"))

    advanceUntilIdle()

    assertThat(
        analytics.events.any {
          it.contains("get", ignoreCase = true) && it.contains("start", ignoreCase = true)
        }
      )
      .isTrue()
    assertThat(analytics.events.any { it.contains("explore", ignoreCase = true) }).isTrue()
  }

  @Test
  fun `skip action becomes gated after invocation`() = runTest {
    val repository =
      UserProfileRepositorySpy().apply {
        val profile = UiUxDomainReflection.newUserProfile(onboardingCompleted = false)
        profileFlow.value = profile
        preferencesFlow.value = UiUxDomainReflection.newUiPreferences(onboardingCompleted = false)
      }
    val analytics = AnalyticsRecorder()
    val dispatcher = StandardTestDispatcher(testScheduler)
    val viewModel = instantiateViewModel(repository, analytics, dispatcher)

    advanceUntilIdle()

    val stateFlow = resolveStateFlow(viewModel)
    val beforeSkip = resolveBoolean(stateFlow.value, "skipEnabled", "isSkipEnabled", "canSkip")
    assertThat(beforeSkip).isTrue()

    invokeMethod(viewModel, arrayOf("onSkip", "onSkipOnboarding", "handleSkip"))

    advanceUntilIdle()

    val afterSkip = resolveBoolean(stateFlow.value, "skipEnabled", "isSkipEnabled", "canSkip")
    assertThat(afterSkip).isFalse()
  }

  private fun instantiateViewModel(
    repository: UserProfileRepositorySpy,
    analytics: AnalyticsRecorder,
    dispatcher: CoroutineDispatcher
  ): Any {
    val repositoryProxy = repository.asProxy()
    val observeUseCase =
      instantiateUiUxUseCase(
        "com.vjaykrsna.nanoai.feature.uiux.domain.ObserveUserProfileUseCase",
        repositoryProxy,
        dispatcher,
      )
    val recordUseCase =
      instantiateUiUxUseCase(
        "com.vjaykrsna.nanoai.feature.uiux.domain.RecordOnboardingProgressUseCase",
        repositoryProxy,
        dispatcher,
      )
    val updateThemeUseCase =
      instantiateUiUxUseCase(
        "com.vjaykrsna.nanoai.feature.uiux.domain.UpdateThemePreferenceUseCase",
        repositoryProxy,
        dispatcher,
      )
    val toggleCompactUseCase =
      instantiateUiUxUseCase(
        "com.vjaykrsna.nanoai.feature.uiux.domain.ToggleCompactModeUseCase",
        repositoryProxy,
        dispatcher,
      )

    val clazz = Class.forName("com.vjaykrsna.nanoai.feature.uiux.presentation.WelcomeViewModel")
    val constructors = clazz.constructors.sortedBy { it.parameterCount }
    constructors.forEach { constructor ->
      val args = mutableListOf<Any?>()
      var supported = true
      constructor.parameterTypes.forEach { parameter ->
        val typeName = parameter.name
        when {
          parameter.isAssignableFrom(observeUseCase.javaClass) ||
            typeName.contains("ObserveUserProfileUseCase") -> args += observeUseCase
          parameter.isAssignableFrom(recordUseCase.javaClass) ||
            typeName.contains("RecordOnboardingProgressUseCase") -> args += recordUseCase
          parameter.isAssignableFrom(updateThemeUseCase.javaClass) ||
            typeName.contains("UpdateThemePreferenceUseCase") -> args += updateThemeUseCase
          parameter.isAssignableFrom(toggleCompactUseCase.javaClass) ||
            typeName.contains("ToggleCompactModeUseCase") -> args += toggleCompactUseCase
          typeName.contains("SavedStateHandle") -> args += SavedStateHandle()
          typeName == CoroutineDispatcher::class.java.name ||
            typeName == "kotlinx.coroutines.CoroutineDispatcher" -> args += dispatcher
          typeName == "kotlinx.coroutines.CoroutineScope" -> args += TestScope(dispatcher)
          typeName == "kotlin.coroutines.CoroutineContext" -> args += dispatcher
          typeName.contains("Analytics", ignoreCase = true) ->
            args += analyticsProxy(parameter, analytics)
          typeName.contains("Navigator", ignoreCase = true) && parameter.isInterface ->
            args += createInterfaceProxy(parameter)
          parameter.isInterface -> args += createInterfaceProxy(parameter)
          parameter.isAssignableFrom(Boolean::class.java) -> args += false
          else -> {
            val default = instantiateDefault(parameter)
            if (default != null) {
              args += default
            } else {
              supported = false
            }
          }
        }
      }
      if (supported && args.size == constructor.parameterCount) {
        return constructor.newInstance(*args.toTypedArray())
      }
    }
    fail("Unable to instantiate WelcomeViewModel with available dependencies")
  }

  private fun resolveStateFlow(viewModel: Any): StateFlow<Any> {
    val stateFlowClass = StateFlow::class.java
    val method =
      viewModel.javaClass.methods.firstOrNull { method ->
        method.parameterCount == 0 &&
          stateFlowClass.isAssignableFrom(method.returnType) &&
          method.name.lowercase().contains("state")
      } ?: fail("Expected StateFlow state property on ${viewModel.javaClass.name}")
    val result = method.invoke(viewModel)
    return result as? StateFlow<Any>
      ?: fail("Expected StateFlow state property on ${viewModel.javaClass.name}")
  }

  private fun resolveBoolean(instance: Any, vararg propertyCandidates: String): Boolean {
    propertyCandidates.forEach { name ->
      val value = runCatching { getProperty(instance, name) }.getOrNull()
      if (value is Boolean) return value
    }
    fail(
      "Boolean property ${propertyCandidates.joinToString()} not found on " +
        "${instance.javaClass.name}"
    )
  }

  private fun getProperty(instance: Any, property: String): Any? {
    val capitalized =
      property.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    val candidates = listOf("get$capitalized", "is$capitalized")
    val method =
      candidates
        .asSequence()
        .mapNotNull { name -> instance.javaClass.methods.firstOrNull { it.name == name } }
        .firstOrNull() ?: error("Property $property not found on ${instance.javaClass.name}")
    return method.invoke(instance)
  }

  private fun invokeMethod(instance: Any, candidates: Array<String>) {
    val method =
      candidates.firstNotNullOfOrNull { candidate ->
        instance.javaClass.methods.firstOrNull { it.name == candidate && it.parameterCount == 0 }
      }
        ?: fail(
          "None of the method candidates ${candidates.joinToString()} exist on " +
            "${instance.javaClass.name}"
        )
    method.invoke(instance)
  }

  private fun createInterfaceProxy(type: Class<*>): Any =
    Proxy.newProxyInstance(
      type.classLoader,
      arrayOf(type),
    ) { _, method, args ->
      when (method.returnType) {
        Boolean::class.javaPrimitiveType,
        Boolean::class.java -> false
        Int::class.javaPrimitiveType,
        Int::class.java -> 0
        Long::class.javaPrimitiveType,
        Long::class.java -> 0L
        Float::class.javaPrimitiveType,
        Float::class.java -> 0f
        Double::class.javaPrimitiveType,
        Double::class.java -> 0.0
        String::class.java -> ""
        else -> null
      }
    }

  private fun analyticsProxy(parameter: Class<*>, recorder: AnalyticsRecorder): Any =
    if (parameter.isInterface) {
      recorder.asProxy(parameter)
    } else {
      instantiateDefault(parameter)
        ?: fail(
          "Analytics dependency ${parameter.name} must have default constructor or be an interface"
        )
    }

  private fun instantiateDefault(parameter: Class<*>): Any? =
    runCatching {
        val constructor = parameter.declaredConstructors.firstOrNull { it.parameterCount == 0 }
        constructor?.let {
          if (!it.isAccessible) it.isAccessible = true
          it.newInstance()
        }
      }
      .getOrNull()
}
