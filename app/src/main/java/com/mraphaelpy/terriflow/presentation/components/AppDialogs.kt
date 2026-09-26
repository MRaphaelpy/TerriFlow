package com.mraphaelpy.terriflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Dialog de confirmação genérico com botão de ação primária e cancelar.
 *
 * @param icon Ícone opcional exibido no topo do dialog.
 */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Confirmar",
    dismissLabel: String = "Cancelar",
    icon: ImageVector? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon?.let { {
            Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        } },
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        }
    )
}

/**
 * Dialog para ações destrutivas irreversíveis (apagar, remover).
 * Botão de confirmação usa a cor de erro.
 */
@Composable
fun DestructiveDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Apagar"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

/**
 * Dialog de carregamento sem botões. Não pode ser dispensado pelo usuário.
 */
@Composable
fun LoadingDialog(message: String) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Aguarde...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

/**
 * Dialog de erro com ícone de erro, título fixo "Erro" e botão OK.
 */
@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Erro", fontWeight = FontWeight.SemiBold) },
        text = { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text("OK")
            }
        }
    )
}
