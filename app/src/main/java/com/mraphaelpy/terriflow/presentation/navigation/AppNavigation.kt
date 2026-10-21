package com.mraphaelpy.terriflow.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.mraphaelpy.terriflow.presentation.auth.LoginScreen
import com.mraphaelpy.terriflow.presentation.auth.RegisterScreen
import com.mraphaelpy.terriflow.presentation.auth.ResetPasswordScreen
import com.mraphaelpy.terriflow.presentation.dashboard.DashboardScreen
import com.mraphaelpy.terriflow.presentation.map.MapScreen
import com.mraphaelpy.terriflow.presentation.notifications.NotificationsScreen
import com.mraphaelpy.terriflow.presentation.territories.CreateTerritoryScreen
import com.mraphaelpy.terriflow.presentation.territories.EditTerritoryScreen
import com.mraphaelpy.terriflow.presentation.territories.TerritoryListScreen
import com.mraphaelpy.terriflow.presentation.territorydetail.TerritoryDetailScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object ResetPassword : Screen("reset_password")
    object Dashboard : Screen("dashboard")
    object TerritoryList : Screen("territories")
    object CreateTerritory : Screen("territories/create")
    object TerritoryDetail : Screen("territories/{territoryId}") {
        fun createRoute(id: String) = "territories/$id"
    }
    object EditTerritory : Screen("territories/{territoryId}/edit") {
        fun createRoute(id: String) = "territories/$id/edit"
    }
    object Map : Screen("map?highlightId={highlightId}") {
        fun createRoute(highlightId: String? = null) =
            if (highlightId != null) "map?highlightId=$highlightId" else "map?highlightId="
    }
    object Notifications : Screen("notifications")
    object Users : Screen("users")
    object Audit : Screen("audit")
    object Coverage : Screen("coverage")
    object ImportKml : Screen("territories/import")
    object Settings : Screen("settings")
    object CongregationSetup : Screen("congregation_setup")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val mapHighlight = navBackStackEntry?.arguments?.getString("highlightId").orEmpty()
    val selectedTab = when {
        currentRoute == Screen.Dashboard.route -> BottomTab.HOME
        currentRoute == Screen.TerritoryList.route -> BottomTab.TERRITORIES
        currentRoute == Screen.Map.route && mapHighlight.isEmpty() -> BottomTab.MAP
        else -> null
    }
    val clearance = if (selectedTab != null) bottomBarClearance() else 0.dp

    fun openTab(tab: BottomTab) {
        val route = when (tab) {
            BottomTab.HOME -> Screen.Dashboard.route
            BottomTab.TERRITORIES -> Screen.TerritoryList.route
            BottomTab.MAP -> Screen.Map.createRoute()
        }
        navController.navigate(route) {
            popUpTo(Screen.Dashboard.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(LocalBottomBarClearance provides clearance) {
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ) + fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
            },
            exitTransition = {
                scaleOut(
                    targetScale = 0.95f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ) + fadeOut(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
            },
            popEnterTransition = {
                scaleIn(
                    initialScale = 0.95f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ) + fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ) + fadeOut(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
            }
    ) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToReset = { navController.navigate(Screen.ResetPassword.route) },
                onNeedsCongregationSetup = {
                    navController.navigate(Screen.CongregationSetup.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
                onNeedsCongregationSetup = {
                    navController.navigate(Screen.CongregationSetup.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ResetPassword.route) {
            ResetPasswordScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.CongregationSetup.route) {
            com.mraphaelpy.terriflow.presentation.congregation.CongregationSetupScreen(
                onSetupComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.CongregationSetup.route) { inclusive = true }
                    }
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToTerritories = { openTab(BottomTab.TERRITORIES) },
                onNavigateToMap = { openTab(BottomTab.MAP) },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                onNavigateToTerritory = { id ->
                    navController.navigate(Screen.TerritoryDetail.createRoute(id))
                },
                onNavigateToUsers = { navController.navigate(Screen.Users.route) },
                onNavigateToAudit = { navController.navigate(Screen.Audit.route) },
                onNavigateToCoverage = { navController.navigate(Screen.Coverage.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.TerritoryList.route) {
            TerritoryListScreen(
                onNavigateToTerritory = { id ->
                    navController.navigate(Screen.TerritoryDetail.createRoute(id))
                },
                onCreateTerritory = { navController.navigate(Screen.CreateTerritory.route) },
                onImportKml = { navController.navigate(Screen.ImportKml.route) }
            )
        }

        composable(Screen.ImportKml.route) {
            com.mraphaelpy.terriflow.presentation.territories.ImportKmlScreen(
                onNavigateBack = { navController.popBackStack() },
                onImportDone = {
                    navController.navigate(Screen.TerritoryList.route) {
                        popUpTo(Screen.TerritoryList.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CreateTerritory.route) {
            CreateTerritoryScreen(
                onCreated = { id ->
                    navController.navigate(Screen.TerritoryDetail.createRoute(id)) {
                        popUpTo(Screen.TerritoryList.route)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TerritoryDetail.route,
            arguments = listOf(navArgument("territoryId") { type = NavType.StringType })
        ) {
            TerritoryDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.EditTerritory.createRoute(id))
                },
                onNavigateToMap = { highlightId ->
                    navController.navigate(Screen.Map.createRoute(highlightId))
                }
            )
        }

        composable(
            route = Screen.EditTerritory.route,
            arguments = listOf(navArgument("territoryId") { type = NavType.StringType })
        ) {
            EditTerritoryScreen(
                onSaved = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Map.route,
            arguments = listOf(navArgument("highlightId") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            val highlightId = backStackEntry.arguments?.getString("highlightId")
                ?.takeIf { it.isNotEmpty() }
            MapScreen(
                highlightId = highlightId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTerritory = { id ->
                    navController.navigate(Screen.TerritoryDetail.createRoute(id))
                }
            )
        }

        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onNavigateToTerritory = { id ->
                    navController.navigate(Screen.TerritoryDetail.createRoute(id))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Users.route) {
            com.mraphaelpy.terriflow.presentation.users.UsersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Audit.route) {
            com.mraphaelpy.terriflow.presentation.audit.AuditScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTerritory = { id ->
                    navController.navigate(Screen.TerritoryDetail.createRoute(id))
                }
            )
        }
        
        composable(Screen.Coverage.route) {
            com.mraphaelpy.terriflow.presentation.coverage.CoverageScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTerritory = { id ->
                    navController.navigate(Screen.TerritoryDetail.createRoute(id))
                }
            )
        }
        
        composable(Screen.Settings.route) {
            com.mraphaelpy.terriflow.presentation.settings.SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
        
        AnimatedVisibility(
            visible = selectedTab != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            TerriflowBottomNavBar(selected = selectedTab, onSelect = ::openTab)
        }
    }
    }
}
