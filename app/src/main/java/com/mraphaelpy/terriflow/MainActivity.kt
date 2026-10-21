package com.mraphaelpy.terriflow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.mraphaelpy.terriflow.core.updater.UpdateDialog
import com.mraphaelpy.terriflow.notification.LocalNotificationHelper
import com.mraphaelpy.terriflow.presentation.components.LocalUserDirectory
import com.mraphaelpy.terriflow.presentation.main.MainViewModel
import com.mraphaelpy.terriflow.presentation.navigation.AppNavigation
import com.mraphaelpy.terriflow.presentation.navigation.Screen
import com.mraphaelpy.terriflow.ui.theme.TerriFlowTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deepLinkTerritoryId = intent?.getStringExtra(LocalNotificationHelper.EXTRA_TERRITORY_ID)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val appColor by viewModel.appColor.collectAsStateWithLifecycle()
            val textSize by viewModel.textSize.collectAsStateWithLifecycle()
            val contrastLevel by viewModel.contrastLevel.collectAsStateWithLifecycle()
            val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
            val availableUpdate by viewModel.availableUpdate.collectAsStateWithLifecycle()
            val userDirectory by viewModel.userDirectory.collectAsStateWithLifecycle()

            RequestNotificationPermission()

            TerriFlowTheme(themeMode = themeMode, appColor = appColor, textSize = textSize, contrastLevel = contrastLevel) {
                CompositionLocalProvider(LocalUserDirectory provides userDirectory) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        startDestination?.let { destination ->
                            val navController = rememberNavController()
                            AppNavigation(navController = navController, startDestination = destination)

                            LaunchedEffect(destination) {
                                if (deepLinkTerritoryId != null && destination == Screen.Dashboard.route) {
                                    navController.navigate(Screen.TerritoryDetail.createRoute(deepLinkTerritoryId))
                                }
                            }
                        }

                        availableUpdate?.let { info ->
                            UpdateDialog(
                                updateInfo = info,
                                onDismiss = viewModel::dismissUpdate,
                                updateManager = viewModel.updateManager
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RequestNotificationPermission() {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
