package com.mraphaelpy.terriflow.presentation.territories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.core.util.CoordinateParser
import com.mraphaelpy.terriflow.presentation.components.FormErrorText
import com.mraphaelpy.terriflow.presentation.components.FullScreenLoading
import com.mraphaelpy.terriflow.presentation.components.LoadingButton
import com.mraphaelpy.terriflow.presentation.map.openInGoogleMaps

enum class MapMode { PIN, BOUNDARY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTerritoryScreen(
    onSaved: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EditTerritoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var coordInput by remember { mutableStateOf("") }
    var coordError by remember { mutableStateOf<String?>(null) }
    var mapExpanded by remember { mutableStateOf(false) }
    var mapMode by remember { mutableStateOf(MapMode.PIN) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && !initialized) {
            code = uiState.code
            name = uiState.name
            description = uiState.description
            location = uiState.location
            notes = uiState.notes
            initialized = true
        }
    }

    LaunchedEffect(uiState.success) { if (uiState.success) onSaved() }

    if (mapExpanded) {
        // Tela cheia de edição do mapa
        FullMapEditor(
            context = context,
            uiState = uiState,
            mapMode = mapMode,
            onPinSet = { lat, lng -> 
                viewModel.updatePin(lat, lng)
                location = String.format(java.util.Locale.US, "%.6f, %.6f", lat, lng)
            },
            onBoundaryPoint = { viewModel.addBoundaryPoint(it) },
            onUpdateBoundaryPoint = { idx, pt -> viewModel.updateBoundaryPoint(idx, pt) },
            onUndoBoundary = { viewModel.removeLastBoundaryPoint() },
            onClearBoundary = { viewModel.clearBoundary() },
            onClose = { mapExpanded = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Editar território")
                        if (uiState.code.isNotEmpty()) {
                            Text(uiState.code,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            FullScreenLoading(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Campos de texto
            OutlinedTextField(value = code, onValueChange = { code = it },
                label = { Text("Número/Código") }, singleLine = true,
                placeholder = { Text("Ex: 14") },
                modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Nome *") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = description, onValueChange = { description = it },
                label = { Text("Descrição") }, minLines = 2, maxLines = 4,
                modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = location, onValueChange = { location = it },
                label = { Text("Bairro / Localização") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = notes, onValueChange = { notes = it },
                label = { Text("Observações") }, minLines = 2, maxLines = 4,
                modifier = Modifier.fillMaxWidth())

            HorizontalDivider()

            // Seção de localização no mapa
            Text("Localização no mapa", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)

            // Campo de colar coordenadas do Google Maps
            OutlinedTextField(
                value = coordInput,
                onValueChange = {
                    coordInput = it
                    coordError = null
                },
                label = { Text("Colar coordenadas do Google Maps") },
                placeholder = { Text("-23.5505, -46.6333") },
                supportingText = {
                    Text(
                        coordError ?: "Cole lat,lng copiado do Google Maps (toque longo no mapa → copiar coordenadas)",
                        color = if (coordError != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                isError = coordError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                trailingIcon = {
                    if (coordInput.isNotEmpty()) {
                        IconButton(onClick = {
                            val parsed = CoordinateParser.parse(coordInput)
                            if (parsed != null) {
                                viewModel.updatePin(parsed.lat, parsed.lng)
                                coordInput = "%.6f, %.6f".format(parsed.lat, parsed.lng)
                                coordError = null
                            } else {
                                coordError = "Formato inválido. Use: -23.5505, -46.6333"
                            }
                        }) {
                            Icon(Icons.Default.MyLocation, "Usar coordenadas")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Status da localização atual
            if (uiState.latitude != null && uiState.longitude != null) {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.LocationOn, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp))
                                Text("Local de navegação definido", style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium)
                            }
                            Text("%.6f, %.6f".format(uiState.latitude, uiState.longitude),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (uiState.hasBoundary) {
                                Text("${uiState.boundaryPoints.size} pontos de contorno",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Row {
                            // Abrir no Google Maps
                            IconButton(onClick = {
                                openInGoogleMaps(context, uiState.latitude, uiState.longitude)
                            }) {
                                Icon(Icons.Default.OpenInNew, "Abrir no Google Maps",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            // Limpar pino
                            IconButton(onClick = { viewModel.clearPin() }) {
                                Icon(Icons.Default.LocationOff, "Remover localização",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Botões de ação do mapa
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { mapExpanded = true; mapMode = MapMode.PIN },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AddLocation, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Navegação")
                }
                OutlinedButton(
                    onClick = { mapExpanded = true; mapMode = MapMode.BOUNDARY },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Hexagon, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Contorno")
                }
            }

            // Mini-mapa preview (somente leitura)
            val previewLat = uiState.latitude
            val previewLng = uiState.longitude
            if (previewLat != null && previewLng != null) {
                Text("Pré-visualização", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    PreviewMap(
                        context = context,
                        lat = previewLat,
                        lng = previewLng,
                        boundaryPoints = uiState.boundaryPoints,
                        onExpand = { mapExpanded = true }
                    )
                }
            }

            uiState.error?.let { FormErrorText(message = it) }

            Spacer(Modifier.height(8.dp))

            LoadingButton(
                text = "Salvar alterações",
                isLoading = uiState.isSaving,
                onClick = { viewModel.save(code, name, description, location, notes) }
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

val EditTerritoryUiState.hasBoundary: Boolean get() = boundaryPoints.size >= 3
