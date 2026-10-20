package com.mraphaelpy.terriflow.core.report

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

enum class ReportFormat(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    CSV("csv", "text/csv")
}

@Singleton
class ReportExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun export(table: ReportTable, format: ReportFormat, fileName: String): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "${slug(fileName)}.${format.extension}")
        when (format) {
            ReportFormat.CSV -> writeCsv(table, file)
            ReportFormat.PDF -> writePdf(table, file)
        }
        file
    }

    fun shareIntent(file: File, format: ReportFormat): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Compartilhar relatório").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun writeCsv(table: ReportTable, file: File) {
        fun escape(value: String) = "\"" + value.replace("\"", "\"\"") + "\""
        val hasGroups = table.rows.any { it is ReportRow.Group }
        val header = (if (hasGroups) listOf("Grupo") else emptyList()) + table.columns.map { it.title }
        file.bufferedWriter(Charsets.UTF_8).use { out ->
            out.write("\uFEFF")
            out.write(header.joinToString(";") { escape(it) })
            out.newLine()
            var group = ""
            table.rows.forEach { row ->
                when (row) {
                    is ReportRow.Group -> group = row.title
                    is ReportRow.Cells -> {
                        val values = if (hasGroups) listOf(group) + row.values else row.values
                        out.write(values.joinToString(";") { escape(it) })
                        out.newLine()
                    }
                }
            }
        }
    }

    private fun writePdf(table: ReportTable, file: File) {
        val document = PdfDocument()
        val layout = PdfLayout(table)
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun startPage() {
            page?.let { document.finishPage(it) }
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            y = MARGIN
            val canvas = page!!.canvas
            if (pageNumber == 1) {
                canvas.drawText(table.title, MARGIN, y + 16f, layout.titlePaint)
                y += 26f
                canvas.drawText(table.subtitle, MARGIN, y + 10f, layout.subtitlePaint)
                y += 24f
            }
            layout.drawHeader(canvas, y)
            y += ROW_HEIGHT + 4f
            canvas.drawText("Página $pageNumber", PAGE_WIDTH - MARGIN - 50f, PAGE_HEIGHT - 20f, layout.subtitlePaint)
        }

        startPage()
        table.rows.forEach { row ->
            if (y + ROW_HEIGHT * 2 > PAGE_HEIGHT - MARGIN) startPage()
            val canvas = page!!.canvas
            when (row) {
                is ReportRow.Group -> {
                    y += 6f
                    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, layout.groupBackground)
                    canvas.drawText(layout.fit(row.title, layout.boldPaint, PAGE_WIDTH - 2 * MARGIN - 8f), MARGIN + 4f, y + 13f, layout.boldPaint)
                }
                is ReportRow.Cells -> layout.drawCells(canvas, row.values, y)
            }
            y += ROW_HEIGHT
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, layout.linePaint)
        }
        page?.let { document.finishPage(it) }
        file.outputStream().use { document.writeTo(it) }
        document.close()
    }

    private class PdfLayout(private val table: ReportTable) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 16f; typeface = Typeface.DEFAULT_BOLD }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; color = Color.DKGRAY }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9.5f; typeface = Typeface.DEFAULT_BOLD }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }
        val groupBackground = Paint().apply { color = Color.rgb(235, 238, 242) }
        private val headerBackground = Paint().apply { color = Color.rgb(220, 226, 235) }
        private val columnWidths: List<Float> = run {
            val total = table.columns.sumOf { it.weight.toDouble() }.toFloat()
            table.columns.map { (PAGE_WIDTH - 2 * MARGIN) * it.weight / total }
        }

        fun drawHeader(canvas: android.graphics.Canvas, y: Float) {
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, headerBackground)
            drawRow(canvas, table.columns.map { it.title }, y, boldPaint)
        }

        fun drawCells(canvas: android.graphics.Canvas, values: List<String>, y: Float) = drawRow(canvas, values, y, textPaint)

        private fun drawRow(canvas: android.graphics.Canvas, values: List<String>, y: Float, paint: Paint) {
            var x = MARGIN
            values.forEachIndexed { index, value ->
                val width = columnWidths.getOrElse(index) { 0f }
                canvas.drawText(fit(value, paint, width - 6f), x + 3f, y + 13f, paint)
                x += width
            }
        }

        fun fit(text: String, paint: Paint, maxWidth: Float): String {
            if (paint.measureText(text) <= maxWidth) return text
            var end = text.length
            while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
            return text.substring(0, end) + "…"
        }
    }

    private fun slug(name: String): String =
        Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .replace(Regex("[^A-Za-z0-9_-]+"), "-")
            .trim('-')
            .ifEmpty { "relatorio" }

    private companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 32f
        const val ROW_HEIGHT = 18f
    }
}
