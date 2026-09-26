package com.mraphaelpy.terriflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.mraphaelpy.terriflow.domain.repository.AppColor
import com.mraphaelpy.terriflow.domain.repository.SettingsRepository
import com.mraphaelpy.terriflow.domain.repository.ThemeMode
import com.mraphaelpy.terriflow.presentation.navigation.AppNavigation
import com.mraphaelpy.terriflow.presentation.navigation.Screen
import com.mraphaelpy.terriflow.ui.theme.TerriFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deepLinkTerritoryId = intent?.getStringExtra("territoryId")

        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val appColor by settingsRepository.appColor.collectAsState(initial = AppColor.DEFAULT)

            TerriFlowTheme(themeMode = themeMode, appColor = appColor) {
                Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val startDestination = remember {
                        if (auth.currentUser != null) Screen.Dashboard.route
                        else Screen.Login.route
                    }

                    AppNavigation(
                        navController = navController,
                        startDestination = startDestination
                    )

                    if (deepLinkTerritoryId != null && auth.currentUser != null) {
                        navController.navigate(Screen.TerritoryDetail.createRoute(deepLinkTerritoryId))
                    }
                }
            }
        }
    }
}
