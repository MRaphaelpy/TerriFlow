package com.mraphaelpy.terriflow.presentation.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.mraphaelpy.terriflow.presentation.components.ConfirmDialog
import com.mraphaelpy.terriflow.presentation.components.LoadingDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.model.label
import com.mraphaelpy.terriflow.core.util.DateFormats

import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTerritories: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToTerritory: (String) -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToAudit: () -> Unit,
    onNavigateToCoverage: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.updateProfilePicture(uri, context)
        }
    }

    if (showLogoutDialog && !uiState.isLoggingOut) {
        ConfirmDialog(
            title = "Sair do aplicativo",
            text = "Deseja realmente sair? Você precisará fazer login novamente.",
            confirmLabel = "Sair",
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            onConfirm = {
                viewModel.logout(onSuccess = {
                    showLogoutDialog = false
                    onLogout()
                })
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (uiState.isLoggingOut) {
        LoadingDialog("Saindo com segurança...")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TerriFlow") },
                actions = {
                    BadgedBox(
                        badge = {
                            if (uiState.unreadCount > 0) {
                                Badge { Text(uiState.unreadCount.toString()) }
                            }
                        }
                    ) {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notificações")
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações")
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sair")
                    }
                }
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item {
                uiState.currentUser?.let { user ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.mraphaelpy.terriflow.presentation.components.UserAvatar(
                            user = user,
                            size = 56.dp,
                            modifier = Modifier.clickable {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Olá, ${user.name}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.isAdmin) "Administrador" else "Responsável",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            
            if (uiState.isAdmin) {
                item {
                    Text(
                        "Administração",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminShortcutCard(
                            icon = Icons.Default.People,
                            label = "Usuários",
                            onClick = onNavigateToUsers,
                            modifier = Modifier.weight(1f)
                        )
                        AdminShortcutCard(
                            icon = Icons.Default.History,
                            label = "Auditoria",
                            onClick = onNavigateToAudit,
                            modifier = Modifier.weight(1f)
                        )
                        AdminShortcutCard(
                            icon = Icons.Default.Layers,
                            label = "Cobertura",
                            onClick = onNavigateToCoverage,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            if (uiState.overdueTerritories.isNotEmpty() && uiState.isAdmin) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.width(8.dp))
                                Text("Vencidos (> 90 dias)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(12.dp))
                            uiState.overdueTerritories.forEach { t ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onNavigateToTerritory(t.id) },
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = t.code,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = t.currentResponsibleName ?: "Sem responsável",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Resumo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                StatsGrid(uiState.stats)
            }

            if (uiState.recentEvents.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Últimas atividades",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(uiState.recentEvents.take(10)) { event ->
                    RecentEventItem(event = event, onClick = { onNavigateToTerritory(event.territoryId) })
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
            }
        }
    }
}

@Composable
private fun AdminShortcutCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatsGrid(stats: DashboardStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                label = "Disponíveis",
                count = stats.available,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Atribuídos",
                count = stats.assigned,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                label = "Em andamento",
                count = stats.inProgress,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Pausados",
                count = stats.paused,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        StatCard(
            label = "Finalizados",
            count = stats.completed,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    count: Int,
    containerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = label, 
                style = MaterialTheme.typography.labelMedium, 
                fontWeight = FontWeight.SemiBold, 
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun RecentEventItem(event: TerritoryEvent, onClick: () -> Unit) {
    val formatter = DateFormats.shortDateTime()
    val (icon, color) = when (event.type) {
        EventType.ASSIGNED -> Icons.Default.PersonAdd to MaterialTheme.colorScheme.primary
        EventType.COMPLETED -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.secondary
        EventType.PAUSED -> Icons.Default.PauseCircle to MaterialTheme.colorScheme.error
        EventType.CREATED -> Icons.Default.AddCircle to MaterialTheme.colorScheme.tertiary
        EventType.RETURNED -> Icons.AutoMirrored.Filled.Undo to MaterialTheme.colorScheme.onSurfaceVariant
        else -> Icons.Default.MoreHoriz to MaterialTheme.colorScheme.primary
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.12f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(event.type.label(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Por ${event.userName} • ${formatter.format(event.timestamp)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

private val DashboardUiState.isAdmin: Boolean get() = currentUser?.role?.name == "ADMIN" || currentUser?.role?.name == "SUPER_ADMIN"
