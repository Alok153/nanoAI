package com.vjaykrsna.nanoai.testing

import com.vjaykrsna.nanoai.BaseApplication
import dagger.hilt.android.testing.CustomTestApplication

/**
 * Custom test application for Hilt instrumentation tests.
 *
 * This allows Hilt tests to run without conflicting with the main @HiltAndroidApp application.
 */
@CustomTestApplication(BaseApplication::class) class NanoAITestApplication : BaseApplication()
