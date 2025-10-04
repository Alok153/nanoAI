@file:Suppress("FunctionName")

package com.vjaykrsna.nanoai.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vjaykrsna.nanoai.feature.chat.ui.ChatScreen
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryScreen
import com.vjaykrsna.nanoai.feature.settings.presentation.FirstLaunchDisclaimerUiState
import com.vjaykrsna.nanoai.feature.settings.presentation.FirstLaunchDisclaimerViewModel
import com.vjaykrsna.nanoai.feature.settings.ui.FirstLaunchDisclaimerDialog
import com.vjaykrsna.nanoai.feature.settings.ui.SettingsScreen
import com.vjaykrsna.nanoai.feature.sidebar.presentation.SidebarViewModel
import com.vjaykrsna.nanoai.feature.uiux.presentation.AppUiState
import com.vjaykrsna.nanoai.feature.uiux.presentation.HomeViewModel
import com.vjaykrsna.nanoai.feature.uiux.presentation.WelcomeViewModel
import com.vjaykrsna.nanoai.feature.uiux.ui.HomeScreen
import com.vjaykrsna.nanoai.feature.uiux.ui.HomeScreenCallbacks
import com.vjaykrsna.nanoai.feature.uiux.ui.HomeTooltipCallbacks
import com.vjaykrsna.nanoai.feature.uiux.ui.WelcomePrimaryActions
import com.vjaykrsna.nanoai.feature.uiux.ui.WelcomeScreen
import com.vjaykrsna.nanoai.feature.uiux.ui.WelcomeTooltipActions
import com.vjaykrsna.nanoai.ui.sidebar.SidebarContent
import com.vjaykrsna.nanoai.ui.sidebar.SidebarInteractions
import com.vjaykrsna.nanoai.ui.sidebar.SidebarUiState
import kotlinx.coroutines.launch

private data class NavigationUiState(
  val currentRoute: String?,
  val isWelcomeRoute: Boolean,
)

private data class NavigationLayoutState(
  val navigation: NavigationUiState,
  val sidebar: SidebarUiState,
  val disclaimer: FirstLaunchDisclaimerUiState,
  val drawerState: DrawerState,
  val hasBackStackEntry: Boolean,
)

private data class NavigationHandlers(
  val sidebarInteractions: SidebarInteractions,
  val onNavigate: (String) -> Unit,
  val onDrawerToggle: () -> Unit,
  val onCloseDrawer: () -> Unit,
  val onDisclaimerAcknowledge: () -> Unit,
  val onDisclaimerDismiss: () -> Unit,
  val onRouteVisit: (String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScaffold(
  modifier: Modifier = Modifier,
  appState: AppUiState = AppUiState(),
  navController: NavHostController = rememberNavController(),
  sidebarViewModel: SidebarViewModel = hiltViewModel(),
  disclaimerViewModel: FirstLaunchDisclaimerViewModel = hiltViewModel(),
) {
  val (layoutState, handlers) =
    rememberNavigationResources(
      navController = navController,
      sidebarViewModel = sidebarViewModel,
      disclaimerViewModel = disclaimerViewModel,
    )

  HandleNavigationEffects(
    appState = appState,
    navController = navController,
    layoutState = layoutState,
    onRouteVisit = handlers.onRouteVisit,
  )

  NavigationScaffoldContent(
    state = layoutState,
    handlers = handlers,
    navController = navController,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberNavigationResources(
  navController: NavHostController,
  sidebarViewModel: SidebarViewModel,
  disclaimerViewModel: FirstLaunchDisclaimerViewModel,
): Pair<NavigationLayoutState, NavigationHandlers> {
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  val navigationUiState =
    NavigationUiState(
      currentRoute = currentRoute,
      isWelcomeRoute = currentRoute == Screen.Welcome.route
    )

  val sidebarUiState = rememberSidebarUiState(sidebarViewModel)
  val disclaimerUiState by disclaimerViewModel.uiState.collectAsState()

  val onRouteVisit: (String) -> Unit =
    remember(sidebarViewModel) { { route -> sidebarViewModel.emitNavigation(route) } }

  val onNavigate: (String) -> Unit =
    remember(navController, onRouteVisit) {
      { route ->
        navController.navigate(route) {
          popUpTo(Screen.Home.route) { saveState = true }
          launchSingleTop = true
          restoreState = true
        }
        onRouteVisit(route)
      }
    }

  val onCloseDrawer = remember(drawerState, scope) { { scope.launch { drawerState.close() } } }

  val onDrawerToggle =
    remember(drawerState, scope) {
      {
        scope.launch {
          if (drawerState.isClosed) {
            drawerState.open()
          } else {
            drawerState.close()
          }
        }
      }
    }

  val sidebarInteractions =
    rememberSidebarInteractions(
      sidebarViewModel = sidebarViewModel,
      onNavigate = onNavigate,
      onCloseDrawer = onCloseDrawer,
    )

  val state =
    NavigationLayoutState(
      navigation = navigationUiState,
      sidebar = sidebarUiState,
      disclaimer = disclaimerUiState,
      drawerState = drawerState,
      hasBackStackEntry = navBackStackEntry != null,
    )

  val handlers =
    NavigationHandlers(
      sidebarInteractions = sidebarInteractions,
      onNavigate = onNavigate,
      onDrawerToggle = onDrawerToggle,
      onCloseDrawer = onCloseDrawer,
      onDisclaimerAcknowledge = disclaimerViewModel::onAcknowledge,
      onDisclaimerDismiss = disclaimerViewModel::onDismiss,
      onRouteVisit = onRouteVisit,
    )

  return state to handlers
}

@Composable
private fun rememberSidebarUiState(
  sidebarViewModel: SidebarViewModel,
): SidebarUiState {
  val threads by sidebarViewModel.threads.collectAsState()
  val searchQuery by sidebarViewModel.searchQuery.collectAsState()
  val showArchived by sidebarViewModel.showArchived.collectAsState()
  val inferencePreference by sidebarViewModel.inferencePreference.collectAsState()
  val pinnedTools by sidebarViewModel.pinnedTools.collectAsState()

  return SidebarUiState(
    threads = threads,
    searchQuery = searchQuery,
    showArchived = showArchived,
    inferenceMode = inferencePreference.mode,
    pinnedTools = pinnedTools,
  )
}

@Composable
private fun rememberSidebarInteractions(
  sidebarViewModel: SidebarViewModel,
  onNavigate: (String) -> Unit,
  onCloseDrawer: () -> Unit,
): SidebarInteractions =
  remember(sidebarViewModel, onNavigate, onCloseDrawer) {
    SidebarInteractions(
      onSearchQueryChange = sidebarViewModel::setSearchQuery,
      onToggleArchive = sidebarViewModel::toggleShowArchived,
      onInferenceModeChange = sidebarViewModel::setInferenceMode,
      onThreadSelected = { _ ->
        onNavigate(Screen.Chat.route)
        onCloseDrawer()
      },
      onArchiveThread = sidebarViewModel::archiveThread,
      onDeleteThread = sidebarViewModel::deleteThread,
      onNewThread = {
        sidebarViewModel.createNewThread(null)
        onNavigate(Screen.Chat.route)
        onCloseDrawer()
      },
      onNavigateHome = {
        onNavigate(Screen.Home.route)
        onCloseDrawer()
      },
      onNavigateSettings = {
        onNavigate(Screen.Settings.route)
        onCloseDrawer()
      },
    )
  }

@Composable
private fun HandleNavigationEffects(
  appState: AppUiState,
  navController: NavHostController,
  layoutState: NavigationLayoutState,
  onRouteVisit: (String) -> Unit,
) {
  val navigationUiState = layoutState.navigation
  val currentOnRouteVisit by rememberUpdatedState(onRouteVisit)

  LaunchedEffect(
    appState.shouldShowWelcome,
    navigationUiState.currentRoute,
    layoutState.hasBackStackEntry,
  ) {
    if (!layoutState.hasBackStackEntry) return@LaunchedEffect
    when {
      appState.shouldShowWelcome && navigationUiState.currentRoute != Screen.Welcome.route -> {
        navController.navigate(Screen.Welcome.route) {
          popUpTo(Screen.Home.route)
          launchSingleTop = true
        }
        currentOnRouteVisit(Screen.Welcome.route)
      }
      !appState.shouldShowWelcome && navigationUiState.currentRoute == Screen.Welcome.route -> {
        navController.navigate(Screen.Home.route) {
          popUpTo(Screen.Welcome.route) { inclusive = true }
          launchSingleTop = true
        }
        currentOnRouteVisit(Screen.Home.route)
      }
    }
  }

  LaunchedEffect(navigationUiState.isWelcomeRoute, layoutState.drawerState) {
    if (navigationUiState.isWelcomeRoute && layoutState.drawerState.isOpen) {
      layoutState.drawerState.close()
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationScaffoldContent(
  state: NavigationLayoutState,
  handlers: NavigationHandlers,
  navController: NavHostController,
  modifier: Modifier = Modifier,
) {
  BackHandler(enabled = state.drawerState.isOpen, onBack = handlers.onCloseDrawer)

  ModalNavigationDrawer(
    drawerState = state.drawerState,
    gesturesEnabled = !state.navigation.isWelcomeRoute,
    drawerContent = { NavigationDrawer(state.sidebar, handlers.sidebarInteractions) },
    modifier = modifier,
  ) {
    FirstLaunchDisclaimerDialog(
      isVisible = state.disclaimer.shouldShowDialog,
      onAcknowledge = handlers.onDisclaimerAcknowledge,
      onDismiss = handlers.onDisclaimerDismiss,
    )

    NavigationSurface(
      navigationUiState = state.navigation,
      onDrawerToggle = handlers.onDrawerToggle,
      onNavigate = handlers.onNavigate,
    ) { contentModifier ->
      NavigationRoutes(
        navController = navController,
        onRouteVisit = handlers.onRouteVisit,
        modifier = contentModifier,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationDrawer(
  sidebarUiState: SidebarUiState,
  sidebarInteractions: SidebarInteractions,
) {
  ModalDrawerSheet(
    modifier =
      Modifier.width(300.dp).fillMaxHeight().semantics {
        contentDescription = "Navigation drawer with conversation threads"
      },
  ) {
    SidebarContent(
      state = sidebarUiState,
      interactions = sidebarInteractions,
      modifier = Modifier.fillMaxHeight(),
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationSurface(
  navigationUiState: NavigationUiState,
  onDrawerToggle: () -> Unit,
  onNavigate: (String) -> Unit,
  content: @Composable (Modifier) -> Unit,
) {
  Scaffold(
    topBar = { NavigationTopBar(navigationUiState, onDrawerToggle) },
    bottomBar = {
      if (!navigationUiState.isWelcomeRoute) {
        BottomNavigationBar(
          currentRoute = navigationUiState.currentRoute,
          onNavigate = onNavigate,
        )
      }
    },
  ) { innerPadding ->
    content(Modifier.padding(innerPadding))
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationTopBar(
  navigationUiState: NavigationUiState,
  onDrawerToggle: () -> Unit,
) {
  if (navigationUiState.isWelcomeRoute) return

  TopAppBar(
    title = { Text(text = titleForRoute(navigationUiState.currentRoute)) },
    navigationIcon = {
      IconButton(
        onClick = onDrawerToggle,
        modifier = Modifier.semantics { contentDescription = "Open navigation drawer" },
      ) {
        Icon(Icons.Default.Menu, "Menu")
      }
    },
    colors =
      TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer
      ),
  )
}

@Composable
private fun NavigationRoutes(
  navController: NavHostController,
  onRouteVisit: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  NavHost(
    navController = navController,
    startDestination = Screen.Home.route,
    modifier = modifier,
  ) {
    welcomeRoute(navController, onRouteVisit)
    homeRoute(onRouteVisit)
    composable(Screen.Chat.route) { ChatScreen() }
    composable(Screen.ModelLibrary.route) { ModelLibraryScreen() }
    composable(Screen.Settings.route) { SettingsScreen() }
  }
}

private fun NavGraphBuilder.welcomeRoute(
  navController: NavHostController,
  onRouteVisit: (String) -> Unit,
) {
  composable(Screen.Welcome.route) { entry ->
    val welcomeViewModel: WelcomeViewModel = hiltViewModel(entry)
    val state by welcomeViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { onRouteVisit(Screen.Welcome.route) }

    WelcomeScreen(
      state = state,
      actions =
        WelcomePrimaryActions(
          onGetStarted = {
            welcomeViewModel.onGetStarted()
            navigateToHome(navController, onRouteVisit)
          },
          onExplore = {
            welcomeViewModel.onExploreFeatures()
            navigateToHome(navController, onRouteVisit)
          },
          onSkip = {
            welcomeViewModel.onSkip()
            navigateToHome(navController, onRouteVisit)
          },
        ),
      tooltipActions =
        WelcomeTooltipActions(
          onHelp = welcomeViewModel::onTooltipHelp,
          onDismiss = welcomeViewModel::onTooltipDismiss,
          onDontShowAgain = welcomeViewModel::onTooltipDontShowAgain,
        ),
    )
  }
}

private fun NavGraphBuilder.homeRoute(
  onRouteVisit: (String) -> Unit,
) {
  composable(Screen.Home.route) { entry ->
    val homeViewModel: HomeViewModel = hiltViewModel(entry)
    val state by homeViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { onRouteVisit(Screen.Home.route) }

    HomeScreen(
      state = state,
      callbacks =
        HomeScreenCallbacks(
          onToggleTools = homeViewModel::toggleToolsExpanded,
          onActionClick = homeViewModel::onRecentAction,
          onRetryOffline = homeViewModel::retryPendingActions,
          tooltip =
            HomeTooltipCallbacks(
              onDismiss = homeViewModel::dismissTooltip,
              onHelp = homeViewModel::onTooltipHelp,
              onDontShowAgain = homeViewModel::dontShowTooltipAgain,
            ),
        ),
    )
  }
}

private fun navigateToHome(
  navController: NavHostController,
  onRouteVisit: (String) -> Unit,
) {
  navController.navigate(Screen.Home.route) {
    popUpTo(Screen.Welcome.route) { inclusive = true }
    launchSingleTop = true
  }
  onRouteVisit(Screen.Home.route)
}

private fun titleForRoute(route: String?): String =
  when (route) {
    Screen.Home.route -> "Home"
    Screen.Chat.route -> "Chat"
    Screen.ModelLibrary.route -> "Model Library"
    Screen.Settings.route -> "Settings"
    else -> "nanoAI"
  }

// Sidebar content moved to com.vjaykrsna.nanoai.ui.sidebar.SidebarContent

@Composable
private fun BottomNavigationBar(
  currentRoute: String?,
  onNavigate: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  NavigationBar(
    modifier = modifier.semantics { contentDescription = "Bottom navigation bar" },
  ) {
    NavigationBarItem(
      selected = currentRoute == Screen.Chat.route,
      onClick = { onNavigate(Screen.Chat.route) },
      icon = { Icon(Icons.Default.Menu, "Chat") },
      label = { Text("Chat") },
      modifier = Modifier.semantics { contentDescription = "Navigate to Chat" },
    )
    NavigationBarItem(
      selected = currentRoute == Screen.ModelLibrary.route,
      onClick = { onNavigate(Screen.ModelLibrary.route) },
      icon = { Icon(Icons.Default.Menu, "Library") },
      label = { Text("Library") },
      modifier = Modifier.semantics { contentDescription = "Navigate to Model Library" },
    )
    NavigationBarItem(
      selected = currentRoute == Screen.Settings.route,
      onClick = { onNavigate(Screen.Settings.route) },
      icon = { Icon(Icons.Default.Settings, "Settings") },
      label = { Text("Settings") },
      modifier = Modifier.semantics { contentDescription = "Navigate to Settings" },
    )
  }
}
