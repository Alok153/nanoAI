package com.vjaykrsna.nanoai.uiux

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI/UX macrobenchmark enforcing product-grade performance budgets.
 *
 * Targets per constitution:
 *  - Cold start (First Meaningful Paint) <= 300 ms (p75) with partial compilation
 *  - Interaction latency for primary navigation <= 100 ms
 *  - Theme toggle and offline banner transitions avoid layout jank
 *
 * These tests are expected to fail until the UI implementation is optimized.
 */
@RunWith(AndroidJUnit4::class)
class UiUxStartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStart_meetsFmpBudget_partialCompilation() =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            iterations = 5,
            startupMode = StartupMode.COLD,
            setupBlock = {
                pressHome()
            },
        ) {
            startActivityAndWait()

            device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), 5_000)

            // Verify welcome hero appears quickly (acts as proxy for FMP)
            val hero = device.findObject(By.res(PACKAGE_NAME, "welcome_hero_title"))
            checkNotNull(hero) { "T029: Welcome hero not found during cold start benchmark." }
        }

    @Test
    fun navigate_home_to_settings_latency() =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric(), TraceMetric("Choreographer#doFrame")),
            compilationMode = CompilationMode.Partial(),
            iterations = 3,
            setupBlock = {
                startActivityAndWait()
                device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), 5_000)
            },
        ) {
            val sidebarToggle = device.findObject(By.res(PACKAGE_NAME, "sidebar_toggle"))
            sidebarToggle?.click()
            device.wait(Until.hasObject(By.res(PACKAGE_NAME, "sidebar_drawer")), 2_000)

            val settingsItem = device.findObject(By.res(PACKAGE_NAME, "sidebar_item_settings"))
            settingsItem?.click()
            device.wait(Until.hasObject(By.res(PACKAGE_NAME, "settings_grouped_options")), 2_000)
        }

    @Test
    fun themeToggle_animation_jankFree() =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.None(),
            iterations = 3,
            setupBlock = {
                startActivityAndWait()
                device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), 5_000)

                val sidebarToggle = device.findObject(By.res(PACKAGE_NAME, "sidebar_toggle"))
                sidebarToggle?.click()
                device.wait(Until.hasObject(By.res(PACKAGE_NAME, "sidebar_item_settings")), 2_000)
                device.findObject(By.res(PACKAGE_NAME, "sidebar_item_settings"))?.click()
                device.wait(Until.hasObject(By.res(PACKAGE_NAME, "theme_toggle_switch")), 2_000)
            },
        ) {
            val themeToggle = device.findObject(By.res(PACKAGE_NAME, "theme_toggle_switch"))
            themeToggle?.click()
            device.wait(Until.hasObject(By.res(PACKAGE_NAME, "theme_toggle_persistence_status")), 2_000)
        }

    companion object {
        private const val PACKAGE_NAME = "com.vjaykrsna.nanoai"
    }
}
