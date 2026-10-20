package com.mraphaelpy.terriflow.presentation.territories

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.label
import com.mraphaelpy.terriflow.presentation.components.FullScreenLoading
import com.mraphaelpy.terriflow.presentation.components.PillSearchField
import com.mraphaelpy.terriflow.presentation.components.TerritoryFilterChips
import com.mraphaelpy.terriflow.presentation.components.TerritoryShapeBadge
import com.mraphaelpy.terriflow.presentation.components.UserAvatarByName
import com.mraphaelpy.terriflow.presentation.components.statusColor

private val BottomNavClearance = 112.dp
private val FabBottomOffset = 32.dp
private val OuterCorner = 24.dp
private val InnerCorner = 6.dp
private val PressedCorner = 32.dp
private val BlockProgressStatuses = setOf(TerritoryStatus.IN_PROGRESS, TerritoryStatus.PAUSED)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TerritoryListScreen(
    onNavigateToTerritory: (String) -> Unit,
    onCreateTerritory: () -> Unit,
    onImportKml: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: TerritoryListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Territórios") },
                subtitle = { Text(summary(uiState)) },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (uiState.isAdmin) {
                FloatingActionButtonMenu(
                    expanded = fabMenuExpanded,
                    modifier = Modifier.padding(bottom = FabBottomOffset),
                    button = {
                        ToggleFloatingActionButton(
                            checked = fabMenuExpanded,
                            onCheckedChange = { fabMenuExpanded = it }
                        ) {
                            Icon(
                                imageVector = if (checkedProgress > 0.5f) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = if (fabMenuExpanded) "Fechar menu" else "Adicionar territórios",
                                modifier = Modifier.graphicsLayer { rotationZ = checkedProgress * 90f }
                            )
                        }
                    }
                ) {
                    FloatingActionButtonMenuItem(
                        onClick = {
                            fabMenuExpanded = false
                            onImportKml()
                        },
                        text = { Text("Importar KML/KMZ") },
                        icon = { Icon(Icons.Default.FolderOpen, contentDescription = null) }
                    )
                    FloatingActionButtonMenuItem(
                        onClick = {
                            fabMenuExpanded = false
                            onCreateTerritory()
                        },
                        text = { Text("Novo território") },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) }
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
            PillSearchField(
                query = uiState.filter.query,
                onQueryChange = viewModel::updateQuery,
                placeholder = "Buscar código, nome, rua ou dirigente",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(12.dp))
            TerritoryFilterChips(
                filter = uiState.filter,
                statusCounts = uiState.statusCounts,
                myCount = uiState.myCount,
                onAllClick = viewModel::clearStatus,
                onMineClick = viewModel::toggleOnlyMine,
                onStatusClick = viewModel::toggleStatus
            )

            when {
                uiState.isLoading -> FullScreenLoading()
                uiState.sections.isEmpty() -> EmptyTerritories(
                    hasFilters = uiState.filter.isActive,
                    onClearFilters = viewModel::clearFilters
                )
                else -> TerritorySections(
                    uiState = uiState,
                    onTerritoryClick = onNavigateToTerritory
                )
            }
        }
    }
}

private fun summary(uiState: TerritoryListUiState): String = when {
    uiState.isLoading -> "Carregando…"
    uiState.filter.isActive -> "${uiState.visibleCount} de ${uiState.totalCount} territórios"
    else -> {
        val inProgress = uiState.statusCounts[TerritoryStatus.IN_PROGRESS] ?: 0
        buildString {
            append("${uiState.totalCount} territórios")
            if (inProgress > 0) append(" · $inProgress em andamento")
        }
    }
}

@Composable
private fun TerritorySections(
    uiState: TerritoryListUiState,
    onTerritoryClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = BottomNavClearance + 72.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        uiState.sections.forEach { section ->
            item(key = "header-${section.status.name}", contentType = "header") {
                SectionHeader(
                    status = section.status,
                    count = section.territories.size,
                    modifier = Modifier.animateItem()
                )
            }
            itemsIndexed(
                items = section.territories,
                key = { _, territory -> territory.id },
                contentType = { _, _ -> "territory" }
            ) { index, territory ->
                TerritoryListItem(
                    territory = territory,
                    color = uiState.palette[territory.id] ?: statusColor(territory.status),
                    isMine = uiState.currentUserId != null && territory.currentResponsibleId == uiState.currentUserId,
                    position = index,
                    count = section.territories.size,
                    onClick = { onTerritoryClick(territory.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(status: TerritoryStatus, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(statusColor(status), CircleShape)
        )
        Text(
            status.label(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
            Text(
                "$count",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TerritoryListItem(
    territory: Territory,
    color: Color,
    isMine: Boolean,
    position: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerSpec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    val top by animateDpAsState(
        targetValue = if (isPressed) PressedCorner else if (position == 0) OuterCorner else InnerCorner,
        animationSpec = cornerSpec,
        label = "topCorner"
    )
    val bottom by animateDpAsState(
        targetValue = if (isPressed) PressedCorner else if (position == count - 1) OuterCorner else InnerCorner,
        animationSpec = cornerSpec,
        label = "bottomCorner"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
        color = if (isMine) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TerritoryShapeBadge(code = territory.code, color = color)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        territory.code,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (isMine) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                            Text(
                                "Seu",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    territory.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (territory.location.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            territory.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                ResponsibleLine(territory)
                if (territory.hasBlocks && territory.status in BlockProgressStatuses) {
                    Text(
                        "${territory.completedBlockCount}/${territory.blockPolygons.size} quadras",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!territory.hasLocation && !territory.hasBoundary && !territory.hasBlocks) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = "Sem localização no mapa",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun ResponsibleLine(territory: Territory) {
    val name = territory.currentResponsibleName
    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (name != null) {
            UserAvatarByName(name = name, size = 20.dp, userId = territory.currentResponsibleId)
            Text(
                name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                "Sem dirigente",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyTerritories(hasFilters: Boolean, onClearFilters: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .padding(bottom = BottomNavClearance),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie12Sided.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (hasFilters) Icons.Default.SearchOff else Icons.Default.Map,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            if (hasFilters) "Nenhum território encontrado" else "Nenhum território cadastrado",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (hasFilters) "Tente outra busca ou remova os filtros." else "Os territórios cadastrados aparecerão aqui.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (hasFilters) {
            Spacer(Modifier.height(20.dp))
            FilledTonalButton(onClick = onClearFilters) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Limpar filtros")
            }
        }
    }
}
