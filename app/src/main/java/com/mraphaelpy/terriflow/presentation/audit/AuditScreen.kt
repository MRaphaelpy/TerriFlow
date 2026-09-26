package com.mraphaelpy.terriflow.presentation.audit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.mraphaelpy.terriflow.presentation.components.FullScreenLoading
import com.mraphaelpy.terriflow.presentation.components.eventIcon
import com.mraphaelpy.terriflow.core.util.DateFormats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTerritory: (String) -> Unit,
    viewModel: AuditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auditoria Global") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            FullScreenLoading(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Últimas movimentações",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
            }
            items(uiState.events) { event ->
                AuditItem(
                    event = event,
                    user = uiState.users[event.userId],
                    onClick = { onNavigateToTerritory(event.territoryId) }
                )
            }
        }
    }
}

@Composable
private fun AuditItem(event: TerritoryEvent, user: com.mraphaelpy.terriflow.domain.model.User?, onClick: () -> Unit) {
    val formatter = DateFormats.fullDateTime()
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                com.mraphaelpy.terriflow.presentation.components.UserAvatarByName(
                    name = event.userName,
                    photoUrl = user?.photoUrl,
                    size = 48.dp
                )
                
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(20.dp).offset(x = 2.dp, y = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = eventIcon(event.type),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(event.type.label(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Por ${event.userName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                event.extra["toUserName"]?.let { to ->
                    Text("Para: $to", style = MaterialTheme.typography.bodySmall)
                }
                event.extra["territoryCode"]?.let { code ->
                     Text("Território: $code", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }
            Text(formatter.format(event.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

