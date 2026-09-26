package com.mraphaelpy.terriflow.presentation.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerenciar Usuários") },
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

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isSuperAdmin = uiState.currentUser?.role == UserRole.SUPER_ADMIN
            items(uiState.users) { user ->
                UserCard(
                    user = user,
                    isSuperAdmin = isSuperAdmin,
                    onToggleRole = { viewModel.toggleUserRole(user) },
                    onToggleStatus = { viewModel.toggleUserStatus(user) }
                )
            }
        }
    }
}

@Composable
private fun UserCard(
    user: User,
    isSuperAdmin: Boolean,
    onToggleRole: () -> Unit,
    onToggleStatus: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val roleIcon = when (user.role) {
                UserRole.SUPER_ADMIN -> Icons.Default.AdminPanelSettings
                UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                else -> Icons.Default.Person
            }
            val roleColor = when (user.role) {
                UserRole.SUPER_ADMIN, UserRole.ADMIN -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val roleText = when (user.role) {
                UserRole.SUPER_ADMIN -> "Super Admin"
                UserRole.ADMIN -> "Administrador"
                else -> "Dirigente"
            }
            
            Box(contentAlignment = Alignment.BottomEnd) {
                UserAvatar(user = user, size = 56.dp)
                
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(24.dp).offset(x = 4.dp, y = 4.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = roleIcon,
                            contentDescription = null,
                            tint = roleColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = roleText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (isSuperAdmin && user.role != UserRole.SUPER_ADMIN) {
                    TextButton(onClick = onToggleRole) {
                        Text(if (user.role == UserRole.ADMIN) "Rebaixar" else "Tornar Admin")
                    }
                }
                if (user.role != UserRole.SUPER_ADMIN) {
                    TextButton(onClick = onToggleStatus) {
                        Text(if (user.active) "Desativar" else "Ativar", color = if (user.active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
