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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.mraphaelpy.terriflow.domain.repository.AppColor
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import com.mraphaelpy.terriflow.domain.repository.SettingsRepository
import com.mraphaelpy.terriflow.domain.repository.ThemeMode
import com.mraphaelpy.terriflow.presentation.navigation.AppNavigation
import com.mraphaelpy.terriflow.presentation.navigation.Screen
import com.mraphaelpy.terriflow.ui.theme.TerriFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var congregationRepository: CongregationRepository
    @Inject lateinit var updateManager: com.mraphaelpy.terriflow.core.updater.AppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deepLinkTerritoryId = intent?.getStringExtra("territoryId")

        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val appColor by settingsRepository.appColor.collectAsState(initial = AppColor.DEFAULT)

            // Solicitação de permissão de notificação (Android 13+)
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasPermission) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                // Garante que o token FCM do aparelho é registrado para receber notificações
                if (auth.currentUser != null) {
                    runCatching {
                        val token = FirebaseMessaging.getInstance().token.await()
                        authRepository.updateFcmToken(token)
                    }
                }
            }

            TerriFlowTheme(themeMode = themeMode, appColor = appColor) {
                Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var startDestination by remember { mutableStateOf<String?>(null) }
                    var availableUpdate by remember { mutableStateOf<com.mraphaelpy.terriflow.core.updater.AppUpdateInfo?>(null) }

                    LaunchedEffect(Unit) {
                        // Verifica se existe atualização disponível ao abrir o app (apenas se o usuário não tiver adiado esta versão)
                        val updateInfo = updateManager.checkForUpdate()
                        if (updateInfo != null && updateInfo.hasUpdate && !updateManager.isVersionDismissed(updateInfo.latestVersion)) {
                            availableUpdate = updateInfo
                        }

                        if (auth.currentUser == null) {
                            startDestination = Screen.Login.route
                        } else {
                            val congId = congregationRepository.getCurrentCongregationId()
                            startDestination = if (congId != null) Screen.Dashboard.route else Screen.CongregationSetup.route
                        }
                    }

                    startDestination?.let { destination ->
                        val navController = rememberNavController()

                        AppNavigation(
                            navController = navController,
                            startDestination = destination
                        )

                        if (deepLinkTerritoryId != null && auth.currentUser != null) {
                            navController.navigate(Screen.TerritoryDetail.createRoute(deepLinkTerritoryId))
                        }
                    }

                    // Exibe o diálogo de atualização se houver uma nova versão
                    availableUpdate?.let { info ->
                        com.mraphaelpy.terriflow.core.updater.UpdateDialog(
                            updateInfo = info,
                            onDismiss = {
                                updateManager.dismissVersion(info.latestVersion)
                                availableUpdate = null
                            },
                            updateManager = updateManager
                        )
                    }
                }
            }
        }
    }
}
