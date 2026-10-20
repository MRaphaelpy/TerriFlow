package com.mraphaelpy.terriflow.presentation.territories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TerritoryFormState(
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val notes: String = ""
)

@Composable
fun TerritoryFormFields(
    state: TerritoryFormState,
    onChange: (TerritoryFormState) -> Unit,
    code: String?,
    nameError: Boolean = false
) {
    FormSection("Identificação") {
        code?.let {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Tag, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column {
                    Text("Número do território", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        FormField(
            value = state.name,
            onValueChange = { onChange(state.copy(name = it)) },
            label = "Nome *",
            icon = Icons.AutoMirrored.Filled.Label,
            isError = nameError && state.name.isBlank(),
            supporting = if (nameError && state.name.isBlank()) "Informe o nome do território" else null
        )
    }
    FormSection("Detalhes") {
        FormField(
            value = state.location,
            onValueChange = { onChange(state.copy(location = it)) },
            label = "Bairro / Localização",
            icon = Icons.Default.LocationCity
        )
        FormField(
            value = state.description,
            onValueChange = { onChange(state.copy(description = it)) },
            label = "Descrição",
            icon = Icons.Default.Description,
            singleLine = false
        )
        FormField(
            value = state.notes,
            onValueChange = { onChange(state.copy(notes = it)) },
            label = "Observações",
            icon = Icons.Default.EditNote,
            singleLine = false
        )
    }
}

@Composable
fun FormSection(title: String, content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    placeholder: String? = null,
    supporting: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supporting?.let { { Text(it) } },
        leadingIcon = { Icon(icon, contentDescription = null) },
        isError = isError,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        maxLines = if (singleLine) 1 else 5,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    )
}

val TerritoryFormSaver = listSaver<TerritoryFormState, String>(
    save = { listOf(it.name, it.description, it.location, it.notes) },
    restore = { TerritoryFormState(it[0], it[1], it[2], it[3]) }
)
