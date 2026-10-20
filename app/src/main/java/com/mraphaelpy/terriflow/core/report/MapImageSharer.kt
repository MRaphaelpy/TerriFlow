package com.mraphaelpy.terriflow.core.report

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import androidx.core.content.FileProvider
import java.io.File

/** Gera uma imagem PNG de um mapa já desenhado na tela, com uma faixa de título, e a compartilha. */
object MapImageSharer {

    fun share(context: Context, mapView: View, title: String, subtitle: String) {
        if (mapView.width == 0 || mapView.height == 0) return
        val density = context.resources.displayMetrics.density
        val footerHeight = (64 * density).toInt()

        val bitmap = Bitmap.createBitmap(mapView.width, mapView.height + footerHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        mapView.draw(canvas)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(32, 33, 36)
            textSize = 18 * density
            typeface = Typeface.DEFAULT_BOLD
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 13 * density
        }
        val left = 16 * density
        canvas.drawText(title, left, mapView.height + 28 * density, titlePaint)
        canvas.drawText(subtitle, left, mapView.height + 50 * density, subtitlePaint)

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "mapa-territorio.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, title)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Compartilhar mapa"))
    }
}
