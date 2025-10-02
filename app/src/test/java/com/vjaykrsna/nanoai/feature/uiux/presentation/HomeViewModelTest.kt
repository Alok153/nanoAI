package com.vjaykrsna.nanoai.feature.uiux.presentation

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.fail

/**
 * Placeholder UI/UX contract test for the future HomeViewModel (T053).
 *
 * The goal is to keep this test failing until the HomeViewModel exposes the
 * required state surface:
 *  - Ranked recommended actions (most recent first)
 *  - Offline banner visibility derived from connectivity state
 *  - Contextual tooltip surfacing for undiscovered features
 */
class HomeViewModelTest {
    @Test
    fun homeViewModel_exposesRankedActions_offlineBanner_andTooltips() =
        runTest {
            val className = "com.vjaykrsna.nanoai.feature.uiux.presentation.HomeViewModel"

            val viewModelClass =
                try {
                    Class.forName(className)
                } catch (error: ClassNotFoundException) {
                    fail("T053: Provide HomeViewModel at $className before marking T021 complete.")
                }

            val uiStateGetter =
                viewModelClass.methods.firstOrNull { it.name == "getUiState" }
                    ?: fail("T021: HomeViewModel must expose a `uiState` StateFlow for scenario orchestration.")

            val returnType = uiStateGetter.returnType.name
            if (!returnType.contains("StateFlow")) {
                fail("T021: HomeViewModel.uiState should be backed by StateFlow; found $returnType instead.")
            }

            fail(
                "T021: Implement recommended action ranking, offline banner synthesis, and tooltip surfacing before removing this sentinel failure.",
            )
        }
}
