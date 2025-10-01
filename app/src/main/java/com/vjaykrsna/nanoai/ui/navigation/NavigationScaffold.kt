package com.vjaykrsna.nanoai.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.feature.chat.ui.ChatScreen
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryScreen
import com.vjaykrsna.nanoai.feature.settings.ui.SettingsScreen
import com.vjaykrsna.nanoai.feature.sidebar.presentation.SidebarViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScaffold(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    sidebarViewModel: SidebarViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val threads by sidebarViewModel.threads.collectAsState()
    val searchQuery by sidebarViewModel.searchQuery.collectAsState()
    val showArchived by sidebarViewModel.showArchived.collectAsState()

    // Handle back button when drawer is open
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .semantics { contentDescription = "Navigation drawer with conversation threads" }
            ) {
                SidebarContent(
                    threads = threads,
                    searchQuery = searchQuery,
                    showArchived = showArchived,
                    onSearchQueryChanged = { sidebarViewModel.setSearchQuery(it) },
                    onToggleArchived = { sidebarViewModel.toggleShowArchived() },
                    onThreadClick = { thread ->
                        // Navigate to chat with thread
                        navController.navigate(Screen.Chat.route)
                        scope.launch { drawerState.close() }
                    },
                    onArchiveThread = { sidebarViewModel.archiveThread(it) },
                    onDeleteThread = { sidebarViewModel.deleteThread(it) },
                    onNewThread = {
                        sidebarViewModel.createNewThread(null)
                        navController.navigate(Screen.Chat.route)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.fillMaxHeight()
                )
            }
        },
        modifier = modifier
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentRoute) {
                                Screen.Chat.route -> "Chat"
                                Screen.ModelLibrary.route -> "Model Library"
                                Screen.Settings.route -> "Settings"
                                else -> "nanoAI"
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "Toggle navigation drawer"
                            }
                        ) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            bottomBar = {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Chat.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Chat.route) {
                    ChatScreen()
                }
                composable(Screen.ModelLibrary.route) {
                    ModelLibraryScreen()
                }
                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
            }
        }
    }
}

@Composable
private fun SidebarContent(
    threads: List<ChatThread>,
    searchQuery: String,
    showArchived: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onToggleArchived: () -> Unit,
    onThreadClick: (ChatThread) -> Unit,
    onArchiveThread: (java.util.UUID) -> Unit,
    onDeleteThread: (java.util.UUID) -> Unit,
    onNewThread: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Conversations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onNewThread,
                modifier = Modifier.semantics {
                    contentDescription = "Create new conversation"
                }
            ) {
                Icon(Icons.Default.Add, "New")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search field
        androidx.compose.material3.TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Search conversations...") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Search conversations" }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Archive toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showArchived) "Archived" else "Active",
                style = MaterialTheme.typography.titleMedium
            )
            AssistChip(
                onClick = onToggleArchived,
                label = { Text(if (showArchived) "Show Active" else "Show Archived") },
                leadingIcon = { Icon(Icons.Default.Delete, null) },
                modifier = Modifier.semantics {
                    contentDescription = "Toggle archived conversations"
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Thread list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "Conversation threads list" }
        ) {
            items(
                items = threads,
                key = { it.threadId.toString() },
                contentType = { "thread_item" }
            ) { thread ->
                ThreadItem(
                    thread = thread,
                    onClick = { onThreadClick(thread) },
                    onArchive = { onArchiveThread(thread.threadId) },
                    onDelete = { onDeleteThread(thread.threadId) }
                )
            }
        }
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
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = thread.title ?: "Untitled Chat",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val timestamp = thread.updatedAt
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .let { "${it.monthNumber}/${it.dayOfMonth}" }
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(
                        onClick = onArchive,
                        modifier = Modifier.semantics {
                            contentDescription = "Archive conversation"
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            "Archive",
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.semantics {
                            contentDescription = "Delete conversation"
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            "Delete",
                            modifier = Modifier.padding(4.dp)
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
        modifier = modifier.semantics {
            contentDescription = "Bottom navigation bar"
        }
    ) {
        NavigationBarItem(
            selected = currentRoute == Screen.Chat.route,
            onClick = { onNavigate(Screen.Chat.route) },
            icon = { Icon(Icons.Default.Menu, "Chat") },
            label = { Text("Chat") },
            modifier = Modifier.semantics {
                contentDescription = "Navigate to Chat"
            }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.ModelLibrary.route,
            onClick = { onNavigate(Screen.ModelLibrary.route) },
            icon = { Icon(Icons.Default.Menu, "Library") },
            label = { Text("Library") },
            modifier = Modifier.semantics {
                contentDescription = "Navigate to Model Library"
            }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Settings.route,
            onClick = { onNavigate(Screen.Settings.route) },
            icon = { Icon(Icons.Default.Settings, "Settings") },
            label = { Text("Settings") },
            modifier = Modifier.semantics {
                contentDescription = "Navigate to Settings"
            }
        )
    }
}
