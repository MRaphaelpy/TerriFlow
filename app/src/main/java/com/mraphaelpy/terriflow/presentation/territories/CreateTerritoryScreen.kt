package com.mraphaelpy.terriflow.presentation.territories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.presentation.components.FormErrorText
import com.mraphaelpy.terriflow.presentation.components.LoadingButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTerritoryScreen(
    onCreated: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CreateTerritoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(uiState.success) {
        if (uiState.success && uiState.createdTerritoryId != null) {
            onCreated(uiState.createdTerritoryId!!)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novo território") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Número/Código") },
                        placeholder = { Text("Ex: 14 (Deixe vazio p/ auto-gerar)") },
                        leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome *") },
                        leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição") },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                        minLines = 3,
                        maxLines = 5,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Localização / Bairro") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Observações") },
                        leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                        minLines = 2,
                        maxLines = 4,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            uiState.error?.let {
                FormErrorText(message = it, modifier = Modifier.padding(horizontal = 8.dp))
            }

            Spacer(Modifier.height(16.dp))

            LoadingButton(
                text = "Criar território",
                isLoading = uiState.isLoading,
                onClick = { viewModel.createTerritory(code, name, description, location, notes) },
                shape = MaterialTheme.shapes.large,
                leadingIcon = Icons.Default.CheckCircle,
                height = 56.dp
            )
        }
    }
}
