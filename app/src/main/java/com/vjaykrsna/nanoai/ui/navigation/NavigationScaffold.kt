@file:Suppress("FunctionName")

package com.vjaykrsna.nanoai.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.model.InferenceMode
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
import com.vjaykrsna.nanoai.feature.uiux.ui.SidebarDrawer
import com.vjaykrsna.nanoai.feature.uiux.ui.WelcomeScreen
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private data class NavigationUiState(
  val currentRoute: String?,
  val isWelcomeRoute: Boolean,
)

private data class SidebarDrawerUiState(
  val threads: List<ChatThread>,
  val searchQuery: String,
  val showArchived: Boolean,
  val inferenceMode: InferenceMode,
  val pinnedTools: List<String>,
)

private data class SidebarCallbacks(
  val onSearchQueryChange: (String) -> Unit,
  val onToggleArchive: () -> Unit,
  val onInferenceModeChange: (InferenceMode) -> Unit,
  val onThreadSelected: (ChatThread) -> Unit,
  val onArchiveThread: (UUID) -> Unit,
  val onDeleteThread: (UUID) -> Unit,
  val onNewThread: () -> Unit,
  val onNavigateHome: () -> Unit,
  val onNavigateSettings: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScaffold(
  appState: AppUiState = AppUiState(),
  modifier: Modifier = Modifier,
  navController: NavHostController = rememberNavController(),
  sidebarViewModel: SidebarViewModel = hiltViewModel(),
  disclaimerViewModel: FirstLaunchDisclaimerViewModel = hiltViewModel(),
) {
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  val isWelcomeRoute = currentRoute == Screen.Welcome.route

  val threads by sidebarViewModel.threads.collectAsState()
  val searchQuery by sidebarViewModel.searchQuery.collectAsState()
  val showArchived by sidebarViewModel.showArchived.collectAsState()
  val inferencePreference by sidebarViewModel.inferencePreference.collectAsState()
  val pinnedTools by sidebarViewModel.pinnedTools.collectAsState()
  val disclaimerUiState by disclaimerViewModel.uiState.collectAsState()
  val navigationUiState =
    NavigationUiState(currentRoute = currentRoute, isWelcomeRoute = isWelcomeRoute)
  val sidebarUiState =
    SidebarDrawerUiState(
      threads = threads,
      searchQuery = searchQuery,
      showArchived = showArchived,
      inferenceMode = inferencePreference.mode,
      pinnedTools = pinnedTools,
    )

  HandleNavigationEffects(
    appState = appState,
    navigationUiState = navigationUiState,
    navController = navController,
    sidebarViewModel = sidebarViewModel,
    drawerState = drawerState,
    hasBackStackEntry = navBackStackEntry != null,
  )

  val sidebarCallbacks =
    SidebarCallbacks(
      onSearchQueryChange = sidebarViewModel::setSearchQuery,
      onToggleArchive = sidebarViewModel::toggleShowArchived,
      onInferenceModeChange = { mode -> sidebarViewModel.setInferenceMode(mode) },
      onThreadSelected = { thread ->
        navController.navigate(Screen.Chat.route)
        sidebarViewModel.emitNavigation(Screen.Chat.route)
        scope.launch { drawerState.close() }
      },
      onArchiveThread = sidebarViewModel::archiveThread,
      onDeleteThread = sidebarViewModel::deleteThread,
      onNewThread = {
        sidebarViewModel.createNewThread(null)
        navController.navigate(Screen.Chat.route) {
          popUpTo(Screen.Home.route) { saveState = true }
          launchSingleTop = true
          restoreState = true
        }
        sidebarViewModel.emitNavigation(Screen.Chat.route)
        scope.launch { drawerState.close() }
      },
      onNavigateHome = {
        navController.navigate(Screen.Home.route) {
          popUpTo(Screen.Home.route) { saveState = true }
          launchSingleTop = true
          restoreState = true
        }
        sidebarViewModel.emitNavigation(Screen.Home.route)
        scope.launch { drawerState.close() }
      },
      onNavigateSettings = {
        navController.navigate(Screen.Settings.route)
        sidebarViewModel.emitNavigation(Screen.Settings.route)
        scope.launch { drawerState.close() }
      },
    )

  val onNavigate: (String) -> Unit = { route ->
    navController.navigate(route) {
      popUpTo(Screen.Home.route) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  NavigationScaffoldContent(
    navigationUiState = navigationUiState,
    drawerState = drawerState,
    sidebarUiState = sidebarUiState,
    sidebarCallbacks = sidebarCallbacks,
    disclaimerUiState = disclaimerUiState,
    onDrawerToggle = {
      scope.launch { if (drawerState.isClosed) drawerState.open() else drawerState.close() }
    },
    onNavigate = onNavigate,
    onCloseDrawer = { scope.launch { drawerState.close() } },
    onDisclaimerAcknowledge = disclaimerViewModel::onAcknowledge,
    onDisclaimerDismiss = disclaimerViewModel::onDismiss,
    navController = navController,
    sidebarViewModel = sidebarViewModel,
    modifier = modifier,
  )
}

@Composable
private fun HandleNavigationEffects(
  appState: AppUiState,
  navigationUiState: NavigationUiState,
  navController: NavHostController,
  sidebarViewModel: SidebarViewModel,
  drawerState: DrawerState,
  hasBackStackEntry: Boolean,
) {
  LaunchedEffect(appState.shouldShowWelcome, navigationUiState.currentRoute, hasBackStackEntry) {
    if (!hasBackStackEntry) return@LaunchedEffect
    when {
      appState.shouldShowWelcome && navigationUiState.currentRoute != Screen.Welcome.route -> {
        navController.navigate(Screen.Welcome.route) {
          popUpTo(Screen.Home.route)
          launchSingleTop = true
        }
        sidebarViewModel.emitNavigation(Screen.Welcome.route)
      }
      !appState.shouldShowWelcome && navigationUiState.currentRoute == Screen.Welcome.route -> {
        navController.navigate(Screen.Home.route) {
          popUpTo(Screen.Welcome.route) { inclusive = true }
          launchSingleTop = true
        }
        sidebarViewModel.emitNavigation(Screen.Home.route)
      }
    }
  }

  LaunchedEffect(navigationUiState.isWelcomeRoute) {
    if (navigationUiState.isWelcomeRoute && drawerState.isOpen) {
      drawerState.close()
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationScaffoldContent(
  navigationUiState: NavigationUiState,
  drawerState: DrawerState,
  sidebarUiState: SidebarDrawerUiState,
  sidebarCallbacks: SidebarCallbacks,
  disclaimerUiState: FirstLaunchDisclaimerUiState,
  onDrawerToggle: () -> Unit,
  onNavigate: (String) -> Unit,
  onCloseDrawer: () -> Unit,
  onDisclaimerAcknowledge: () -> Unit,
  onDisclaimerDismiss: () -> Unit,
  navController: NavHostController,
  sidebarViewModel: SidebarViewModel,
  modifier: Modifier = Modifier,
) {
  BackHandler(enabled = drawerState.isOpen) { onCloseDrawer() }

  ModalNavigationDrawer(
    drawerState = drawerState,
    gesturesEnabled = !navigationUiState.isWelcomeRoute,
    drawerContent = {
      ModalDrawerSheet(
        modifier =
          Modifier.width(300.dp).fillMaxHeight().semantics {
            contentDescription = "Navigation drawer with conversation threads"
          },
      ) {
        SidebarContent(
          state = sidebarUiState,
          callbacks = sidebarCallbacks,
          modifier = Modifier.fillMaxHeight(),
        )
      }
    },
    modifier = modifier,
  ) {
    FirstLaunchDisclaimerDialog(
      isVisible = disclaimerUiState.shouldShowDialog,
      onAcknowledge = onDisclaimerAcknowledge,
      onDismiss = onDisclaimerDismiss,
    )

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
      NavigationRoutes(
        navController = navController,
        sidebarViewModel = sidebarViewModel,
        modifier = Modifier.padding(innerPadding),
      )
    }
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
  sidebarViewModel: SidebarViewModel,
  modifier: Modifier = Modifier,
) {
  NavHost(
    navController = navController,
    startDestination = Screen.Home.route,
    modifier = modifier,
  ) {
    composable(Screen.Welcome.route) { entry ->
      val welcomeViewModel: WelcomeViewModel = hiltViewModel(entry)
      val state by welcomeViewModel.uiState.collectAsStateWithLifecycle()

      WelcomeScreen(
        state = state,
        onGetStartedClick = {
          welcomeViewModel.onGetStarted()
          navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Welcome.route) { inclusive = true }
            launchSingleTop = true
          }
          sidebarViewModel.emitNavigation(Screen.Home.route)
        },
        onExplore = {
          welcomeViewModel.onExploreFeatures()
          navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Welcome.route) { inclusive = true }
            launchSingleTop = true
          }
          sidebarViewModel.emitNavigation(Screen.Home.route)
        },
        onSkip = {
          welcomeViewModel.onSkip()
          navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Welcome.route) { inclusive = true }
            launchSingleTop = true
          }
          sidebarViewModel.emitNavigation(Screen.Home.route)
        },
        onTooltipHelp = welcomeViewModel::onTooltipHelp,
        onTooltipDismiss = welcomeViewModel::onTooltipDismiss,
        onTooltipDontShow = welcomeViewModel::onTooltipDontShowAgain,
      )
    }
    composable(Screen.Home.route) { entry ->
      val homeViewModel: HomeViewModel = hiltViewModel(entry)
      val state by homeViewModel.uiState.collectAsStateWithLifecycle()

      LaunchedEffect(Unit) { sidebarViewModel.emitNavigation(Screen.Home.route) }

      HomeScreen(
        state = state,
        onToggleTools = homeViewModel::toggleToolsExpanded,
        onActionClick = homeViewModel::onRecentAction,
        onTooltipDismiss = homeViewModel::dismissTooltip,
        onTooltipHelp = homeViewModel::onTooltipHelp,
        onTooltipDontShow = homeViewModel::dontShowTooltipAgain,
        onRetryOffline = homeViewModel::retryPendingActions,
      )
    }
    composable(Screen.Chat.route) { ChatScreen() }
    composable(Screen.ModelLibrary.route) { ModelLibraryScreen() }
    composable(Screen.Settings.route) { SettingsScreen() }
  }
}

private fun titleForRoute(route: String?): String =
  when (route) {
    Screen.Home.route -> "Home"
    Screen.Chat.route -> "Chat"
    Screen.ModelLibrary.route -> "Model Library"
    Screen.Settings.route -> "Settings"
    else -> "nanoAI"
  }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SidebarContent(
  state: SidebarDrawerUiState,
  callbacks: SidebarCallbacks,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxHeight().testTag("sidebar_drawer_container")) {
    SidebarDrawer(
      pinnedTools = state.pinnedTools,
      onNavigateSettings = callbacks.onNavigateSettings,
      onNavigateHome = callbacks.onNavigateHome,
      modifier = Modifier.fillMaxWidth(),
    )

    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Conversations",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        IconButton(
          onClick = callbacks.onNewThread,
          modifier = Modifier.semantics { contentDescription = "Create new conversation" },
        ) {
          Icon(Icons.Default.Add, "New")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      androidx.compose.material3.TextField(
        value = state.searchQuery,
        onValueChange = callbacks.onSearchQueryChange,
        placeholder = { Text("Search conversations...") },
        leadingIcon = { Icon(Icons.Default.Search, "Search") },
        modifier =
          Modifier.fillMaxWidth().semantics { contentDescription = "Search conversations" },
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (state.showArchived) "Archived" else "Active",
          style = MaterialTheme.typography.titleMedium,
        )
        AssistChip(
          onClick = callbacks.onToggleArchive,
          label = { Text(if (state.showArchived) "Show Active" else "Show Archived") },
          leadingIcon = { Icon(Icons.Default.Delete, null) },
          modifier = Modifier.semantics { contentDescription = "Toggle archived conversations" },
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      InferencePreferenceToggleRow(
        inferenceMode = state.inferenceMode,
        onInferenceModeChange = callbacks.onInferenceModeChange,
        modifier = Modifier.fillMaxWidth(),
      )
    }

    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
      modifier = Modifier.weight(1f).semantics { contentDescription = "Conversation threads list" },
    ) {
      items(
        items = state.threads,
        key = { it.threadId.toString() },
        contentType = { "thread_item" },
      ) { thread ->
        ThreadItem(
          thread = thread,
          onClick = { callbacks.onThreadSelected(thread) },
          onArchive = { callbacks.onArchiveThread(thread.threadId) },
          onDelete = { callbacks.onDeleteThread(thread.threadId) },
        )
      }
    }
  }
}

@VisibleForTesting
@Composable
internal fun InferencePreferenceToggleRow(
  inferenceMode: InferenceMode,
  onInferenceModeChange: (InferenceMode) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = "Inference Preference",
        style = MaterialTheme.typography.titleMedium,
      )
      Text(
        text =
          if (inferenceMode == InferenceMode.LOCAL_FIRST) {
            "Prefer on-device models when available"
          } else {
            "Prefer cloud inference when online"
          },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = inferenceMode == InferenceMode.LOCAL_FIRST,
      onCheckedChange = { checked ->
        val mode = if (checked) InferenceMode.LOCAL_FIRST else InferenceMode.CLOUD_FIRST
        onInferenceModeChange(mode)
      },
      modifier = Modifier.semantics { contentDescription = "Toggle inference preference" },
    )
  }
}

@Composable
private fun ThreadItem(
  thread: ChatThread,
  onClick: () -> Unit,
  onArchive: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = thread.title ?: "Untitled Chat",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(4.dp))
          val timestamp =
            thread.updatedAt.toLocalDateTime(TimeZone.currentSystemDefault()).let {
              "${it.monthNumber}/${it.dayOfMonth}"
            }
          Text(
            text = timestamp,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Row {
          IconButton(
            onClick = onArchive,
            modifier = Modifier.semantics { contentDescription = "Archive conversation" },
          ) {
            Icon(
              Icons.Default.Delete,
              "Archive",
              modifier = Modifier.padding(4.dp),
            )
          }
          IconButton(
            onClick = onDelete,
            modifier = Modifier.semantics { contentDescription = "Delete conversation" },
          ) {
            Icon(
              Icons.Default.Delete,
              "Delete",
              modifier = Modifier.padding(4.dp),
            )
          }
        }
      }
    }
  }
}

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
