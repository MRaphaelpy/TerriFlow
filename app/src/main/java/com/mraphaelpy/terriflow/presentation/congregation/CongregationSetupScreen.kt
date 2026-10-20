package com.mraphaelpy.terriflow.presentation.congregation

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.domain.model.Congregation
import com.mraphaelpy.terriflow.presentation.auth.AuthLayout
import com.mraphaelpy.terriflow.presentation.components.FormErrorText
import com.mraphaelpy.terriflow.presentation.components.LoadingButton

private enum class SetupMode(val label: String) { JOIN("Entrar"), CREATE("Criar") }

@Composable
fun CongregationSetupScreen(
    onSetupComplete: () -> Unit,
    onLogout: () -> Unit,
    viewModel: CongregationSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(SetupMode.JOIN) }
    var congregationName by rememberSaveable { mutableStateOf("") }
    var joinCode by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete && uiState.congregation == null) onSetupComplete()
    }

    val created = uiState.congregation
    AuthLayout(
        icon = Icons.Default.Groups,
        title = if (created != null) "Congregação criada!" else "Sua congregação",
        subtitle = if (created != null) "Envie o código aos irmãos para que eles entrem" else "Entre com o código de convite ou crie uma nova",
        footer = {
            if (created == null) {
                TextButton(onClick = { viewModel.logout(onLogout) }) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Usar outra conta")
                }
            }
        }
    ) {
        if (created != null) {
            CreatedContent(created, onContinue = onSetupComplete)
        } else {
            ModeSelector(mode) {
                mode = it
                viewModel.clearError()
            }
            when (mode) {
                SetupMode.JOIN -> {
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { joinCode = it.uppercase().trim() },
                        label = { Text("Código de convite") },
                        placeholder = { Text("Ex.: CENTRO-1234") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        supportingText = { Text("Peça o código ao administrador da congregação") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    )
                    uiState.error?.let { FormErrorText(message = it) }
                    LoadingButton(text = "Entrar na congregação", isLoading = uiState.isLoading, onClick = { viewModel.joinByCode(joinCode) })
                }
                SetupMode.CREATE -> {
                    OutlinedTextField(
                        value = congregationName,
                        onValueChange = { congregationName = it },
                        label = { Text("Nome da congregação") },
                        leadingIcon = { Icon(Icons.Default.AddHome, contentDescription = null) },
                        supportingText = { Text("Você será o administrador principal e receberá um código de convite") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    )
                    uiState.error?.let { FormErrorText(message = it) }
                    LoadingButton(text = "Criar congregação", isLoading = uiState.isLoading, onClick = { viewModel.create(congregationName) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModeSelector(selected: SetupMode, onSelect: (SetupMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween), modifier = Modifier.fillMaxWidth()) {
        SetupMode.entries.forEachIndexed { index, option ->
            ToggleButton(
                checked = selected == option,
                onCheckedChange = { onSelect(option) },
                shapes = if (index == 0) ButtonGroupDefaults.connectedLeadingButtonShapes() else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (option == SetupMode.JOIN) Icons.AutoMirrored.Filled.Login else Icons.Default.AddHome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(option.label)
            }
        }
    }
}

@Composable
private fun CreatedContent(congregation: Congregation, onContinue: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Text(congregation.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Código de convite", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(congregation.code, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            FilledTonalIconButton(onClick = { clipboard.setText(AnnotatedString(congregation.code)) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar código")
            }
            FilledTonalIconButton(onClick = {
                val text = "Entre na congregação ${congregation.name} no TerriFlow usando o código: ${congregation.code}"
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Compartilhar código"))
            }) {
                Icon(Icons.Default.Share, contentDescription = "Compartilhar código")
            }
        }
    }
    Text(
        "O código também fica disponível depois em Configurações.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
        Text("Continuar")
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}
