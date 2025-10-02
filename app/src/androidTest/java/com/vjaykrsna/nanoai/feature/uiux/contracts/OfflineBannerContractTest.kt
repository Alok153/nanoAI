package com.vjaykrsna.nanoai.feature.uiux.contracts

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vjaykrsna.nanoai.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Contract test for the Offline banner (FR-006).
 * Ensures offline messaging, disabled-action affordance, and retry semantics are exposed.
 *
 * This test intentionally fails until the UI layer adds the required semantics.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class OfflineBannerContractTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun offlineBanner_displaysMessaging_andDisabledAffordance() {
        composeRule
            .onNodeWithTag("offline_banner_container")
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag("offline_banner_message")
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag("offline_banner_disabled_actions")
            .assertIsDisplayed()
    }

    @Test
    fun offlineBanner_retryAction_isAccessible() {
        composeRule
            .onNodeWithTag("offline_banner_retry")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeRule
            .onNodeWithTag("offline_banner_queue_status")
            .assertIsDisplayed()
    }
}
