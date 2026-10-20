package com.mraphaelpy.terriflow.presentation.territorydetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mraphaelpy.terriflow.core.util.DateFormats
import com.mraphaelpy.terriflow.domain.model.TerritoryNote
import com.mraphaelpy.terriflow.domain.model.TerritoryNoteKind

@Composable
internal fun TerritoryNotesSection(
    notes: List<TerritoryNote>,
    canDelete: (TerritoryNote) -> Boolean,
    onAdd: (TerritoryNoteKind, String, String) -> Unit,
    onDelete: (TerritoryNote) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<TerritoryNote?>(null) }

    if (showAddDialog) {
        AddNoteDialog(
            onSave = { kind, address, text ->
                onAdd(kind, address, text)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
    pendingDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remover anotação?") },
            text = { Text(note.address.ifBlank { note.text }) },
            confirmButton = {
                Button(onClick = {
                    onDelete(note)
                    pendingDelete = null
                }) { Text("Remover") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text("Anotações", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Visível só para administradores e o dirigente atual",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            FilledTonalButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Adicionar")
            }
        }

        val sorted = notes.sortedBy { it.kind != TerritoryNoteKind.DO_NOT_CALL }
        if (sorted.isEmpty()) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Nenhuma anotação. Registre endereços que pediram para não ser visitados ou dicas para o próximo dirigente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }
        sorted.forEachIndexed { index, note ->
            val top = if (index == 0) 24.dp else 6.dp
            val bottom = if (index == sorted.lastIndex) 24.dp else 6.dp
            NoteRow(
                note = note,
                shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
                onDelete = if (canDelete(note)) ({ pendingDelete = note }) else null
            )
        }
    }
}

@Composable
private fun NoteRow(note: TerritoryNote, shape: RoundedCornerShape, onDelete: (() -> Unit)?) {
    val isDoNotCall = note.kind == TerritoryNoteKind.DO_NOT_CALL
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = if (isDoNotCall) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(
                if (isDoNotCall) Icons.Default.Block else Icons.AutoMirrored.Filled.Notes,
                contentDescription = null,
                tint = if (isDoNotCall) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (isDoNotCall) "Não visitar" else "Observação",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDoNotCall) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary
                )
                if (note.address.isNotBlank()) {
                    Text(note.address, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                if (note.text.isNotBlank()) {
                    Text(note.text, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "${note.authorName} · ${DateFormats.fullDate().format(note.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remover anotação")
                }
            }
        }
    }
}

@Composable
private fun AddNoteDialog(onSave: (TerritoryNoteKind, String, String) -> Unit, onDismiss: () -> Unit) {
    var kind by remember { mutableStateOf(TerritoryNoteKind.DO_NOT_CALL) }
    var address by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    val isDoNotCall = kind == TerritoryNoteKind.DO_NOT_CALL
    val canSave = if (isDoNotCall) address.isNotBlank() else text.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova anotação") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TerritoryNoteKind.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = kind == option,
                            onClick = { kind = option },
                            shape = SegmentedButtonDefaults.itemShape(index, TerritoryNoteKind.entries.size)
                        ) {
                            Text(if (option == TerritoryNoteKind.DO_NOT_CALL) "Não visitar" else "Observação")
                        }
                    }
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(if (isDoNotCall) "Endereço" else "Endereço (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(if (isDoNotCall) "Detalhes (opcional)" else "Anotação") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = { onSave(kind, address, text) }, enabled = canSave) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
