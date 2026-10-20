package com.mraphaelpy.terriflow.presentation.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.label

fun statusColor(status: TerritoryStatus): Color = when (status) {
    TerritoryStatus.AVAILABLE   -> Color(0xFF4CAF50)
    TerritoryStatus.ASSIGNED    -> Color(0xFF2196F3)
    TerritoryStatus.IN_PROGRESS -> Color(0xFFFF9800)
    TerritoryStatus.PAUSED      -> Color(0xFF9E9E9E)
    TerritoryStatus.COMPLETED   -> Color(0xFF8BC34A)
    TerritoryStatus.RETURNED    -> Color(0xFFF44336)
    TerritoryStatus.ARCHIVED    -> Color(0xFF607D8B)
}

private fun statusColorInt(status: TerritoryStatus): Int = statusColor(status).toArgb()

fun territoryColor(territoryId: String): Color {
    val h = territoryId.hashCode()
    val hue        = ((h and 0x7FFFFFFF) % 360).toFloat()
    val saturation = 0.55f + ((h ushr 8 and 0xFF) % 30) / 100f
    val lightness  = 0.36f + ((h ushr 16 and 0xFF) % 18) / 100f
    return Color.hsl(hue, saturation, lightness)
}

internal fun createStatusMarkerIcon(
    context: Context,
    color: Int,
    code: String,
    responsibleName: String? = null,
    scale: Float = 1.0f,
    showStar: Boolean = false
): android.graphics.drawable.Drawable {
    val baseW = 140
    val hasResponsible = !responsibleName.isNullOrEmpty()
    val baseH = if (hasResponsible) 175 else 140
    val w = (baseW * scale).toInt()
    val h = (baseH * scale).toInt()
    val bodyH = ((h - 35 * scale)).toFloat()

    val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val strokeWidth = 5f * scale
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
    }
    val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = 0xFFFFFFFF.toInt()
        textSize = 26f * scale
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = 0xFFFFFFFF.toInt()
        textSize = 18f * scale
        textAlign = Paint.Align.CENTER
    }

    if (showStar) {
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0xFFFFD700.toInt()
            style = Paint.Style.STROKE
            this.strokeWidth = 6f * scale
        }
        val rect2 = android.graphics.RectF(2f, 2f, w - 2f, bodyH + 2f)
        canvas.drawRoundRect(rect2, 22f, 22f, highlightPaint)
    }

    val rect = android.graphics.RectF(4f * scale, 4f * scale, w - 4f * scale, bodyH)
    canvas.drawRoundRect(rect, 20f * scale, 20f * scale, bodyPaint)
    canvas.drawRoundRect(rect, 20f * scale, 20f * scale, strokePaint)

    val ptrW = 14f * scale
    val triPath = android.graphics.Path().apply {
        moveTo(w / 2f - ptrW, bodyH)
        lineTo(w / 2f + ptrW, bodyH)
        lineTo(w / 2f, h.toFloat() - 4f * scale)
        close()
    }
    canvas.drawPath(triPath, bodyPaint)

    val shortCode = code.takeLast(3).trimStart('0').ifEmpty { "0" }
    if (hasResponsible) {
        canvas.drawText(shortCode, w / 2f, bodyH / 2f - 4f * scale, codePaint)
        canvas.drawLine(
            16f * scale, bodyH / 2f + 8f * scale,
            w - 16f * scale, bodyH / 2f + 8f * scale,
            strokePaint.apply { this.strokeWidth = 1f * scale }
        )
        val initials = responsibleName.split(" ")
            .take(2).joinToString(" ") { it.take(1).uppercase() + it.drop(1).take(4) }
        canvas.drawText(initials, w / 2f, bodyH - 10f * scale, namePaint)
    } else {
        canvas.drawText(shortCode, w / 2f, bodyH / 2f + codePaint.textSize / 3f, codePaint)
    }

    if (showStar) {
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = 0xFFFFD700.toInt() }
        canvas.drawCircle(w - 14f * scale, 14f * scale, 10f * scale, starPaint)
        val textPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0xFF333333.toInt()
            textSize = 14f * scale
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText("★", w - 14f * scale, 14f * scale + textPaint2.textSize / 3f, textPaint2)
    }

    return bitmap.toDrawable(context.resources)
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
