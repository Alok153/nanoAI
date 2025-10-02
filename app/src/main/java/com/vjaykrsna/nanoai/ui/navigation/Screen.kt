package com.vjaykrsna.nanoai.ui.navigation

sealed class Screen(
    val route: String,
) {
    data object Welcome : Screen("welcome")

    data object Home : Screen("home")

    data object Chat : Screen("chat")

    data object ModelLibrary : Screen("library")

    data object Settings : Screen("settings")
}
