@file:Suppress("CyclomaticComplexMethod") // Complex test setup with reflection

package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.uiux.domain.UiUxDomainReflection
import com.vjaykrsna.nanoai.feature.uiux.domain.UserProfileRepositorySpy
import com.vjaykrsna.nanoai.feature.uiux.domain.instantiateUiUxUseCase
import kotlin.test.Test
import kotlin.test.fail
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class HomeViewModelTest {
  @Test
  fun stateReflectsRecentActionsOfflineStatus_andTooltipDismissal() = runTest {
    val spy = UserProfileRepositorySpy()
    val profile = UiUxDomainReflection.newUserProfile(dismissedTips = emptyMap())
    spy.profileFlow.value = profile
    val uiStateSnapshot =
      UiUxDomainReflection.newUiStateSnapshot(recentActions = listOf("a", "b", "c"))
    spy.uiStateFlow.value = uiStateSnapshot
    spy.offlineStatusFlow.value = true

    val dispatcher = StandardTestDispatcher(testScheduler)
    val viewModel = instantiateHomeViewModel(spy, dispatcher)

    advanceUntilIdle()

    val uiState = resolveUiState(viewModel)
    val state = uiState.value
    assertThat(state.recentActions).containsExactly("a", "b", "c").inOrder()
    assertThat(state.offlineBannerVisible).isTrue()
    assertThat(state.tooltipEntryVisible).isTrue()

    spy.offlineStatusFlow.value = false
    advanceUntilIdle()
    assertThat(uiState.value.offlineBannerVisible).isFalse()

    invokeDismissTooltip(viewModel)
    advanceUntilIdle()

    assertThat(uiState.value.tooltipEntryVisible).isFalse()
    val onboardingRecord =
      spy.lastOnboardingRecord ?: fail("Expected onboarding record after dismiss")
    assertThat(onboardingRecord.first.keys).contains("home_tools_tip")
  }

  @Test
  fun toolsToggle_andRecentActionUpdateUiState() = runTest {
    val spy = UserProfileRepositorySpy()
    val profile = UiUxDomainReflection.newUserProfile(dismissedTips = emptyMap())
    spy.profileFlow.value = profile

    val dispatcher = StandardTestDispatcher(testScheduler)
    val viewModel = instantiateHomeViewModel(spy, dispatcher)

    advanceUntilIdle()

    val uiState = resolveUiState(viewModel)
    assertThat(uiState.value.toolsExpanded).isFalse()

    invokeToggleTools(viewModel)
    advanceUntilIdle()
    assertThat(uiState.value.toolsExpanded).isTrue()

    invokeRecentAction(viewModel, "action-1")
    advanceUntilIdle()
    assertThat(uiState.value.lastInteractedAction).isEqualTo("action-1")
    assertThat(uiState.value.latencyIndicatorVisible).isTrue()
  }

  private fun instantiateHomeViewModel(
    repository: UserProfileRepositorySpy,
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

    val clazz = Class.forName("com.vjaykrsna.nanoai.feature.uiux.presentation.HomeViewModel")
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
          typeName == CoroutineDispatcher::class.java.name ||
            typeName == "kotlinx.coroutines.CoroutineDispatcher" -> args += dispatcher
          typeName == "kotlinx.coroutines.CoroutineScope" -> args += TestScope(dispatcher)
          typeName == "kotlin.coroutines.CoroutineContext" -> args += dispatcher
          else -> supported = false
        }
      }
      if (supported && args.size == constructor.parameterCount) {
        return constructor.newInstance(*args.toTypedArray())
      }
    }
    fail("Unable to instantiate HomeViewModel with repository spies")
  }

  private fun resolveUiState(viewModel: Any): StateFlow<HomeUiState> {
    val method =
      viewModel.javaClass.methods.firstOrNull { method ->
        method.parameterCount == 0 &&
          StateFlow::class.java.isAssignableFrom(method.returnType) &&
          method.name.lowercase().contains("state")
      } ?: fail("HomeViewModel must expose a StateFlow state accessor")
    @Suppress("UNCHECKED_CAST") return method.invoke(viewModel) as StateFlow<HomeUiState>
  }

  private fun invokeToggleTools(viewModel: Any) {
    val method =
      viewModel.javaClass.methods.firstOrNull {
        it.name.contains("toggle", ignoreCase = true) && it.parameterCount == 0
      } ?: fail("Expected toggleToolsExpanded method on HomeViewModel")
    method.invoke(viewModel)
  }

  private fun invokeRecentAction(viewModel: Any, actionId: String) {
    val method =
      viewModel.javaClass.methods.firstOrNull { method ->
        method.parameterCount == 1 &&
          method.parameterTypes[0] == String::class.java &&
          method.name.contains("action", ignoreCase = true)
      } ?: fail("Expected onRecentAction(String) method on HomeViewModel")
    method.invoke(viewModel, actionId)
  }

  private fun invokeDismissTooltip(viewModel: Any) {
    val method =
      viewModel.javaClass.methods.firstOrNull {
        it.name.contains("dismiss", ignoreCase = true) && it.parameterCount == 0
      } ?: fail("Expected dismissTooltip method on HomeViewModel")
    method.invoke(viewModel)
  }
}
