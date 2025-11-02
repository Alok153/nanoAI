package com.vjaykrsna.nanoai.feature.uiux.navigation

import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId

/**
 * Central registry of navigation destinations for the unified shell.
 *
 * Each [Screen] exposes a canonical [route] used by the navigation controller and a set of
 * [deepLinks] that external entry points can target. Mode-linked screens also surface their
 * corresponding [modeId] so command palette actions and shell reducers stay in sync.
 */
sealed class Screen(
  val route: String,
  val deepLinks: List<String> = emptyList(),
  val modeId: ModeId? = null,
) {
  data object Home :
    Screen(route = "home", deepLinks = buildDeepLinks("home"), modeId = ModeId.HOME)

  data object Chat :
    Screen(route = "chat", deepLinks = buildDeepLinks("chat"), modeId = ModeId.CHAT)

  data object Image :
    Screen(route = "image", deepLinks = buildDeepLinks("image"), modeId = ModeId.IMAGE)

  data object ImageGallery :
    Screen(
      route = "image/gallery",
      deepLinks = buildDeepLinks("image/gallery"),
      modeId = ModeId.IMAGE,
    )

  data object Audio :
    Screen(route = "audio", deepLinks = buildDeepLinks("audio"), modeId = ModeId.AUDIO)

  data object Code :
    Screen(route = "code", deepLinks = buildDeepLinks("code"), modeId = ModeId.CODE)

  data object Translate :
    Screen(route = "translate", deepLinks = buildDeepLinks("translate"), modeId = ModeId.TRANSLATE)

  data object History :
    Screen(route = "history", deepLinks = buildDeepLinks("history"), modeId = ModeId.HISTORY)

  data object Library :
    Screen(route = "library", deepLinks = buildDeepLinks("library"), modeId = ModeId.LIBRARY)

  data object Tools :
    Screen(route = "tools", deepLinks = buildDeepLinks("tools"), modeId = ModeId.TOOLS)

  data object Settings :
    Screen(route = "settings", deepLinks = buildDeepLinks("settings"), modeId = ModeId.SETTINGS)

  data object SettingsAppearance :
    Screen(
      route = "settings/appearance",
      deepLinks = buildDeepLinks("settings/appearance"),
      modeId = ModeId.SETTINGS,
    )

  data object SettingsModels :
    Screen(
      route = "settings/models",
      deepLinks = buildDeepLinks("settings/models"),
      modeId = ModeId.SETTINGS,
    )

  data object HelpDocs : Screen(route = "help/docs", deepLinks = buildDeepLinks("help/docs"))

  data object HelpShortcuts :
    Screen(route = "help/shortcuts", deepLinks = buildDeepLinks("help/shortcuts"))

  data object CoverageDashboard :
    Screen(route = "coverage/dashboard", deepLinks = buildDeepLinks("coverage/dashboard"))

  companion object {
    /** Stable listing of screens for lookup helpers. */
    val all: List<Screen> =
      listOf(
        Home,
        Chat,
        Image,
        Audio,
        Code,
        Translate,
        History,
        Library,
        Tools,
        Settings,
        SettingsAppearance,
        SettingsModels,
        HelpDocs,
        HelpShortcuts,
        CoverageDashboard,
      )

    /** Finds the [Screen] that owns the provided [route] (supports nested segments). */
    fun fromRoute(route: String): Screen? {
      val exact = all.firstOrNull { screen -> screen.route == route }
      if (exact != null) return exact

      val normalized = route.substringBefore('/')
      return all.firstOrNull { screen ->
        screen.route == normalized || screen.route.startsWith("$normalized/")
      } ?: all.firstOrNull { screen -> route.startsWith(screen.route) }
    }

    /** Maps a [ModeId] to its canonical [Screen]. Defaults to [Home] when unknown. */
    fun fromModeId(modeId: ModeId): Screen =
      all.firstOrNull { screen -> screen.modeId == modeId } ?: Home
  }
}

private const val DEEP_LINK_SCHEME = "nanoai"
private const val DEEP_LINK_HOST = "shell"

private fun buildDeepLinks(route: String): List<String> =
  listOf("$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/$route", "$DEEP_LINK_SCHEME://$route")
