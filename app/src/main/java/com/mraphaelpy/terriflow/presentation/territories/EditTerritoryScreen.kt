package com.mraphaelpy.terriflow.presentation.territories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.core.util.CoordinateParser
import com.mraphaelpy.terriflow.domain.model.polygonCentroid
import com.mraphaelpy.terriflow.presentation.components.FormErrorText
import com.mraphaelpy.terriflow.presentation.components.FullScreenLoading
import com.mraphaelpy.terriflow.presentation.components.LoadingButton
import com.mraphaelpy.terriflow.presentation.map.openInGoogleMaps
import java.util.Locale

enum class MapMode { PIN, BOUNDARY }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditTerritoryScreen(
    onSaved: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EditTerritoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var form by rememberSaveable(stateSaver = TerritoryFormSaver) { mutableStateOf(TerritoryFormState()) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var mapEditorMode by remember { mutableStateOf<MapMode?>(null) }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && !initialized) {
            form = TerritoryFormState(uiState.name, uiState.description, uiState.location, uiState.notes)
            initialized = true
        }
    }
    LaunchedEffect(uiState.success) { if (uiState.success) onSaved() }

    mapEditorMode?.let { mode ->
        FullMapEditor(
            context = context,
            uiState = uiState,
            mapMode = mode,
            onPinSet = { lat, lng -> viewModel.updatePin(lat, lng) },
            onBoundaryPoint = viewModel::addBoundaryPoint,
            onUpdateBoundaryPoint = viewModel::updateBoundaryPoint,
            onUndoBoundary = viewModel::removeLastBoundaryPoint,
            onClearBoundary = viewModel::clearBoundary,
            onClose = { mapEditorMode = null }
        )
        return
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Editar território") },
                subtitle = { if (uiState.code.isNotEmpty()) Text(uiState.code) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            if (!uiState.isLoading) {
                LoadingButton(
                    text = "Salvar alterações",
                    isLoading = uiState.isSaving,
                    onClick = { viewModel.save(form.name, form.description, form.location, form.notes) },
                    leadingIcon = Icons.Default.CheckCircle,
                    height = 56.dp,
                    modifier = Modifier.navigationBarsPadding().padding(16.dp)
                )
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            FullScreenLoading(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TerritoryFormFields(
                state = form,
                onChange = { form = it },
                code = uiState.code,
                nameError = form.name.isBlank()
            )
            MapSection(
                uiState = uiState,
                onEditPin = { mapEditorMode = MapMode.PIN },
                onEditBoundary = { mapEditorMode = MapMode.BOUNDARY },
                onClearPin = viewModel::clearPin,
                onPastePin = viewModel::updatePin,
                onOpenInMaps = { lat, lng -> openInGoogleMaps(context, lat, lng) }
            )
            uiState.error?.let { FormErrorText(message = it, modifier = Modifier.padding(horizontal = 8.dp)) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MapSection(
    uiState: EditTerritoryUiState,
    onEditPin: () -> Unit,
    onEditBoundary: () -> Unit,
    onClearPin: () -> Unit,
    onPastePin: (Double, Double) -> Unit,
    onOpenInMaps: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    var coordinates by rememberSaveable { mutableStateOf("") }
    var coordinatesError by rememberSaveable { mutableStateOf<String?>(null) }
    val hasPin = uiState.latitude != null && uiState.longitude != null
    val previewCenter = if (hasPin) {
        uiState.latitude!! to uiState.longitude!!
    } else {
        polygonCentroid(uiState.boundaryPoints)?.let { it.lat to it.lng }
    }

    fun applyCoordinates() {
        val parsed = CoordinateParser.parse(coordinates)
        if (parsed == null) {
            coordinatesError = "Formato inválido. Ex.: -23.5505, -46.6333"
        } else {
            onPastePin(parsed.lat, parsed.lng)
            coordinates = ""
            coordinatesError = null
        }
    }

    FormSection("Localização no mapa") {
        StatusLine(
            icon = if (hasPin) Icons.Default.LocationOn else Icons.Default.LocationOff,
            text = if (hasPin) {
                "Pino em " + String.format(Locale.US, "%.5f, %.5f", uiState.latitude, uiState.longitude)
            } else {
                "Sem pino de navegação"
            },
            active = hasPin
        )
        StatusLine(
            icon = Icons.Default.Hexagon,
            text = if (uiState.hasBoundary) "Contorno com ${uiState.boundaryPoints.size} pontos" else "Sem contorno desenhado",
            active = uiState.hasBoundary
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(onClick = onEditPin, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (hasPin) "Mover pino" else "Marcar pino")
            }
            FilledTonalButton(onClick = onEditBoundary, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Hexagon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Contorno")
            }
        }

        OutlinedTextField(
            value = coordinates,
            onValueChange = {
                coordinates = it
                coordinatesError = null
            },
            label = { Text("Colar coordenadas") },
            placeholder = { Text("-23.5505, -46.6333") },
            supportingText = {
                Text(coordinatesError ?: "No Google Maps, toque e segure no local e copie as coordenadas")
            },
            isError = coordinatesError != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { applyCoordinates() }),
            trailingIcon = {
                if (coordinates.isNotBlank()) {
                    IconButton(onClick = ::applyCoordinates) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Usar coordenadas")
                    }
                }
            },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        )

        previewCenter?.let { (lat, lng) ->
            Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().height(200.dp)) {
                PreviewMap(
                    context = context,
                    lat = lat,
                    lng = lng,
                    boundaryPoints = uiState.boundaryPoints,
                    onExpand = if (hasPin) onEditPin else onEditBoundary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { onOpenInMaps(lat, lng) }) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Google Maps")
                }
                if (hasPin) {
                    FilledTonalButton(onClick = onClearPin) {
                        Icon(Icons.Default.LocationOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Remover pino")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(icon: ImageVector, text: String, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

val EditTerritoryUiState.hasBoundary: Boolean get() = boundaryPoints.size >= 3
