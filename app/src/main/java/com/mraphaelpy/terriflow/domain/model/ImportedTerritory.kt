package com.mraphaelpy.terriflow.domain.model

data class ImportedTerritory(
    val name: String,
    val description: String,
    val boundaryPoints: List<LatLng>,
    val centerPoint: LatLng?,
    val blockPolygons: List<List<LatLng>> = emptyList()
) {
    val centroid: LatLng?
        get() = centerPoint ?: if (boundaryPoints.size >= 3) {
            LatLng(
                lat = boundaryPoints.map { it.lat }.average(),
                lng = boundaryPoints.map { it.lng }.average()
            )
        } else null
}
