package com.mraphaelpy.terriflow.core.updater

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

private sealed interface UpdateStep {
    data object Ready : UpdateStep
    data class Downloading(val progress: Float) : UpdateStep
    data class NeedsPermission(val file: File) : UpdateStep
    data class ReadyToInstall(val file: File) : UpdateStep
    data class Failed(val message: String) : UpdateStep
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit,
    updateManager: AppUpdateManager
) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf<UpdateStep>(UpdateStep.Ready) }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun install(file: File) {
        if (updateManager.canInstallPackages()) {
            step = UpdateStep.ReadyToInstall(file)
            updateManager.installApk(file)
        } else {
            step = UpdateStep.NeedsPermission(file)
            updateManager.openInstallPermissionSettings()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val waiting = step as? UpdateStep.NeedsPermission
            if (event == Lifecycle.Event.ON_RESUME && waiting != null && updateManager.canInstallPackages()) {
                install(waiting.file)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val busy = step is UpdateStep.Downloading
    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy)
    ) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.size(16.dp))
                Text("Nova versão disponível", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "v${updateInfo.currentVersion} → v${updateInfo.latestVersion}" +
                        (if (updateInfo.apkSize > 0) " · ${formatSize(updateInfo.apkSize)}" else ""),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.size(16.dp))
                Text("O que mudou", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).heightIn(min = 56.dp, max = 200.dp)
                ) {
                    Text(
                        updateInfo.changelog,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())
                    )
                }
                Spacer(Modifier.size(20.dp))

                when (val current = step) {
                    is UpdateStep.Downloading -> Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearWavyProgressIndicator(progress = { current.progress }, modifier = Modifier.fillMaxWidth())
                        Text(
                            "Baixando: ${(current.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    is UpdateStep.NeedsPermission -> StepMessage(
                        icon = Icons.Default.Security,
                        text = "Permita que o TerriFlow instale atualizações e volte para o app. A instalação continua automaticamente.",
                        actionLabel = "Abrir permissão",
                        onAction = { updateManager.openInstallPermissionSettings() },
                        onDismiss = onDismiss
                    )
                    is UpdateStep.ReadyToInstall -> StepMessage(
                        icon = Icons.Default.InstallMobile,
                        text = "Download concluído. Confirme a instalação na tela do Android.",
                        actionLabel = "Instalar",
                        onAction = { install(current.file) },
                        onDismiss = onDismiss
                    )
                    UpdateStep.Ready, is UpdateStep.Failed -> {
                        (current as? UpdateStep.Failed)?.let {
                            Text(it.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 12.dp))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Depois") }
                            Button(
                                onClick = {
                                    step = UpdateStep.Downloading(0f)
                                    scope.launch {
                                        when (val result = updateManager.downloadApk(updateInfo) { step = UpdateStep.Downloading(it) }) {
                                            is DownloadResult.Success -> install(result.file)
                                            is DownloadResult.Failure -> step = UpdateStep.Failed(result.message)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (current is UpdateStep.Failed) "Tentar de novo" else "Atualizar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepMessage(icon: ImageVector, text: String, actionLabel: String, onAction: () -> Unit, onDismiss: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Depois") }
            Button(onClick = onAction, modifier = Modifier.weight(1.5f)) { Text(actionLabel) }
        }
    }
}

private fun formatSize(bytes: Long): String = String.format(Locale("pt", "BR"), "%.1f MB", bytes / 1_048_576.0)
