package com.mraphaelpy.terriflow.presentation.users

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.model.UserRole
import com.mraphaelpy.terriflow.presentation.components.FullScreenLoading
import com.mraphaelpy.terriflow.presentation.components.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    onNavigateBack: () -> Unit,
    viewModel: UsersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    // Dialog states
    var editingUser by remember { mutableStateOf<User?>(null) }
    var userToChangeRole by remember { mutableStateOf<Pair<User, UserRole>?>(null) }
    var userToToggleStatus by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gerenciar Usuários", fontWeight = FontWeight.Bold)
                        Text(
                            "${uiState.users.size} membro(s) na congregação",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            FullScreenLoading(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Barra de Busca
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Buscar por nome ou e-mail...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Chips de Filtro
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.filterRole == null,
                        onClick = { viewModel.onFilterRoleSelected(null) },
                        label = { Text("Todos (${uiState.users.size})") }
                    )
                }
                item {
                    val respCount = uiState.users.count { it.role == UserRole.RESPONSIBLE }
                    FilterChip(
                        selected = uiState.filterRole == UserRole.RESPONSIBLE,
                        onClick = { viewModel.onFilterRoleSelected(UserRole.RESPONSIBLE) },
                        label = { Text("Responsáveis ($respCount)") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
                item {
                    val adminCount = uiState.users.count { it.role in listOf(UserRole.ADMIN, UserRole.SUPER_ADMIN) }
                    FilterChip(
                        selected = uiState.filterRole == UserRole.ADMIN,
                        onClick = { viewModel.onFilterRoleSelected(UserRole.ADMIN) },
                        label = { Text("Administradores ($adminCount)") },
                        leadingIcon = {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (uiState.filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Nenhum usuário encontrado",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.filteredUsers, key = { it.id }) { user ->
                        val isSelf = uiState.currentUser?.id == user.id
                        UserCard(
                            user = user,
                            isSelf = isSelf,
                            canManage = uiState.isAdmin,
                            isSuperAdmin = uiState.isSuperAdmin,
                            onEditName = { editingUser = user },
                            onChangeRole = { newRole -> userToChangeRole = user to newRole },
                            onToggleStatus = { userToToggleStatus = user },
                            onCopyEmail = {
                                clipboardManager.setText(AnnotatedString(user.email))
                                snackbarHostState.currentSnackbarData?.dismiss()
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Editar Nome
    editingUser?.let { targetUser ->
        EditUserNameDialog(
            user = targetUser,
            isSelf = uiState.currentUser?.id == targetUser.id,
            onDismiss = { editingUser = null },
            onConfirm = { newName ->
                viewModel.updateUserName(targetUser, newName)
                editingUser = null
            }
        )
    }

    // Dialog: Confirmar Alteração de Função
    userToChangeRole?.let { (targetUser, newRole) ->
        val roleLabel = when (newRole) {
            UserRole.SUPER_ADMIN -> "Super Administrador"
            UserRole.ADMIN -> "Administrador"
            UserRole.RESPONSIBLE -> "Responsável"
        }
        val explanation = when (newRole) {
            UserRole.ADMIN -> "Ao tornar este irmão Administrador, ele poderá gerenciar territórios, atribuir designações e administrar outros usuários da congregação."
            UserRole.RESPONSIBLE -> "Ao tornar este irmão Responsável, ele continuará podendo ser designado para territórios e atualizar o progresso de suas designações."
            else -> ""
        }

        AlertDialog(
            onDismissRequest = { userToChangeRole = null },
            icon = {
                Icon(
                    imageVector = if (newRole == UserRole.ADMIN) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Tornar $roleLabel?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Deseja alterar o papel de ${targetUser.name} para $roleLabel?")
                    if (explanation.isNotBlank()) {
                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUserRole(targetUser, newRole)
                        userToChangeRole = null
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userToChangeRole = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog: Confirmar Ativação / Desativação
    userToToggleStatus?.let { targetUser ->
        val isActivating = !targetUser.active
        AlertDialog(
            onDismissRequest = { userToToggleStatus = null },
            icon = {
                Icon(
                    imageVector = if (isActivating) Icons.Default.CheckCircle else Icons.Default.PersonOff,
                    contentDescription = null,
                    tint = if (isActivating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text(if (isActivating) "Reativar usuário?" else "Desativar usuário?") },
            text = {
                Text(
                    if (isActivating)
                        "Deseja reativar o acesso de ${targetUser.name}? Ele voltará a ter acesso aos territórios e recursos da congregação."
                    else
                        "Deseja desativar ${targetUser.name}? Ele não poderá mais acessar os territórios nem receber novas designações até ser reativado."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleUserStatus(targetUser)
                        userToToggleStatus = null
                    },
                    colors = if (!isActivating) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(if (isActivating) "Reativar" else "Desativar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userToToggleStatus = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun UserCard(
    user: User,
    isSelf: Boolean,
    canManage: Boolean,
    isSuperAdmin: Boolean,
    onEditName: () -> Unit,
    onChangeRole: (UserRole) -> Unit,
    onToggleStatus: () -> Unit,
    onCopyEmail: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (user.active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (user.active) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar com badge de função
            Box(contentAlignment = Alignment.BottomEnd) {
                UserAvatar(user = user, size = 52.dp)

                val badgeIcon = when (user.role) {
                    UserRole.SUPER_ADMIN -> Icons.Default.Shield
                    UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                    UserRole.RESPONSIBLE -> Icons.Default.Person
                }
                val badgeColor = when (user.role) {
                    UserRole.SUPER_ADMIN -> MaterialTheme.colorScheme.tertiary
                    UserRole.ADMIN -> MaterialTheme.colorScheme.primary
                    UserRole.RESPONSIBLE -> MaterialTheme.colorScheme.secondary
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(20.dp).offset(x = 4.dp, y = 4.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Informações do Usuário
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = user.name.ifBlank { "Sem nome" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isSelf) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Você",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (user.email.isNotBlank()) {
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Tags de Papel e Status
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (user.role) {
                        UserRole.SUPER_ADMIN -> {
                            AssistChip(
                                onClick = {},
                                label = { Text("Super Admin") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                border = null,
                                modifier = Modifier.height(24.dp)
                            )
                        }
                        UserRole.ADMIN -> {
                            AssistChip(
                                onClick = {},
                                label = { Text("Administrador") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = null,
                                modifier = Modifier.height(24.dp)
                            )
                        }
                        UserRole.RESPONSIBLE -> {
                            AssistChip(
                                onClick = {},
                                label = { Text("Responsável") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = null,
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }

                    if (!user.active) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "Inativo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Menu de Opções
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Mais opções",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    // Opção 1: Trocar Nome
                    if (isSelf || canManage) {
                        DropdownMenuItem(
                            text = { Text(if (isSelf) "Editar meu nome" else "Trocar nome") },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                menuExpanded = false
                                onEditName()
                            }
                        )
                    }

                    // Opção 2: Tornar Responsável / Administrador
                    if (canManage && (!isSelf || isSuperAdmin)) {
                        val isTargetSuperAdmin = user.role == UserRole.SUPER_ADMIN

                        if (!isTargetSuperAdmin || isSuperAdmin) {
                            if (user.role == UserRole.ADMIN) {
                                DropdownMenuItem(
                                    text = { Text("Tornar Responsável") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onChangeRole(UserRole.RESPONSIBLE)
                                    }
                                )
                            } else if (user.role == UserRole.RESPONSIBLE) {
                                DropdownMenuItem(
                                    text = { Text("Tornar Administrador") },
                                    leadingIcon = {
                                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onChangeRole(UserRole.ADMIN)
                                    }
                                )
                            }
                        }
                    }

                    // Opção 3: Ativar / Desativar
                    if (canManage && !isSelf && user.role != UserRole.SUPER_ADMIN) {
                        DropdownMenuItem(
                            text = { Text(if (user.active) "Desativar usuário" else "Reativar usuário") },
                            leadingIcon = {
                                Icon(
                                    if (user.active) Icons.Default.PersonOff else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (user.active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onToggleStatus()
                            }
                        )
                    }

                    // Opção 4: Copiar E-mail
                    if (user.email.isNotBlank()) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Copiar e-mail") },
                            leadingIcon = {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onCopyEmail()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditUserNameDialog(
    user: User,
    isSelf: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(user.name) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(if (isSelf) "Editar Meu Nome" else "Trocar Nome de Usuário")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isSelf) "Altere seu nome de exibição no aplicativo:" else "Altere o nome cadastrado para este irmão:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        isError = it.isBlank()
                    },
                    label = { Text("Nome completo") },
                    singleLine = true,
                    isError = isError,
                    supportingText = {
                        if (isError) Text("O nome não pode ficar vazio")
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        onConfirm(newName.trim())
                    } else {
                        isError = true
                    }
                },
                enabled = newName.isNotBlank() && newName.trim() != user.name
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
