package com.mraphaelpy.terriflow.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mraphaelpy.terriflow.domain.model.TerritoryCode

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TerritoryShapeBadge(code: String, color: Color, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val label = TerritoryCode.numberOf(code)?.toString() ?: code.trim().take(5)
    val style = when {
        size >= 64.dp -> if (label.length <= 2) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge
        size >= 48.dp -> if (label.length <= 2) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall
        else -> if (label.length <= 2) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelSmall
    }
    Box(
        modifier = modifier
            .size(size)
            .background(color, MaterialShapes.Cookie9Sided.toShape()),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = style, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
    }
}
