package com.vjaykrsna.nanoai.feature.uiux.domain

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.fail

class ToggleCompactModeUseCaseTest {
    @Test
    fun `persists compact mode preference and updates layout snapshots`() =
        runTest {
            val spy = UserProfileRepositorySpy()
            val initialLayout = UiUxDomainReflection.newLayoutSnapshot(isCompact = false)
            val secondaryLayout = UiUxDomainReflection.newLayoutSnapshot(id = "layout-2", isCompact = false)
            spy.layoutSnapshotsFlow.value = listOf(initialLayout, secondaryLayout)
            spy.preferencesFlow.value =
                UiUxDomainReflection.newUiPreferences(
                    visualDensity = UiUxDomainReflection.visualDensity("DEFAULT"),
                )

            val dispatcher = StandardTestDispatcher(testScheduler)
            val useCase =
                instantiateUseCase(
                    className = "com.vjaykrsna.nanoai.feature.uiux.domain.ToggleCompactModeUseCase",
                    repository = spy.asProxy(),
                    dispatcher = dispatcher,
                )

            invokeToggle(useCase, enabled = true)

            advanceUntilIdle()

            assertThat(spy.lastCompactToggle).isTrue()
            val compactPrefs = spy.preferencesFlow.value ?: fail("Expected preferences emission for compact mode")
            val density = UiUxDomainReflection.getProperty(compactPrefs, "visualDensity")
            assertThat(density.toString()).isEqualTo("COMPACT")
            assertThat(spy.layoutSnapshotsFlow.value.all { UiUxDomainReflection.getProperty(it, "compact") as Boolean }).isTrue()

            invokeToggle(useCase, enabled = false)

            advanceUntilIdle()

            assertThat(spy.lastCompactToggle).isFalse()
            val defaultPrefs = spy.preferencesFlow.value ?: fail("Expected preferences emission for default mode")
            val densityDefault = UiUxDomainReflection.getProperty(defaultPrefs, "visualDensity")
            assertThat(densityDefault.toString()).isEqualTo("DEFAULT")
            assertThat(spy.layoutSnapshotsFlow.value.any { UiUxDomainReflection.getProperty(it, "compact") as Boolean }).isFalse()

            assertThat(spy.invocations.any { it.contains("compact", ignoreCase = true) }).isTrue()
        }

    private fun instantiateUseCase(
        className: String,
        repository: Any,
        dispatcher: CoroutineDispatcher,
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

    private fun invokeToggle(
        instance: Any,
        enabled: Boolean,
    ) {
        val method =
            instance.javaClass.methods.firstOrNull { method ->
                method.parameterTypes.none { it.name.contains("Continuation") } &&
                    method.parameterCount == 1 &&
                    isBooleanType(method.parameterTypes[0])
            } ?: fail("Expected single-boolean toggle method on ${instance.javaClass.name}")
        method.invoke(instance, enabled)
    }

    private fun isBooleanType(type: Class<*>): Boolean = type == java.lang.Boolean.TYPE || type == java.lang.Boolean::class.java
}
