package com.mraphaelpy.terriflow.data.local.converter

import androidx.room.TypeConverter
import com.mraphaelpy.terriflow.domain.model.LatLng
import java.util.Date

class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun toTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromStringMap(value: String?): Map<String, String> {
        if (value.isNullOrEmpty()) return emptyMap()
        return value.split("||").mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
    }

    @TypeConverter
    fun toStringMap(map: Map<String, String>?): String {
        return map?.entries?.joinToString("||") { "${it.key}=${it.value}" } ?: ""
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split("||")
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String {
        return list?.joinToString("||") ?: ""
    }

    @TypeConverter
    fun fromLatLngList(value: String?): List<LatLng> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(";").mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) {
                val lat = parts[0].toDoubleOrNull()
                val lng = parts[1].toDoubleOrNull()
                if (lat != null && lng != null) LatLng(lat, lng) else null
            } else null
        }
    }

    @TypeConverter
    fun toLatLngList(list: List<LatLng>?): String {
        return list?.joinToString(";") { "${it.lat},${it.lng}" } ?: ""
    }

    @TypeConverter
    fun fromBlockPolygons(value: String?): List<List<LatLng>> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split("|").map { polyStr ->
            polyStr.split(";").mapNotNull { pt ->
                val parts = pt.split(",")
                if (parts.size == 2) {
                    val lat = parts[0].toDoubleOrNull()
                    val lng = parts[1].toDoubleOrNull()
                    if (lat != null && lng != null) LatLng(lat, lng) else null
                } else null
            }
        }.filter { it.size >= 3 }
    }

    @TypeConverter
    fun toBlockPolygons(list: List<List<LatLng>>?): String {
        return list?.joinToString("|") { polygon ->
            polygon.joinToString(";") { "${it.lat},${it.lng}" }
        } ?: ""
    }
}
