package com.inyogeshwar.metaclean.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.inyogeshwar.metaclean.core.ui.theme.*

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
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        containerColor = iOSBackgroundDark,
        tonalElevation = 0.dp
    ) {
        items.forEach { screen ->
            val isSelected = currentRoute == screen.route

            NavigationBarItem(
                icon = {
                    BadgedBox(
                        badge = {
                            if (screen == Screen.Batch && queueCount > 0) {
                                Badge(containerColor = iOSSystemBlue) {
                                    Text(text = "$queueCount", fontSize = 10.sp, color = Color.White)
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
                label = {
                    Text(
                        text = screen.title,
                        fontSize = 10.sp,
                        color = if (isSelected) iOSSystemBlue else iOSTextSecondary
                    )
                },
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
                    selectedIconColor = iOSSystemBlue,
                    unselectedIconColor = iOSTextSecondary,
                    selectedTextColor = iOSSystemBlue,
                    unselectedTextColor = iOSTextSecondary,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
