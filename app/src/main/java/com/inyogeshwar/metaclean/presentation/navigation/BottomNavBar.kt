package com.inyogeshwar.metaclean.presentation.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.inyogeshwar.metaclean.core.ui.theme.AccentPrimary

@Composable
fun BottomNavBar(
    navController: NavController,
    queueCount: Int,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Screen.Home,
        Screen.Inspector,
        Screen.Batch,
        Screen.History,
        Screen.Settings
    )

    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { screen ->
            val isSelected = currentRoute == screen.route

            NavigationBarItem(
                icon = {
                    BadgedBox(
                        badge = {
                            if (screen == Screen.Batch && queueCount > 0) {
                                Badge(containerColor = AccentPrimary) {
                                    Text(text = "$queueCount")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                            contentDescription = screen.title
                        )
                    }
                },
                label = { Text(screen.title) },
                selected = isSelected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentPrimary,
                    selectedTextColor = AccentPrimary,
                    indicatorColor = AccentPrimary.copy(alpha = 0.15f)
                )
            )
        }
    }
}
