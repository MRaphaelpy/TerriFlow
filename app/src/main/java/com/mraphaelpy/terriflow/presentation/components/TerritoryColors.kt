package com.mraphaelpy.terriflow.presentation.components

import androidx.compose.ui.graphics.Color
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryCode
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus

fun statusColor(status: TerritoryStatus): Color = when (status) {
    TerritoryStatus.AVAILABLE   -> Color(0xFF4CAF50)
    TerritoryStatus.ASSIGNED    -> Color(0xFF2196F3)
    TerritoryStatus.IN_PROGRESS -> Color(0xFFFF9800)
    TerritoryStatus.PAUSED      -> Color(0xFF9E9E9E)
    TerritoryStatus.COMPLETED   -> Color(0xFF8BC34A)
    TerritoryStatus.RETURNED    -> Color(0xFFF44336)
    TerritoryStatus.ARCHIVED    -> Color(0xFF607D8B)
}

fun territoryPalette(territories: List<Territory>): Map<String, Color> =
    territories
        .sortedWith(compareBy({ TerritoryCode.numberOf(it.code) ?: Int.MAX_VALUE }, { it.code }, { it.id }))
        .withIndex()
        .associate { (index, territory) ->
            val hue = (index * GOLDEN_ANGLE_DEGREES) % 360f
            val lightness = if (index % 2 == 0) 0.42f else 0.5f
            territory.id to Color.hsl(hue, 0.7f, lightness)
        }

private const val GOLDEN_ANGLE_DEGREES = 137.508f
