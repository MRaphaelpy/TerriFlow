package com.mraphaelpy.terriflow.presentation.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.core.util.DateFormats
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.model.label
import com.mraphaelpy.terriflow.presentation.components.ConfirmDialog
import com.mraphaelpy.terriflow.presentation.components.LoadingDialog
import com.mraphaelpy.terriflow.presentation.components.TerritoryShapeBadge
import com.mraphaelpy.terriflow.presentation.components.TerritoryStatusDisplayOrder
import com.mraphaelpy.terriflow.presentation.components.UserAvatar
import com.mraphaelpy.terriflow.presentation.components.eventIcon
import com.mraphaelpy.terriflow.presentation.components.segmentedShape
import com.mraphaelpy.terriflow.presentation.components.statusColor
import com.mraphaelpy.terriflow.presentation.territorydetail.statusSince

private val BottomNavClearance = 112.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.updateProfilePicture(uri)
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

    val user = uiState.currentUser
    if (showEditNameDialog && user != null) {
        EditNameDialog(
            currentName = user.name,
            onSave = { name ->
                viewModel.updateMyName(name)
                showEditNameDialog = false
            },
            onDismiss = { showEditNameDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(greeting(user), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                subtitle = { user?.let { Text(it.role.label()) } },
                actions = {
                    BadgedBox(
                        badge = {
                            if (uiState.unreadCount > 0) Badge { Text(uiState.unreadCount.toString()) }
                        }
                    ) {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notificações")
                        }
                    }
                    if (user != null) {
                        Box {
                            IconButton(onClick = { showProfileMenu = true }) {
                                UserAvatar(user = user, size = 32.dp)
                            }
                            ProfileMenu(
                                expanded = showProfileMenu,
                                onDismiss = { showProfileMenu = false },
                                onChangePhoto = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                onEditName = { showEditNameDialog = true },
                                onSettings = onNavigateToSettings,
                                onLogout = { showLogoutDialog = true }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.padding(bottom = BottomNavClearance)) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = BottomNavClearance),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "summary") {
                    SummaryCard(
                        total = uiState.totalTerritories,
                        statusCounts = uiState.statusCounts,
                        onClick = onNavigateToTerritories
                    )
                }

                if (uiState.myTerritories.isNotEmpty()) {
                    item(key = "mine-title") { SectionTitle("Seus territórios") }
                    item(key = "mine") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(uiState.myTerritories, key = { it.id }) { territory ->
                                MyTerritoryCard(
                                    territory = territory,
                                    color = uiState.palette[territory.id] ?: statusColor(territory.status),
                                    onClick = { onNavigateToTerritory(territory.id) }
                                )
                            }
                        }
                    }
                }

                if (uiState.isAdmin && uiState.overdueTerritories.isNotEmpty()) {
                    item(key = "overdue") {
                        OverdueCard(uiState.overdueTerritories, onNavigateToTerritory, viewModel::remind)
                    }
                }

                if (uiState.isAdmin) {
                    item(key = "admin-title") { SectionTitle("Administração") }
                    item(key = "admin") {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ShortcutTile(Icons.Default.People, "Usuários", MaterialShapes.Cookie4Sided.toShape(), onNavigateToUsers, Modifier.weight(1f))
                            ShortcutTile(Icons.Default.History, "Auditoria", MaterialShapes.Clover4Leaf.toShape(), onNavigateToAudit, Modifier.weight(1f))
                            ShortcutTile(Icons.Default.Layers, "Cobertura", MaterialShapes.SoftBurst.toShape(), onNavigateToCoverage, Modifier.weight(1f))
                        }
                    }
                }

                if (uiState.recentActivity.isNotEmpty()) {
                    item(key = "activity-title") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle("Atividade recente", Modifier.weight(1f))
                            if (uiState.isAdmin) {
                                TextButton(onClick = onNavigateToAudit) { Text("Ver tudo") }
                            }
                        }
                    }
                    itemsIndexed(uiState.recentActivity, key = { _, activity -> activity.event.id }) { index, activity ->
                        ActivityRow(
                            activity = activity,
                            color = activity.territory?.let { uiState.palette[it.id] } ?: MaterialTheme.colorScheme.primary,
                            shape = segmentedShape(index, uiState.recentActivity.size),
                            onClick = { onNavigateToTerritory(activity.event.territoryId) }
                        )
                    }
                }
            }
        }
    }
}

private fun greeting(user: User?): String =
    user?.name?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotEmpty() }?.let { "Olá, $it" } ?: "TerriFlow"

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun ProfileMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onChangePhoto: () -> Unit,
    onEditName: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        val item: @Composable (String, ImageVector, Color?, () -> Unit) -> Unit = { text, icon, tint, action ->
            DropdownMenuItem(
                text = { Text(text, color = tint ?: Color.Unspecified) },
                leadingIcon = { Icon(icon, contentDescription = null, tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = {
                    onDismiss()
                    action()
                }
            )
        }
        item("Alterar foto", Icons.Default.PhotoCamera, null, onChangePhoto)
        item("Editar nome", Icons.Default.Edit, null, onEditName)
        item("Configurações", Icons.Default.Settings, null, onSettings)
        HorizontalDivider()
        item("Sair", Icons.AutoMirrored.Filled.Logout, MaterialTheme.colorScheme.error, onLogout)
    }
}

@Composable
private fun SummaryCard(total: Int, statusCounts: Map<TerritoryStatus, Int>, onClick: () -> Unit) {
    val ordered = TerritoryStatusDisplayOrder.filter { (statusCounts[it] ?: 0) > 0 }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "$total",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "territórios",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (total > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(CircleShape),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    ordered.forEach { status ->
                        Box(
                            modifier = Modifier
                                .weight((statusCounts[status] ?: 0).toFloat())
                                .fillMaxSize()
                                .background(statusColor(status), RoundedCornerShape(3.dp))
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ordered.forEach { status ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(10.dp).background(statusColor(status), CircleShape))
                            Text(
                                "${statusCounts[status]} ${status.label().lowercase()}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Nenhum território cadastrado ainda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun MyTerritoryCard(territory: Territory, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(200.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TerritoryShapeBadge(code = territory.code, color = color, size = 44.dp)
                Column {
                    Text(territory.code, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        territory.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).background(statusColor(territory.status), CircleShape))
                Text(territory.status.label(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            val since = statusSince(territory)
            if (since.isNotEmpty()) {
                Text(since, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun OverdueCard(territories: List<Territory>, onTerritoryClick: (String) -> Unit, onRemind: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    "Com o dirigente há mais de 4 meses",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            territories.forEach { territory ->
                Surface(
                    onClick = { onTerritoryClick(territory.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(territory.code, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(territory.currentResponsibleName ?: "Sem dirigente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        TextButton(onClick = { onRemind(territory.id) }) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cobrar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutTile(icon: ImageVector, label: String, shape: Shape, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.tertiaryContainer, shape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun ActivityRow(activity: RecentActivity, color: Color, shape: Shape, onClick: () -> Unit) {
    val event = activity.event
    val territory = activity.territory
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(eventIcon(event.type), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    event.type.label(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    territory?.let { "${it.code} · ${it.name}" } ?: "Território removido",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${event.userName} · ${DateFormats.shortDateTime().format(event.timestamp)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EditNameDialog(currentName: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
        title = { Text("Editar meu nome") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome completo") },
                singleLine = true,
                isError = name.isBlank(),
                supportingText = { if (name.isBlank()) Text("O nome não pode ficar vazio") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(name) },
                enabled = name.isNotBlank() && name.trim() != currentName
            ) { Text("Salvar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
