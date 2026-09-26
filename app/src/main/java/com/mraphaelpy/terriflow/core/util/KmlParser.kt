package com.mraphaelpy.terriflow.core.util

import android.util.Xml
import com.mraphaelpy.terriflow.domain.model.LatLng
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object KmlParser {

    data class ParsedTerritory(
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

    fun parse(inputStream: InputStream, isKmz: Boolean): List<ParsedTerritory> {
        val kmlStream = if (isKmz) {
            extractKmlFromKmz(inputStream) ?: return emptyList()
        } else {
            inputStream
        }
        return parseKml(kmlStream)
    }

    private fun extractKmlFromKmz(inputStream: InputStream): InputStream? {
        val zip = ZipInputStream(BufferedInputStream(inputStream))
        var entry = zip.nextEntry
        while (entry != null) {
            if (entry.name.endsWith(".kml", ignoreCase = true)) {
                return zip.readBytes().inputStream()
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
        return null
    }

    private fun parseKml(inputStream: InputStream): List<ParsedTerritory> {
        val results = mutableListOf<ParsedTerritory>()

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        var folderDepth = 0
        var folderName = ""
        var folderDesc = ""
        var folderAllPoints = mutableListOf<LatLng>()
        var folderBlockPolygons = mutableListOf<List<LatLng>>()
        var inFolderName = false
        var inFolderDesc = false

        var inPlacemark = false
        var inPlacemarkName = false
        var inPlacemarkDesc = false
        var inOuterBoundary = false
        var inPointTag = false
        var inBoundaryCoords = false
        var inPointCoords = false

        var currentName = ""
        var currentDesc = ""
        var currentBoundary = mutableListOf<LatLng>()
        var currentPoint: LatLng? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "Folder" -> {
                        folderDepth++
                        if (folderDepth == 1) {
                            folderName = ""
                            folderDesc = ""
                            folderAllPoints = mutableListOf()
                            folderBlockPolygons = mutableListOf()
                        }
                    }
                    "Placemark" -> {
                        inPlacemark = true
                        currentName = ""
                        currentDesc = ""
                        currentBoundary = mutableListOf()
                        currentPoint = null
                    }
                    "name" -> when {
                        inPlacemark -> inPlacemarkName = true
                        folderDepth == 1 -> inFolderName = true  // top-level folder name only
                    }
                    "description" -> when {
                        inPlacemark -> inPlacemarkDesc = true
                        folderDepth == 1 -> inFolderDesc = true
                    }
                    "outerBoundaryIs" -> inOuterBoundary = true
                    "Point" -> if (inPlacemark) inPointTag = true
                    "coordinates" -> when {
                        inOuterBoundary -> inBoundaryCoords = true
                        inPointTag -> inPointCoords = true
                    }
                }

                XmlPullParser.END_TAG -> when (parser.name) {
                    "Folder" -> {
                        if (folderDepth == 1 && folderAllPoints.size >= 3) {
                            results += ParsedTerritory(
                                name = folderName.trim().ifEmpty { "Território" },
                                description = folderDesc.trim(),
                                boundaryPoints = convexHull(folderAllPoints),
                                centerPoint = null,
                                blockPolygons = folderBlockPolygons.toList()
                            )
                        }
                        folderDepth = (folderDepth - 1).coerceAtLeast(0)
                    }
                    "Placemark" -> {
                        if (folderDepth > 0) {
                            folderAllPoints.addAll(currentBoundary)
                            if (currentBoundary.size >= 3) {
                                folderBlockPolygons.add(currentBoundary.toList())
                            }
                        } else {
                            if (currentBoundary.size >= 3 || currentPoint != null) {
                                results += ParsedTerritory(
                                    name = currentName.trim(),
                                    description = currentDesc.trim(),
                                    boundaryPoints = currentBoundary.toList(),
                                    centerPoint = currentPoint
                                )
                            }
                        }
                        inPlacemark = false
                    }
                    "name" -> {
                        inPlacemarkName = false
                        inFolderName = false
                    }
                    "description" -> {
                        inPlacemarkDesc = false
                        inFolderDesc = false
                    }
                    "outerBoundaryIs" -> inOuterBoundary = false
                    "Point" -> inPointTag = false
                    "coordinates" -> {
                        inBoundaryCoords = false
                        inPointCoords = false
                    }
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text ?: ""
                    when {
                        inPlacemarkName -> currentName += text
                        inPlacemarkDesc -> currentDesc += text
                        inFolderName -> folderName += text
                        inFolderDesc -> folderDesc += text
                        inBoundaryCoords -> currentBoundary = parseCoordinateString(text).toMutableList()
                        inPointCoords -> currentPoint = parseCoordinateString(text).firstOrNull()
                    }
                }
            }
            event = parser.next()
        }
        return results
    }

    private fun parseCoordinateString(text: String): List<LatLng> =
        text.trim().split(Regex("\\s+")).mapNotNull { coord ->
            val parts = coord.split(",")
            if (parts.size >= 2) {
                runCatching {
                    LatLng(lat = parts[1].toDouble(), lng = parts[0].toDouble())
                }.getOrNull()
            } else null
        }

    private fun convexHull(points: List<LatLng>): List<LatLng> {
        val pts = points.distinctBy { "${it.lat},${it.lng}" }
        if (pts.size < 3) return pts

        val pivot = pts.minWith(compareBy({ it.lat }, { it.lng }))
        val sorted = pts.filter { it != pivot }.sortedWith { a, b ->
            val c = cross(pivot, a, b)
            when {
                c > 0.0 -> -1
                c < 0.0 -> 1
                else -> dist2(pivot, a).compareTo(dist2(pivot, b))
            }
        }

        val hull = mutableListOf(pivot)
        for (p in sorted) {
            while (hull.size > 1 && cross(hull[hull.size - 2], hull.last(), p) <= 0.0) {
                hull.removeAt(hull.size - 1)
            }
            hull.add(p)
        }
        return hull
    }

    private fun cross(o: LatLng, a: LatLng, b: LatLng): Double =
        (a.lng - o.lng) * (b.lat - o.lat) - (a.lat - o.lat) * (b.lng - o.lng)

    private fun dist2(a: LatLng, b: LatLng): Double {
        val dLat = a.lat - b.lat
        val dLng = a.lng - b.lng
        return dLat * dLat + dLng * dLng
    }
}
