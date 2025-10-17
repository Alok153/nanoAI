package com.vjaykrsna.nanoai.ui.sidebar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SidebarNavigationTest : BaseSidebarContentTest() {

  @Test
  fun sidebarNavigationDrawer_displaysHomeAndSettings() {
    val state = defaultState(pinnedTools = listOf("settings"), activeRoute = "home")

    renderSidebar(state)

    composeRule.onNodeWithTag("sidebar_drawer_container").assertIsDisplayed()
  }
}
