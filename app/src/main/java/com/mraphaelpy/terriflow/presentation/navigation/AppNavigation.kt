package com.mraphaelpy.terriflow.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.ui.Modifier

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
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.TerritoryList.route,
        Screen.Map.route
    )

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
                onNavigateToReset = { navController.navigate(Screen.ResetPassword.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ResetPassword.route) {
            ResetPasswordScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToTerritories = { navController.navigate(Screen.TerritoryList.route) },
                onNavigateToMap = { navController.navigate(Screen.Map.createRoute()) },
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
                onImportKml = { navController.navigate(Screen.ImportKml.route) },
                onNavigateBack = { navController.popBackStack() }
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
        
        if (showBottomBar) {
            TerriflowBottomNavBar(
                currentRoute = currentRoute,
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToTerritories = {
                    navController.navigate(Screen.TerritoryList.route) {
                        popUpTo(Screen.Dashboard.route)
                    }
                },
                onNavigateToMap = {
                    navController.navigate(Screen.Map.createRoute()) {
                        popUpTo(Screen.Dashboard.route)
                    }
                },
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun TerriflowBottomNavBar(
    currentRoute: String?,
    onNavigateToDashboard: () -> Unit,
    onNavigateToTerritories: () -> Unit,
    onNavigateToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .fillMaxWidth(),
        contentAlignment = androidx.compose.ui.Alignment.BottomCenter
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp).copy(alpha = 0.90f),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                tonalElevation = 0.dp,
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
            ) {
                NavigationBarItem(
                    selected = currentRoute == Screen.Dashboard.route,
                    onClick = onNavigateToDashboard,
                    icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                    label = { Text("Início") }
                )
                NavigationBarItem(
                    selected = currentRoute == Screen.TerritoryList.route,
                    onClick = onNavigateToTerritories,
                    icon = { Icon(Icons.Default.GridView, contentDescription = "Territórios") },
                    label = { Text("Territórios") }
                )
                NavigationBarItem(
                    selected = currentRoute?.startsWith("map") == true,
                    onClick = onNavigateToMap,
                    icon = { Icon(Icons.Default.Map, contentDescription = "Mapa") },
                    label = { Text("Mapa") }
                )
            }
        }
    }
}
