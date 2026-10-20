package com.mraphaelpy.terriflow.core.report

data class ReportColumn(val title: String, val weight: Float = 1f)

/** Linha de agrupamento (ex.: território no S-13) ou linha comum de dados. */
sealed interface ReportRow {
    data class Group(val title: String) : ReportRow
    data class Cells(val values: List<String>) : ReportRow
}

data class ReportTable(
    val title: String,
    val subtitle: String,
    val columns: List<ReportColumn>,
    val rows: List<ReportRow>
)
