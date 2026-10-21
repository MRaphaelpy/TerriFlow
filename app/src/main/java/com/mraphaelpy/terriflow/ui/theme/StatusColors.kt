package com.mraphaelpy.terriflow.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.blend.Blend
import com.materialkolor.hct.Hct
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus

@Immutable
class StatusColors(private val colors: Map<TerritoryStatus, Color>) {
    operator fun get(status: TerritoryStatus): Color = colors.getValue(status)

    companion object {
        val Base = StatusColors(
            mapOf(
                TerritoryStatus.AVAILABLE to Color(0xFF4CAF50),
                TerritoryStatus.ASSIGNED to Color(0xFF2196F3),
                TerritoryStatus.IN_PROGRESS to Color(0xFFFF9800),
                TerritoryStatus.PAUSED to Color(0xFF9E9E9E),
                TerritoryStatus.COMPLETED to Color(0xFF8BC34A),
                TerritoryStatus.RETURNED to Color(0xFFF44336),
                TerritoryStatus.ARCHIVED to Color(0xFF607D8B)
            )
        )

        fun harmonizedWith(primary: Color, dark: Boolean): StatusColors = StatusColors(
            TerritoryStatus.entries.associateWith { status ->
                val harmonized = Hct.fromInt(Blend.harmonize(Base[status].toArgb(), primary.toArgb()))
                val tone = if (dark) DARK_TONE else LIGHT_TONE
                Color(harmonized.withTone(tone).toInt())
            }
        )

        private const val LIGHT_TONE = 55.0
        private const val DARK_TONE = 72.0
    }
}

val LocalStatusColors = staticCompositionLocalOf { StatusColors.Base }
