package com.mraphaelpy.terriflow.presentation.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.domain.model.AppNotification
import com.mraphaelpy.terriflow.presentation.components.EmptyState
import com.mraphaelpy.terriflow.presentation.components.FullScreenLoading
import com.mraphaelpy.terriflow.core.util.DateFormats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateToTerritory: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificações") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (uiState.notifications.any { !it.read }) {
                        IconButton(onClick = { viewModel.markAllRead() }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Marcar todas como lidas")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            FullScreenLoading(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        if (uiState.notifications.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Notifications,
                title = "Nenhuma notificação",
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.notifications, key = { it.id }) { notification ->
                NotificationItem(
                    notification = notification,
                    onClick = {
                        viewModel.markRead(notification.id)
                        notification.territoryId?.let { onNavigateToTerritory(it) }
                    }
                )
            }
        }
    }
}

@Composable
private fun NotificationItem(notification: AppNotification, onClick: () -> Unit) {
    val formatter = DateFormats.shortDateTime()
    
    val containerColor = if (!notification.read) 
        MaterialTheme.colorScheme.primaryContainer 
    else 
        MaterialTheme.colorScheme.surfaceContainerLow

    val contentColor = if (!notification.read)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (!notification.read) 4.dp else 1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (!notification.read) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (!notification.read) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (!notification.read) Icons.Default.NotificationsActive else Icons.Default.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (!notification.read) FontWeight.Bold else FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (!notification.read) contentColor.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = formatter.format(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (!notification.read) contentColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            if (!notification.read) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(10.dp)
                ) {}
            }
        }
    }
}
