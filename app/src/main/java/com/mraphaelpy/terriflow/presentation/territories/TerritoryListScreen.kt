package com.mraphaelpy.terriflow.presentation.territories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.core.util.DateFormats
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.label
import com.mraphaelpy.terriflow.presentation.components.FullScreenLoading
import com.mraphaelpy.terriflow.presentation.components.StatusChip
import com.mraphaelpy.terriflow.presentation.components.UserAvatarByName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerritoryListScreen(
    onNavigateToTerritory: (String) -> Unit,
    onCreateTerritory: () -> Unit,
    onImportKml: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: TerritoryListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Territórios") },

                actions = {
                    if (uiState.filter.query.isNotEmpty() || uiState.filter.status.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearFilters() }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar filtros")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.isAdmin) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 80.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = onImportKml,
                        shape = MaterialTheme.shapes.large,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Importar KML/KMZ")
                    }
                    ExtendedFloatingActionButton(
                        onClick = onCreateTerritory,
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Novo") },
                        shape = MaterialTheme.shapes.extraLarge
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            SearchBar(
                query = uiState.filter.query,
                onQueryChange = { viewModel.updateQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Status filters
            StatusFilterRow(
                selectedStatus = uiState.filter.status,
                onStatusSelected = { viewModel.updateStatusFilter(it) }
            )

            HorizontalDivider()

            if (uiState.isLoading) {
                FullScreenLoading()
            } else if (uiState.territories.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (uiState.filter.query.isNotEmpty() || uiState.filter.status.isNotEmpty())
                            "Nenhum território encontrado"
                        else "Nenhum território cadastrado",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.territories, key = { it.id }) { territory ->
                        TerritoryListItem(
                            territory = territory,
                            currentUserId = uiState.currentUserId,
                            onClick = { onNavigateToTerritory(territory.id) },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(160.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Buscar código, nome ou bairro...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        modifier = modifier
    )
}

@Composable
private fun StatusFilterRow(selectedStatus: String, onStatusSelected: (String) -> Unit) {
    val statuses = listOf("" to "Todos") + TerritoryStatus.entries.map { it.name to it.label() }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(statuses) { (value, label) ->
            FilterChip(
                selected = selectedStatus == value,
                onClick = { onStatusSelected(value) },
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerritoryListItem(territory: Territory, currentUserId: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isMine = territory.currentResponsibleId == currentUserId && currentUserId != null

    val headerColor = when (territory.status) {
        TerritoryStatus.AVAILABLE   -> MaterialTheme.colorScheme.secondaryContainer
        TerritoryStatus.ASSIGNED    -> MaterialTheme.colorScheme.tertiaryContainer
        TerritoryStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer
        TerritoryStatus.PAUSED      -> MaterialTheme.colorScheme.surfaceVariant
        TerritoryStatus.COMPLETED   -> MaterialTheme.colorScheme.secondaryContainer
        TerritoryStatus.RETURNED    -> MaterialTheme.colorScheme.errorContainer
        TerritoryStatus.ARCHIVED    -> MaterialTheme.colorScheme.surfaceVariant
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp, vertical = 5.dp),
        onClick = onClick,
        shape = RoundedCornerShape(5.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isMine) 4.dp else 2.dp
        ),
    ) {
        Column {
            // Header colorido por status
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            territory.code,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isMine) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(10.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("MEU", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    StatusChip(territory.status)
                }
            }

            // Nome e localização
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = territory.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (territory.location.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            territory.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Rodapé: dirigente + data
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (territory.currentResponsibleName != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        UserAvatarByName(name = territory.currentResponsibleName, photoUrl = territory.currentResponsiblePhotoUrl, size = 26.dp)
                        Column {
                            Text("Dirigente", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(
                                territory.currentResponsibleName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Text(
                        "Sem dirigente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.width(8.dp))
                val formatter = DateFormats.shortDate()
                Text(
                    text = formatter.format(territory.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

