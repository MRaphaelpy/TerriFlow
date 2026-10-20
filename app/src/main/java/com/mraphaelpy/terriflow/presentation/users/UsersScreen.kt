package com.mraphaelpy.terriflow.presentation.users

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.model.UserRole
import com.mraphaelpy.terriflow.domain.model.label
import com.mraphaelpy.terriflow.presentation.components.FullScreenLoading
import com.mraphaelpy.terriflow.presentation.components.PillSearchField
import com.mraphaelpy.terriflow.presentation.components.UserAvatar

private sealed interface PendingAction {
    data class Rename(val user: User) : PendingAction
    data class ChangeRole(val user: User, val role: UserRole) : PendingAction
    data class ToggleActive(val user: User) : PendingAction
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UsersScreen(
    onNavigateBack: () -> Unit,
    viewModel: UsersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var pending by remember { mutableStateOf<PendingAction?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    selectedUser?.let { user ->
        UserActionsSheet(
            user = user,
            uiState = uiState,
            onDismiss = { selectedUser = null },
            onAction = { action ->
                selectedUser = null
                pending = action
            },
            onCopyEmail = {
                selectedUser = null
                clipboard.setText(AnnotatedString(user.email))
            }
        )
    }

    when (val action = pending) {
        is PendingAction.Rename -> EditUserNameDialog(
            user = action.user,
            isSelf = uiState.currentUser?.id == action.user.id,
            onDismiss = { pending = null },
            onConfirm = { name ->
                viewModel.updateUserName(action.user, name)
                pending = null
            }
        )
        is PendingAction.ChangeRole -> ChangeRoleDialog(
            user = action.user,
            role = action.role,
            onDismiss = { pending = null },
            onConfirm = {
                viewModel.updateUserRole(action.user, action.role)
                pending = null
            }
        )
        is PendingAction.ToggleActive -> ToggleActiveDialog(
            user = action.user,
            onDismiss = { pending = null },
            onConfirm = {
                viewModel.toggleUserStatus(action.user)
                pending = null
            }
        )
        null -> Unit
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Usuários") },
                subtitle = {
                    Text(
                        if (uiState.visibleCount == uiState.total) "${uiState.total} membros na congregação"
                        else "${uiState.visibleCount} de ${uiState.total} membros"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            FullScreenLoading(Modifier.padding(padding))
            return@Scaffold
        }
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PillSearchField(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = "Buscar por nome ou e-mail",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(UserFilter.entries.filter { it == UserFilter.ALL || (uiState.counts[it] ?: 0) > 0 }) { filter ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { viewModel.onFilterSelected(filter) },
                        label = { Text("${filter.label} (${uiState.counts[filter] ?: 0})") },
                        shape = CircleShape
                    )
                }
            }

            if (uiState.sections.isEmpty()) {
                EmptyUsers()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    uiState.sections.forEach { section ->
                        item(key = "header-${section.title}") {
                            Text(
                                "${section.title} · ${section.users.size}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 8.dp).animateItem()
                            )
                        }
                        itemsIndexed(section.users, key = { _, user -> user.id }) { index, user ->
                            UserRow(
                                user = user,
                                isSelf = uiState.currentUser?.id == user.id,
                                territories = uiState.currentTerritories[user.id] ?: 0,
                                shape = segmentShape(index, section.users.size),
                                onClick = { selectedUser = user },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRow(
    user: User,
    isSelf: Boolean,
    territories: Int,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = if (isSelf) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AvatarWithRole(user, size = 48)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        user.name.ifBlank { "Sem nome" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isSelf) Pill("Você", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                }
                if (user.email.isNotBlank()) {
                    Text(
                        user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    listOf(
                        user.role.label(),
                        when (territories) {
                            0 -> "sem territórios"
                            1 -> "1 território"
                            else -> "$territories territórios"
                        }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AvatarWithRole(user: User, size: Int) {
    Box(contentAlignment = Alignment.BottomEnd) {
        UserAvatar(user = user, size = size.dp)
        val (icon, color) = roleBadge(user.role)
        Box(
            modifier = Modifier
                .size((size * 0.42f).dp)
                .offset(x = 4.dp, y = 4.dp)
                .background(MaterialTheme.colorScheme.surface, MaterialShapes.Cookie6Sided.toShape())
                .padding(2.dp)
                .background(color, MaterialShapes.Cookie6Sided.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = user.role.label(), tint = Color.White, modifier = Modifier.size((size * 0.24f).dp))
        }
    }
}

@Composable
private fun roleBadge(role: UserRole): Pair<ImageVector, Color> = when (role) {
    UserRole.SUPER_ADMIN -> Icons.Default.Shield to MaterialTheme.colorScheme.tertiary
    UserRole.ADMIN -> Icons.Default.AdminPanelSettings to MaterialTheme.colorScheme.primary
    UserRole.RESPONSIBLE -> Icons.Default.Person to MaterialTheme.colorScheme.secondary
}

@Composable
private fun Pill(text: String, container: Color, content: Color) {
    Surface(shape = CircleShape, color = container) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserActionsSheet(
    user: User,
    uiState: UsersUiState,
    onDismiss: () -> Unit,
    onAction: (PendingAction) -> Unit,
    onCopyEmail: () -> Unit
) {
    val isSelf = uiState.currentUser?.id == user.id
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AvatarWithRole(user, size = 64)
                Column {
                    Text(user.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        listOfNotNull(user.role.label(), "Inativo".takeIf { !user.active }).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (uiState.canRename(user)) {
                SheetAction(Icons.Default.Edit, if (isSelf) "Editar meu nome" else "Trocar nome") {
                    onAction(PendingAction.Rename(user))
                }
            }
            uiState.roleChanges(user).forEach { role ->
                SheetAction(
                    if (role == UserRole.ADMIN) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                    "Tornar ${role.label()}"
                ) { onAction(PendingAction.ChangeRole(user, role)) }
            }
            if (uiState.canToggleActive(user)) {
                SheetAction(
                    if (user.active) Icons.Default.PersonOff else Icons.Default.CheckCircle,
                    if (user.active) "Desativar usuário" else "Reativar usuário",
                    tint = if (user.active) MaterialTheme.colorScheme.error else null
                ) { onAction(PendingAction.ToggleActive(user)) }
            }
            if (user.email.isNotBlank()) {
                SheetAction(Icons.Default.ContentCopy, "Copiar e-mail", onClick = onCopyEmail)
            }
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, tint: Color? = null, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, color = tint ?: Color.Unspecified) },
        leadingContent = { Icon(icon, contentDescription = null, tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyUsers() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(112.dp).background(MaterialTheme.colorScheme.secondaryContainer, MaterialShapes.Cookie12Sided.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.height(20.dp))
        Text("Nenhum usuário encontrado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ChangeRoleDialog(user: User, role: UserRole, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val explanation = when (role) {
        UserRole.ADMIN -> "Poderá gerenciar territórios, fazer designações e administrar outros usuários da congregação."
        UserRole.RESPONSIBLE -> "Continuará podendo ser designado para territórios e atualizar o progresso das suas designações."
        UserRole.SUPER_ADMIN -> ""
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(if (role == UserRole.ADMIN) Icons.Default.AdminPanelSettings else Icons.Default.Person, contentDescription = null) },
        title = { Text("Tornar ${role.label()}?") },
        text = { Text("${user.name}: $explanation") },
        confirmButton = { Button(onClick = onConfirm) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ToggleActiveDialog(user: User, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val activating = !user.active
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(if (activating) Icons.Default.CheckCircle else Icons.Default.PersonOff, contentDescription = null) },
        title = { Text(if (activating) "Reativar usuário?" else "Desativar usuário?") },
        text = {
            Text(
                if (activating) "${user.name} voltará a ter acesso aos territórios da congregação."
                else "${user.name} não poderá mais acessar os territórios nem receber designações até ser reativado."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (activating) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(if (activating) "Reativar" else "Desativar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun EditUserNameDialog(user: User, isSelf: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(user.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
        title = { Text(if (isSelf) "Editar meu nome" else "Trocar nome") },
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
            Button(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank() && name.trim() != user.name) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun segmentShape(index: Int, count: Int): RoundedCornerShape {
    val top = if (index == 0) 24.dp else 6.dp
    val bottom = if (index == count - 1) 24.dp else 6.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}
