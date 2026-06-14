package com.mobilegamecontroller.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mobilegamecontroller.ui.screens.ConnectScreen
import com.mobilegamecontroller.ui.screens.ControllerScreen
import com.mobilegamecontroller.ui.screens.SettingsScreen
import com.mobilegamecontroller.ui.screens.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val CONNECT = "connect"
    const val CONTROLLER = "controller"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigate = {
                    navController.navigate(Routes.CONNECT) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CONNECT) {
            ConnectScreen(
                onConnected = {
                    navController.navigate(Routes.CONTROLLER) {
                        popUpTo(Routes.CONNECT)
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(Routes.CONTROLLER) {
            ControllerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
