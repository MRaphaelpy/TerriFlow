package com.mraphaelpy.terriflow.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable

data class SheetOption<T>(
    val value: T,
    val label: String,
    val supporting: String? = null,
    val leading: (@Composable () -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OptionSheet(
    title: String,
    options: List<SheetOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                items(options) { option ->
                    ListItem(
                        headlineContent = { Text(option.label) },
                        supportingContent = option.supporting?.let { { Text(it) } },
                        leadingContent = option.leading,
                        trailingContent = if (option.value == selected) {
                            { Icon(Icons.Default.Check, contentDescription = "Selecionado", tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(option.value)
                                onDismiss()
                            }
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
