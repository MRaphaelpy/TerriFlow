package com.mraphaelpy.terriflow.presentation.map

import java.util.WeakHashMap
import org.osmdroid.views.MapView
import android.graphics.ColorMatrixColorFilter
import android.graphics.ColorMatrix
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import com.mraphaelpy.terriflow.domain.model.TerritoryCode

internal class MarkerIcon(val drawable: Drawable, val anchorY: Float)

internal fun createTerritoryMarkerIcon(
    context: Context,
    color: Int,
    statusColor: Int,
    label: String,
    ownerLabel: String?,
    highlighted: Boolean
): MarkerIcon {
    val density = context.resources.displayMetrics.density
    fun dp(value: Float) = value * density

    val radius = dp(if (highlighted) 20f else 15f)
    val ring = dp(if (highlighted) 3f else 2.5f)
    val tail = dp(7f)
    val halo = if (highlighted) dp(6f) else 0f
    val pad = dp(4f) + halo

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        textSize = dp(
            when {
                label.length <= 2 -> if (highlighted) 16f else 13f
                label.length == 3 -> if (highlighted) 13f else 11f
                else -> 9f
            }
        )
    }
    val ownerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = 0xFF202124.toInt()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        textSize = dp(10.5f)
    }

    val ownerText = ownerLabel?.let { ellipsize(it, ownerPaint, dp(90f)) }
    val pillPaddingH = dp(6f)
    val pillHeight = ownerText?.let { ownerPaint.textSize + dp(6f) } ?: 0f
    val pillWidth = ownerText?.let { ownerPaint.measureText(it) + pillPaddingH * 2 } ?: 0f
    val pillGap = if (ownerText != null) dp(2f) else 0f

    val width = maxOf(radius * 2 + pad * 2, pillWidth + dp(4f) * 2)
    val tipY = pad + radius * 2 + tail
    val height = tipY + pillGap + pillHeight + dp(4f)

    val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = width / 2f
    val cy = pad + radius

    if (highlighted) {
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = withAlpha(color, 90) }
        canvas.drawCircle(cx, cy, radius + halo, haloPaint)
    }

    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        setShadowLayer(dp(3f), 0f, dp(1.5f), 0x55000000)
    }
    val pin = Path().apply {
        addCircle(cx, cy, radius, Path.Direction.CW)
        moveTo(cx - radius * 0.55f, cy + radius * 0.8f)
        lineTo(cx, tipY)
        lineTo(cx + radius * 0.55f, cy + radius * 0.8f)
        close()
    }
    canvas.drawPath(pin, bodyPaint)

    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = ring
    }
    canvas.drawCircle(cx, cy, radius - ring / 2f, ringPaint)

    canvas.drawText(label, cx, cy - (labelPaint.descent() + labelPaint.ascent()) / 2f, labelPaint)

    val dotRadius = dp(if (highlighted) 6f else 5f)
    val dotX = cx + radius * 0.72f
    val dotY = cy - radius * 0.72f
    canvas.drawCircle(dotX, dotY, dotRadius + dp(1.5f), Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = AndroidColor.WHITE })
    canvas.drawCircle(dotX, dotY, dotRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = statusColor })

    if (ownerText != null) {
        val top = tipY + pillGap
        val rect = RectF(cx - pillWidth / 2f, top, cx + pillWidth / 2f, top + pillHeight)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = AndroidColor.WHITE
            setShadowLayer(dp(2f), 0f, dp(1f), 0x44000000)
        }
        canvas.drawRoundRect(rect, pillHeight / 2f, pillHeight / 2f, pillPaint)
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = dp(1.2f)
        }
        canvas.drawRoundRect(rect, pillHeight / 2f, pillHeight / 2f, accent)
        canvas.drawText(ownerText, cx, rect.centerY() - (ownerPaint.descent() + ownerPaint.ascent()) / 2f, ownerPaint)
    }

    return MarkerIcon(bitmap.toDrawable(context.resources), anchorY = tipY / height)
}

internal fun shortPersonName(fullName: String): String {
    val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> parts.first()
        else -> "${parts.first()} ${parts.last().first().uppercaseChar()}."
    }
}

private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    var end = text.length
    while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
    return text.substring(0, end).trimEnd() + "…"
}

private fun withAlpha(color: Int, alpha: Int): Int =
    AndroidColor.argb(alpha, AndroidColor.red(color), AndroidColor.green(color), AndroidColor.blue(color))

internal fun markerLabel(code: String): String =
    TerritoryCode.numberOf(code)?.toString() ?: code.trim().take(MAX_CUSTOM_LABEL_LENGTH)

private const val MAX_CUSTOM_LABEL_LENGTH = 5

fun openDirections(context: Context, lat: Double, lng: Double) {
    val uri = "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng".toUri()
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}

@SuppressLint("QueryPermissionsNeeded")
fun openInGoogleMaps(context: Context, lat: Double?, lng: Double?, label: String = "") {
    if (lat == null || lng == null) return
    val encodedLabel = Uri.encode(label)
    val uri = "geo:$lat,$lng?q=$lat,$lng($encodedLabel)".toUri()
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        val webUri = "https://maps.google.com/?q=$lat,$lng".toUri()
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

private val DarkTilesFilter: ColorMatrixColorFilter by lazy {
    val invert = ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    val hueRotate = ColorMatrix(
        floatArrayOf(
            -0.574f, 1.43f, 0.144f, 0f, 0f,
            0.426f, 0.43f, 0.144f, 0f, 0f,
            0.426f, 1.43f, -0.856f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    val dim = ColorMatrix().apply { setScale(0.85f, 0.85f, 0.9f, 1f) }
    val desaturate = ColorMatrix().apply { setSaturation(0.75f) }
    ColorMatrixColorFilter(
        ColorMatrix().apply {
            postConcat(invert)
            postConcat(hueRotate)
            postConcat(desaturate)
            postConcat(dim)
        }
    )
}

private val darkTilesState = WeakHashMap<MapView, Boolean>()

fun MapView.applyDarkTiles(dark: Boolean) {
    if (darkTilesState[this] == dark) return
    darkTilesState[this] = dark
    overlayManager.tilesOverlay.setColorFilter(if (dark) DarkTilesFilter else null)
    invalidate()
}
