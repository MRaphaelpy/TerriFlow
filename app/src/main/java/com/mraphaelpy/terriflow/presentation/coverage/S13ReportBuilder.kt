package com.mraphaelpy.terriflow.presentation.coverage

import com.mraphaelpy.terriflow.core.report.ReportColumn
import com.mraphaelpy.terriflow.core.report.ReportRow
import com.mraphaelpy.terriflow.core.report.ReportTable
import com.mraphaelpy.terriflow.core.util.DateFormats
import com.mraphaelpy.terriflow.domain.model.AssignmentPeriod
import com.mraphaelpy.terriflow.domain.model.ServiceYear
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryCode
import com.mraphaelpy.terriflow.domain.model.TerritoryStatistics

/** Registro de designação de território (modelo S-13) de um ano de serviço. */
object S13ReportBuilder {

    fun build(
        territories: List<Territory>,
        periodsByTerritory: Map<String, List<AssignmentPeriod>>,
        year: ServiceYear,
        congregationName: String?
    ): ReportTable {
        val dates = DateFormats.fullDate()
        val rows = territories
            .sortedWith(compareBy({ TerritoryCode.numberOf(it.code) ?: Int.MAX_VALUE }, { it.code }))
            .flatMap { territory ->
                val periods = periodsByTerritory[territory.id].orEmpty()
                val inYear = periods.filter { it.touches(year) }
                val previous = periods.mapNotNull { it.completedAt }.filter { it.before(year.start) }.maxOrNull()
                val header = buildString {
                    append("${territory.code} — ${territory.name}")
                    append(" · última conclusão antes do ano: ")
                    append(previous?.let(dates::format) ?: "nenhuma")
                }
                val lines = if (inYear.isEmpty()) {
                    listOf(ReportRow.Cells(listOf("—", "—", "—", "Não trabalhado no ano")))
                } else {
                    inYear.map { period ->
                        ReportRow.Cells(
                            listOf(
                                period.responsibleName,
                                period.assignedAt?.let(dates::format) ?: "—",
                                period.completedAt?.let(dates::format) ?: "—",
                                when {
                                    period.isCompleted -> "Concluído"
                                    period.returnedAt != null -> "Devolvido em ${dates.format(period.returnedAt)}"
                                    else -> "Em andamento"
                                }
                            )
                        )
                    }
                }
                listOf(ReportRow.Group(header)) + lines
            }

        val coverage = TerritoryStatistics.coverage(territories, periodsByTerritory, year)
        return ReportTable(
            title = "Registro de designação de território (S-13)",
            subtitle = listOfNotNull(
                congregationName,
                "Ano de serviço ${year.label}",
                "${coverage.worked.size} de ${coverage.total} territórios concluídos (${coverage.percent}%)"
            ).joinToString(" · "),
            columns = listOf(
                ReportColumn("Dirigente", 2f),
                ReportColumn("Designado em", 1.2f),
                ReportColumn("Concluído em", 1.2f),
                ReportColumn("Situação", 1.6f)
            ),
            rows = rows
        )
    }

    private fun AssignmentPeriod.touches(year: ServiceYear): Boolean =
        listOfNotNull(assignedAt, completedAt, returnedAt).any { it in year } ||
            (isOpen && assignedAt != null && assignedAt.before(year.endExclusive))
}
