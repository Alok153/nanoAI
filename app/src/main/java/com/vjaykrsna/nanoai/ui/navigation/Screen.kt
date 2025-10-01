package com.vjaykrsna.nanoai.ui.navigation

sealed class Screen(val route: String) {
    data object Chat : Screen("chat")
    data object ModelLibrary : Screen("library")
    data object Settings : Screen("settings")
}
