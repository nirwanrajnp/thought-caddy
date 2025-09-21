package com.nirwanrn.thoughtcaddy.main

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nirwanrn.thoughtcaddy.auth.AuthViewModel
import com.nirwanrn.thoughtcaddy.journal.JournalScreen
import com.nirwanrn.thoughtcaddy.ui.theme.ThoughtCaddyBlue

/**
 * Bottom navigation destinations
 */
sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Journals : BottomNavItem("journals", Icons.Default.Menu, "Journals")
    object Add : BottomNavItem("add_journal", Icons.Default.Add, "Add")
    object About : BottomNavItem("about", Icons.Default.Info, "About")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ThoughtCaddyBlue
            ) {
                NavigationBarItem(
                    icon = {
                        Icon(
                            BottomNavItem.Journals.icon,
                            contentDescription = BottomNavItem.Journals.label
                        )
                    },
                    label = { Text(BottomNavItem.Journals.label) },
                    selected = currentRoute == BottomNavItem.Journals.route,
                    onClick = {
                        navController.navigate(BottomNavItem.Journals.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ThoughtCaddyBlue,
                        selectedTextColor = ThoughtCaddyBlue,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = ThoughtCaddyBlue.copy(alpha = 0.1f)
                    )
                )

                // Center Add Button
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (currentRoute != "add_journal") {
                                navController.navigate("add_journal")
                            }
                        },
                        containerColor = if (currentRoute != "add_journal")
                            ThoughtCaddyBlue
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (currentRoute != "add_journal")
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = if (currentRoute != "add_journal") 8.dp else 2.dp,
                            pressedElevation = if (currentRoute != "add_journal") 12.dp else 2.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Journal Entry",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                NavigationBarItem(
                    icon = {
                        Icon(
                            BottomNavItem.About.icon,
                            contentDescription = BottomNavItem.About.label
                        )
                    },
                    label = { Text(BottomNavItem.About.label) },
                    selected = currentRoute == BottomNavItem.About.route,
                    onClick = {
                        navController.navigate(BottomNavItem.About.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ThoughtCaddyBlue,
                        selectedTextColor = ThoughtCaddyBlue,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = ThoughtCaddyBlue.copy(alpha = 0.1f)
                    )
                )
            }
        }
    ) { paddingValues ->
        BottomNavHost(
            navController = navController,
            onLogout = onLogout,
            authViewModel = authViewModel,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun BottomNavHost(
    navController: NavHostController,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Journals.route,
        modifier = modifier
    ) {
        composable(BottomNavItem.Journals.route) {
            JournalScreen(
                onLogout = onLogout,
                authViewModel = authViewModel
            )
        }

        composable(BottomNavItem.About.route) {
            AboutScreen()
        }

        composable("add_journal") {
            AddJournalScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onJournalSaved = {
                    navController.navigate(BottomNavItem.Journals.route) {
                        popUpTo("add_journal") { inclusive = true }
                    }
                }
            )
        }
    }
}