package com.mraphaelpy.terriflow.presentation.map

import android.content.Context
import androidx.core.content.edit
import com.mraphaelpy.terriflow.domain.model.GeoBounds

internal class MapAreaStore(context: Context) {

    private val prefs = context.getSharedPreferences("terriflow_map", Context.MODE_PRIVATE)

    fun load(): GeoBounds? {
        if (!prefs.contains(NORTH)) return null
        return GeoBounds(
            north = prefs.getFloat(NORTH, 0f).toDouble(),
            east = prefs.getFloat(EAST, 0f).toDouble(),
            south = prefs.getFloat(SOUTH, 0f).toDouble(),
            west = prefs.getFloat(WEST, 0f).toDouble()
        )
    }

    fun save(bounds: GeoBounds) = prefs.edit {
        putFloat(NORTH, bounds.north.toFloat())
        putFloat(EAST, bounds.east.toFloat())
        putFloat(SOUTH, bounds.south.toFloat())
        putFloat(WEST, bounds.west.toFloat())
    }

    private companion object {
        const val NORTH = "area_north"
        const val EAST = "area_east"
        const val SOUTH = "area_south"
        const val WEST = "area_west"
    }
}
