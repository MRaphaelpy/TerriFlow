package com.mraphaelpy.terriflow.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.mraphaelpy.terriflow.ui.theme.TerriBlue
import com.mraphaelpy.terriflow.ui.theme.TerriGreen
import com.mraphaelpy.terriflow.ui.theme.TerriOrange
import com.mraphaelpy.terriflow.ui.theme.TerriPurple
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.domain.repository.AppColor
import com.mraphaelpy.terriflow.domain.repository.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
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
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tema do Aplicativo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        ThemeOptionRow(
                            label = "Seguir sistema",
                            selected = uiState.themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.updateThemeMode(ThemeMode.SYSTEM) }
                        )
                        ThemeOptionRow(
                            label = "Modo Claro",
                            selected = uiState.themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.updateThemeMode(ThemeMode.LIGHT) }
                        )
                        ThemeOptionRow(
                            label = "Modo Escuro",
                            selected = uiState.themeMode == ThemeMode.DARK,
                            onClick = { viewModel.updateThemeMode(ThemeMode.DARK) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Cores",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ColorOption(
                            color = TerriGreen,
                            selected = uiState.appColor == AppColor.DEFAULT,
                            onClick = { viewModel.updateAppColor(AppColor.DEFAULT) }
                        )
                        ColorOption(
                            color = TerriBlue,
                            selected = uiState.appColor == AppColor.BLUE,
                            onClick = { viewModel.updateAppColor(AppColor.BLUE) }
                        )
                        ColorOption(
                            color = TerriPurple,
                            selected = uiState.appColor == AppColor.PURPLE,
                            onClick = { viewModel.updateAppColor(AppColor.PURPLE) }
                        )
                        ColorOption(
                            color = TerriOrange,
                            selected = uiState.appColor == AppColor.ORANGE,
                            onClick = { viewModel.updateAppColor(AppColor.ORANGE) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ColorOption(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selecionado",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
