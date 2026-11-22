package com.inyogeshwar.metaclean.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.inyogeshwar.metaclean.presentation.batch.BatchScreen
import com.inyogeshwar.metaclean.presentation.history.HistoryScreen
import com.inyogeshwar.metaclean.presentation.home.HomeScreen
import com.inyogeshwar.metaclean.presentation.inspector.InspectorScreen
import com.inyogeshwar.metaclean.presentation.settings.SettingsScreen
import com.inyogeshwar.metaclean.presentation.verification.VerificationScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier.padding(paddingValues)
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToInspector = { navController.navigate(Screen.Inspector.route) },
                onNavigateToBatch = { navController.navigate(Screen.Batch.route) }
            )
        }

        composable(Screen.Inspector.route) {
            InspectorScreen(
                onNavigateToVerification = { itemId ->
                    navController.navigate(Screen.Verification.createRoute(itemId))
                }
            )
        }

        composable(Screen.Batch.route) {
            BatchScreen(
                onNavigateToInspector = { itemId ->
                    navController.navigate(Screen.Inspector.route)
                },
                onNavigateToVerification = { itemId ->
                    navController.navigate(Screen.Verification.createRoute(itemId))
                }
            )
        }

        composable(
            route = Screen.Verification.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) {
            VerificationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
