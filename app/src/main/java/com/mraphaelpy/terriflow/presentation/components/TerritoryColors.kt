package com.mraphaelpy.terriflow.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.mraphaelpy.terriflow.ui.theme.LocalStatusColors
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryCode
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus

@Composable
@ReadOnlyComposable
fun statusColor(status: TerritoryStatus): Color = LocalStatusColors.current[status]

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
