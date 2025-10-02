package com.vjaykrsna.nanoai.feature.uiux.domain

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.fail

class ObserveUserProfileUseCaseTest {
    @Test
    fun `merges dao and datastore flows and hydrates from cache`() =
        runTest {
            val spy = UserProfileRepositorySpy()
            val cachedLayout = UiUxDomainReflection.newLayoutSnapshot(isCompact = true)
            val cachedProfile =
                UiUxDomainReflection.newUserProfile(
                    displayName = "Cached User",
                    themePreference = UiUxDomainReflection.themePreference("LIGHT"),
                    onboardingCompleted = false,
                    pinnedTools = listOf("tool-cache"),
                    savedLayouts = listOf(cachedLayout),
                )
            val cachedUiState =
                UiUxDomainReflection.newUiStateSnapshot(
                    recentActions = listOf("cache-action"),
                    sidebarCollapsed = true,
                )
            val cachedPreferences =
                UiUxDomainReflection.newUiPreferences(
                    themePreference = UiUxDomainReflection.themePreference("DARK"),
                    onboardingCompleted = true,
                    dismissedTips = mapOf("welcome" to true),
                    pinnedTools = listOf("tool-cache"),
                )

            spy.profileFlow.value = cachedProfile
            spy.layoutSnapshotsFlow.value = listOf(cachedLayout)
            spy.uiStateFlow.value = cachedUiState
            spy.preferencesFlow.value = cachedPreferences
            spy.offlineStatusFlow.value = true

            val dispatcher = StandardTestDispatcher(testScheduler)
            val useCase =
                instantiateUseCase(
                    className = "com.vjaykrsna.nanoai.feature.uiux.domain.ObserveUserProfileUseCase",
                    repository = spy.asProxy(),
                    dispatcher = dispatcher,
                )

            val resultFlow = extractResultFlow(useCase)
            val emissions = mutableListOf<Any>()
            val job =
                launch { resultFlow.take(2).toList(emissions) }

            advanceUntilIdle()

            val first = emissions.firstOrNull() ?: fail("Expected cached emission")
            assertCachedEmission(first)

            val remoteLayout =
                UiUxDomainReflection.newLayoutSnapshot(
                    id = "layout-remote",
                    name = "Focus Remote",
                    lastOpenedScreen = "home",
                    pinnedTools = listOf("tool-remote"),
                    isCompact = false,
                )
            val syncedProfile =
                UiUxDomainReflection.newUserProfile(
                    displayName = "Synced User",
                    themePreference = UiUxDomainReflection.themePreference("LIGHT"),
                    onboardingCompleted = true,
                    pinnedTools = listOf("tool-remote"),
                    savedLayouts = listOf(remoteLayout),
                )
            val syncedUiState =
                UiUxDomainReflection.newUiStateSnapshot(
                    recentActions = listOf("remote-action"),
                    sidebarCollapsed = false,
                )
            spy.layoutSnapshotsFlow.value = listOf(remoteLayout)
            spy.profileFlow.value = syncedProfile
            spy.uiStateFlow.value = syncedUiState
            spy.preferencesFlow.value =
                UiUxDomainReflection.copyUiPreferences(
                    spy.preferencesFlow.value,
                    themePreference = UiUxDomainReflection.themePreference("LIGHT"),
                    pinnedTools = listOf("tool-remote"),
                    dismissedTips = mapOf("welcome" to true, "home" to true),
                )
            spy.offlineStatusFlow.value = false

            advanceUntilIdle()

            val second = emissions.getOrNull(1) ?: fail("Expected synced emission")
            assertSyncedEmission(second)

            job.cancel()

            assertThat(spy.invocations.any { it.contains("refresh", ignoreCase = true) }).isTrue()
        }

    private fun assertCachedEmission(result: Any) {
        val profile = requireNotNull(getReference(result, "profile", "userProfile"))
        val displayName = UiUxDomainReflection.getProperty(profile, "displayName") as String?
        assertThat(displayName).isEqualTo("Cached User")

        val theme = UiUxDomainReflection.getProperty(profile, "themePreference")
        assertThat(theme.toString()).isEqualTo("DARK")

        val onboardingCompleted = UiUxDomainReflection.getProperty(profile, "onboardingCompleted") as Boolean
        assertThat(onboardingCompleted).isTrue()

        @Suppress("UNCHECKED_CAST")
        val dismissedTips = UiUxDomainReflection.getProperty(profile, "dismissedTips") as Map<String, Boolean>
        assertThat(dismissedTips).containsExactlyEntriesIn(mapOf("welcome" to true))

        @Suppress("UNCHECKED_CAST")
        val layoutSnapshots = getReference(result, "layoutSnapshots", "layouts") as? List<Any>
        val firstLayout = layoutSnapshots?.firstOrNull() ?: fail("Missing cached layout snapshot")
        val isCompact = UiUxDomainReflection.getProperty(firstLayout, "compact") as Boolean
        assertThat(isCompact).isTrue()

        val uiState = requireNotNull(getReference(result, "uiState", "state"))

        @Suppress("UNCHECKED_CAST")
        val recentActions = UiUxDomainReflection.getProperty(uiState, "recentActions") as List<String>
        assertThat(recentActions).containsExactly("cache-action")

        val hydratedFromCache = getBoolean(result, "hydratedFromCache", "fromCache")
        assertThat(hydratedFromCache).isTrue()

        val offline = getBoolean(result, "offline", "isOffline", "networkOffline")
        assertThat(offline).isTrue()
    }

    private fun assertSyncedEmission(result: Any) {
        val profile = requireNotNull(getReference(result, "profile", "userProfile"))
        val displayName = UiUxDomainReflection.getProperty(profile, "displayName") as String?
        assertThat(displayName).isEqualTo("Synced User")

        val theme = UiUxDomainReflection.getProperty(profile, "themePreference")
        assertThat(theme.toString()).isEqualTo("LIGHT")

        @Suppress("UNCHECKED_CAST")
        val pinnedTools = UiUxDomainReflection.getProperty(profile, "pinnedTools") as List<String>
        assertThat(pinnedTools).contains("tool-remote")

        @Suppress("UNCHECKED_CAST")
        val layoutSnapshots = getReference(result, "layoutSnapshots", "layouts") as? List<Any>
        val firstLayout = layoutSnapshots?.firstOrNull() ?: fail("Missing synced layout snapshot")
        val isCompact = UiUxDomainReflection.getProperty(firstLayout, "compact") as Boolean
        assertThat(isCompact).isFalse()

        val hydratedFromCache = getBoolean(result, "hydratedFromCache", "fromCache")
        assertThat(hydratedFromCache).isFalse()

        val offline = getBoolean(result, "offline", "isOffline", "networkOffline")
        assertThat(offline).isFalse()

        val uiState = requireNotNull(getReference(result, "uiState", "state"))

        @Suppress("UNCHECKED_CAST")
        val recentActions = UiUxDomainReflection.getProperty(uiState, "recentActions") as List<String>
        assertThat(recentActions).containsExactly("remote-action")
    }

    private fun extractResultFlow(instance: Any): Flow<Any> {
        val flowClass = Flow::class.java
        val method =
            instance.javaClass.methods.firstOrNull { method ->
                method.parameterCount == 0 && flowClass.isAssignableFrom(method.returnType)
            } ?: fail("Expected zero-argument Flow source on ${instance.javaClass.name}")
        @Suppress("UNCHECKED_CAST")
        return method.invoke(instance) as Flow<Any>
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

    private fun getReference(
        result: Any,
        vararg propertyCandidates: String,
    ): Any? {
        propertyCandidates.forEach { name ->
            runCatching { return UiUxDomainReflection.getProperty(result, name) }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun getBoolean(
        result: Any,
        vararg propertyCandidates: String,
    ): Boolean {
        propertyCandidates.forEach { name ->
            val value = runCatching { UiUxDomainReflection.getProperty(result, name) }.getOrNull()
            if (value is Boolean) return value
        }
        fail("Result ${result.javaClass.name} missing boolean properties ${propertyCandidates.joinToString()}")
    }
}
