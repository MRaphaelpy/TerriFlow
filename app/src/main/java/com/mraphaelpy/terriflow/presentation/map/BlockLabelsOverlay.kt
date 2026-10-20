package com.mraphaelpy.terriflow.presentation.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Typeface
import com.mraphaelpy.terriflow.domain.model.LatLng
import com.mraphaelpy.terriflow.domain.model.polygonCentroid
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Escreve o número de cada quadra (1, 2, 3...) como texto simples no centro do desenho.
 * Só desenha a partir de [minZoom] para não poluir o mapa quando há muitos territórios.
 */
class BlockLabelsOverlay(
    blocks: List<List<LatLng>>,
    density: Float,
    private val minZoom: Double = 0.0
) : Overlay() {

    private val centers: List<GeoPoint?> = blocks.map { block ->
        polygonCentroid(block)?.let { GeoPoint(it.lat, it.lng) }
    }
    private val point = Point()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(32, 33, 36)
        textSize = 14 * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val haloPaint = Paint(textPaint).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3 * density
        strokeJoin = Paint.Join.ROUND
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || mapView.zoomLevelDouble < minZoom) return
        val projection = mapView.projection
        val baseline = -(textPaint.descent() + textPaint.ascent()) / 2f
        centers.forEachIndexed { index, center ->
            center ?: return@forEachIndexed
            projection.toPixels(center, point)
            val label = "${index + 1}"
            canvas.drawText(label, point.x.toFloat(), point.y + baseline, haloPaint)
            canvas.drawText(label, point.x.toFloat(), point.y + baseline, textPaint)
        }
    }
}
