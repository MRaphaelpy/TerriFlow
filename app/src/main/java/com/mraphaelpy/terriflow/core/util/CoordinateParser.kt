package com.mraphaelpy.terriflow.core.util

import com.mraphaelpy.terriflow.domain.model.LatLng

/**
 * Aceita os formatos que o Google Maps copia para a área de transferência:
 *  - "-23.5505, -46.6333"          (vírgula separando)
 *  - "-23.5505 -46.6333"           (espaço separando)
 *  - "-23°33'01.8\"S 46°37'59.9\"W" (graus/minutos/segundos)
 *  - "https://maps.google.com/?q=-23.5505,-46.6333" (URL)
 */
object CoordinateParser {

    fun parse(input: String): LatLng? {
        val trimmed = input.trim()
        return tryUrl(trimmed)
            ?: tryDecimal(trimmed)
            ?: tryDMS(trimmed)
    }

    private fun tryUrl(input: String): LatLng? {
        if (!input.startsWith("http")) return null
        val qParam = Regex("[?&]q=([^&]+)").find(input)?.groupValues?.get(1) ?: return null
        return tryDecimal(qParam.replace("%2C", ","))
    }

    private fun tryDecimal(input: String): LatLng? {
        val cleaned = input
            .replace(Regex("[°'\"]"), " ")
            .replace(",", " ")
            .trim()
        val parts = cleaned.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.size < 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lng = parts[1].toDoubleOrNull() ?: return null
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) return null
        return LatLng(lat, lng)
    }

    private fun tryDMS(input: String): LatLng? {
        val regex = Regex("""(\d+)[°\s]+(\d+)['\s]+(\d+\.?\d*)["\s]*([NSns])\s+(\d+)[°\s]+(\d+)['\s]+(\d+\.?\d*)["\s]*([EWew])""")
        val match = regex.find(input) ?: return null
        val (d1, m1, s1, dir1, d2, m2, s2, dir2) = match.destructured
        var lat = d1.toDouble() + m1.toDouble() / 60 + s1.toDouble() / 3600
        var lng = d2.toDouble() + m2.toDouble() / 60 + s2.toDouble() / 3600
        if (dir1.uppercase() == "S") lat = -lat
        if (dir2.uppercase() == "W") lng = -lng
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) return null
        return LatLng(lat, lng)
    }
}
