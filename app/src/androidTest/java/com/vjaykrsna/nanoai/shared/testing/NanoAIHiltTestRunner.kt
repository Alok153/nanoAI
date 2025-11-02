package com.vjaykrsna.nanoai.shared.testing

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Custom AndroidJUnitRunner that swaps the application used in instrumentation tests with the
 * Hilt-generated test application. This ensures @HiltAndroidTest classes can inject dependencies
 * without conflicting with the production @HiltAndroidApp application.
 */
class NanoAIHiltTestRunner : AndroidJUnitRunner() {
  override fun newApplication(
    cl: ClassLoader?,
    className: String?,
    context: Context?,
  ): Application {
    val testApplicationClass =
      "com.vjaykrsna.nanoai.shared.testing.NanoAITestApplication_Application"
    return super.newApplication(cl, testApplicationClass, context)
  }
}
